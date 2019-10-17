(ns mut.audio.engine
  "Wrappers on top of Pink audio engine for implementing Instruments.

  The `mut` project uses Pink engine for sound synthesis.
  But, Pink engine is a bit low-level, so here we define some extensions and wrappers on top of it.

  There are 2 notable entities defined here:
   1. Instrument protocol - instruments are implemented as records that extend this protocol.
   2. Orchestra - a registry of allocated instruments (usually global singleton).

  Both things are nasty 'objects' - stateful and mutable.
  And this is caused by nature of audio synthesis - you have to allocate audio buffers, write them
  to output streams, schedule events to be executed later, and maintain state between events.
  So expect to see a lot of shitty code here.
  "
  (:require pink.engine
            pink.node
            [clojure.spec.alpha :as s]
            [medley.core :as medley]
            [mut.music.instr :as music.instr]
            [mut.utils.type :as utils.type]
            [mut.audio.pink-utils :as pink-utils]))


(defprotocol Instrument
  "Basic Instrument audio synthesis protocol.

  It consists of just 1 method:
     (mo->afn [instr mo])
  that converts Music Object (see `:mut.music/object`) to Audio Function (see docs for Pink engine).
  Or, in other words, it takes a note and returns a function that synthesizes digital audio for it.

  So this single method is the minimal implementation for instrument records.
  Once you define how to synthesize sound, this module will use this method to provide a lot
  of extra functionality on top of it (like scheduling events, and mixing instruments together).

  See documentation for Pink audio engine to get the idea about audio functions.
  "
  (mo->afn [instr mo] "convert Music Object to Audio Function."))

(defn instrument? [obj]
  (satisfies? Instrument obj))

;; spec for Instrument records
;;
;; Note: in Pink audio engine, 'instrument' is a function, but here we define it as record.

