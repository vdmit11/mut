(ns mut.audio.instr-proto
  (:require [clojure.spec.alpha :as s]
            [mut.audio.pink-utils :as pink-utils]
            pink.engine
            pink.node))

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
       ::engine])))

(s/def ::engine pink-utils/engine?)
(s/def ::node pink-utils/mixer-node?)


(defn get-current-beat
  [instr]
  (-> instr
      :engine
      .event-list
      .getCurBeat))

(defn play-instrument! [instr mo]
  (when-let [afn (mo->afn instr mo)]
    (pink.node/node-add-func (:node instr) afn)))

(defn schedule-play-instrument! [instr time mo]
  (let [engine (:engine instr)
        event (pink.event.Event. play-instrument! time [instr mo])]
    (pink.engine/engine-add-events engine [event])))
