(ns mut.audio
  "Audio I/O module.

  The `mut.audio` module is responsible for playing the music
  (I hope that in future it will be able to also caputre input, but for now it is just output).

  Basically it solves the following problem:
    Given already existing music score (see `mut.music`), synthesize the sound (heared by a human).
    And make it sound good (not a primitive MIDI piano, as it is usually done, but rather a rich
    orchestra of different kinds of instruments - drums, string synths, pre-recorded samples, etc).

  That involves audio synthesis, like MIDI synths, but except that I want to do more than MIDI,
  so here we have some custom audio synth implementations that are not based on MIDI at all.

  I like to think of Instruments.
  Like, you have a pre-defined arsenal of different instruments (synths, samplers, etc),
  and when you do (play!), it does roughtly that:
   - walk cross score, and split it to parts (each instrument gets its own part)
   - call instruments to convert music objects to digital audio data
   - send data to audio output (perform side-effects)

  That would be an ideal design for me, but it has some problems (see below).

  But anyway, the main unit of this `mut.audio` module is the Instrument.
  The module should mostly consist of different instrument implementations,
  where each instrument defines its own way of playing music (synthesizing, sampling, etc).

  Problems with the design
  ========================

  Running (or interacting with) an Audio Engine has many complications:
   - Playing audio is not pure by definition (have to do I/O on the fly).
   - Have to deal with mutable state (low-level audio buffers, synthesizer implementation, etc).
   - Some resources (threads, buffers, audio outputs) have to be explicitly allocated/freed.
   - Realtime audio playing/synthesis engine must run in a separate high-priority thread,
     that can't experience any pauses - no blocking code, no slow algorithms.
   - Audio engine thread must be able to schedule events with high precision, and `Thread/sleep`
     and OS clock are too weak for that purpose, and the thread has to tick its own internal clock.
   - If you run multiple threads, their internal clocks may skew (so need to sync them).
   - You would like to define different kinds of isntruments (so polymorphism is required).
   - You may even want to use different kinds audio engines, like external MIDI devices.
   - Music notation has complex strucutre (like song sections, and phrases), while instruments
     want low-level instruction-like things like Notes, so need to define a split/walk algorithm,
     and that may also depend (because different instruments have different playing abilities).

  So these are things that we all hate - I/O, mutability, multi-threading, performance issues.
  And in the same time polymorphism is required. So this is a hard task.

  And I still didn't come up with a good design that can solve all these issues,
  and do that in a polymorphic way (that doesn't depend on audio engine).
  Eventually, I gave up and decided to use Pink audio engine exclusively
  (and define all instruments and audio outputs in Pink's terms).

  So this code relies on Pink, but with a hope that I can support different audio engines later.
  "
  (:require [mut.music.instr :as music.instr]
            pink.engine
            pink.node))

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