;; This is a bit confusing, and may look wrong at first, but records allow us to be more flexible,
;; like:
;;  - have more than 1 audio function per instrument (different sounds/timbres for same instrument)
;;  - define more actions via extra protocols (e.g. `Killable` that forces instrument to stop)
;;  - associate extra attributes with instrument record (like `:node` - the Pink's mixer node)
;;
;; So, an Instrument is a record, to which we attach several required fields out of the box:
;;   1. `:engine` - the `pink.engine.Engine`, that runs a background Thread that produces audio.
;;   2. `:node` - the `pink.node/mixer-node`, that can be used to adjust gain/panning.
;;
;; Different instruments may share the same `:engine` (and thus be executed in the same Thread),
;; but each instrument has its own private `:node`, to which the instrument outputs the sound.
;;
;; Instruments are free to define any additional fields (including ones that store mutable state).
(s/def ::instrument
  (s/and
    record?
    instrument?
    (s/keys
      :req-un
      [::node
       ::engine
       ::expire-beat-atom])))

(s/def ::engine pink-utils/engine?)
(s/def ::node pink-utils/mixer-node?)
(s/def ::expire-beat-atom utils.type/atom-containing-long?)

(defn play-instrument! [instr mo]
  (when-let [afn (mo->afn instr mo)]
    (pink.node/node-add-func (:node instr) afn)))

(defn schedule-play-instrument! [instr time mo]
  (let [engine (:engine instr)
        event (pink.event.Event. play-instrument! time [instr mo])]
    (pink.engine/engine-add-events engine [event])))


;; Orchestra of instruments.
;;
;; Problem: in order to play audio, you need to allocate resources, like:
;;  - audio engine (`pink.engine.Engine`) running in a separate Thread
;;  - audio node (`pink.node.Node`) for each instrument
;;  - mutable instrument state (low-level audio buffers)
;;
;; And in addition, you have to free all those resources manually
;; (because GC doesn't know how to stop Threads and close audio outputs).
;;
;; So I (as a music generator, let't say) don't want to do that manually.
;; I would like these things to happen automatically under the hood,
;; while music is being synthesized/played.
;;
;; I would like to be able to just write some music score that looks as pure data,
;; that doesn't refer to any stateful objects (like threads or I/O ports).
;; I imagine it looking like this:
;;
;;   (def score #{{:instr :guitar-1 :contents [...]}
;;                {:instr :guitar-2 :contents [...]}
;;                {:instr :bass :contents [...]}
;;                {:instr :drums :contents [...]}})
;;
;; Here I'm referring instruments by name, like `:guitar-1` and `:guitar-2`,
;; and I expect them to be allocated/deallocated automatically as the code enters/exits
;; corresponding sections of the music score during audio synthesis process.
;;
;; So this is what this Orchestra below is about - keeping global list of allocated instruments
;; to have an ability to refer them by ID (instead of direct reference to object).
(defrecord Orchestra [engine instrs-ref instr-factories])

(defn new-orchestra [instr-factories]
  (map->Orchestra
    {:engine (pink.engine/engine-create :nchnls 2)
     :instrs-ref (ref {})
     :instr-factories instr-factories}))

(defn start! [orchestra]
  (-> orchestra
      :engine
      pink.engine/engine-start))

(defn stop! [orchestra]
  (-> orchestra
      :engine
      pink.engine/engine-stop))

(defn get-current-beat [orchestra-or-instrument]
  (-> orchestra-or-instrument
      .engine
      .event-list
      .getCurBeat))

(defn get-expire-beat [instr]
  @(:expire-beat-atom instr))

(defn expired? [instr]
  (> (get-current-beat instr)
     (get-expire-beat instr)))

(defn prolong-expire-beat! [instr duration]
  (let [expire-beat-atom (:expire-beat-atom instr)
        cur-beat (get-current-beat instr)
        new-expire-beat (+ cur-beat duration)]
    (swap! expire-beat-atom max new-expire-beat))
  instr)

(defn get-instr [orchestra instr-id]
  (get @(:instrs-ref orchestra) instr-id))

(defn contains-instr? [orchestra instr]
  (contains? @(:instrs-ref orchestra) (:id instr)))

(defn attach-instr! [orchestra instr]
  (dosync
    (assert (not (contains-instr? orchestra instr)))
    (alter (:instrs-ref orchestra) assoc (:id instr) instr)
    (pink.engine/engine-add-afunc (:engine instr) (:node instr)))
  instr)

(defn detach-instr! [orchestra instr]
  (dosync
    (assert (contains-instr? orchestra instr))
    (pink.engine/engine-remove-afunc (:engine instr) (:node instr))
    (alter (:instrs-ref orchestra) dissoc (:id instr)))
  instr)

(defn find-instr-factory-fn [instr-factories instr-id]
  (let [instr-type (music.instr/id->type instr-id)]
    (get instr-factories instr-type)))

(defn new-instr [orchestra instr-id]
  (let [{:keys [engine instr-factories]} orchestra
        map->instr (find-instr-factory-fn instr-factories instr-id)
        new-mixer-node (pink.node/mixer-node)]
    (map->instr {:id instr-id
                 :engine engine
                 :node new-mixer-node
                 :expire-beat-atom (atom 0)})))

(defn get-or-create!-instr [orchestra instr-id]
  (dosync
    (or (get-instr orchestra instr-id)
        (->> (new-instr orchestra instr-id)
             (attach-instr! orchestra)))))

(defn alloc-instr! [orchestra instr-id duration]
  (dosync
    (ensure (:instrs-ref orchestra))
    (->
      (get-or-create!-instr orchestra instr-id)
      (prolong-expire-beat! duration))))

(defn dealloc-expired-instrs! [orchestra]
  (dosync
    (let [instrs-ref (:instrs-ref orchestra)
          instrs-map (ensure instrs-ref)
          instrs (vals instrs-map)]
      (doseq [instr instrs]
        (if (expired? instr)
          (detach-instr! orchestra instr)))))
  orchestra)
