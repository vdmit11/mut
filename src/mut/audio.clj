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
            [mut.audio.instr-proto :as instr-proto]
            [mut.audio.pink-utils :refer [new-engine]]
            [mut.utils.ns :refer [autoload-namespaces]]
            [swiss.arrows :refer [-!>]]
            [taoensso.truss :refer [have]]
            pink.engine
            pink.node))

;; Here we have the Pink audio engine.
;;
;; It is a pure-clojure implementation of audio engine without any external dependencies.
;; That is, the digital audio is synthesized entirely in Clojure, without talking to any external
;; software or hardware synthesizers.
;;
;; So this is the main reason why Pink was chosen: no need to set up any external server and
;; communicate with it over the network. Clojure and stock JVM is all you need.
;;
;; Initially, I planned to support multiple different audio engines at once, but that turned out
;; to be a complexity booster that stopped me from progressing for many months.
;; So eventually I gave up and decided to leave only one global instnance of audio engine here.
;;
;; I still have a hope that in future I can bind different instruments to different audio engines,
;; but at the moment all instruments are associated with the same nasty global singleton instance
;; of audio engine. That is, all `:engine` members of all isntrument records are the same
;; `global-pink-audio-engine` instance spawned below (with a chance that it will change in future).
(defonce ^:private global-pink-audio-engine (new-engine))

;; A place for keeping track of allocated instruments.
;;
;; The core abstraction in this `mut.audio` module is an "Instrument".
;; An "Instrument" is a nasty OOP-style instance with mutable state.
;; And, each Instrument is associated with its own mixer node in the Pink's audio synthesis graph.
;;
;; And that means that you have to explicitly allocate/deallocate instruments.
;; If you forget to stop/deallocate the instrument, you have a resource leak: the instrument
;; remains as a node in the audio synthesis graph, and basically it keeps producing sound forever.
;;
;; So, to deal with this resource management problem, we have this mutable `allocated-instrs` map,
;; that looks like this:
;;
;;  {:piano #mut.audio.instr.piano/Piano{...}
;;   :guitar-1 #mut.audio.instr.guitar/Guitar{...}
;;   :guitar-2 #mut.audio.instr.guitar/Guitar{...}}
;;
;; So this is a map, where:
;;  key is an instrument ID, used in music score to assign notes to specific instrument
;;  value is an instrument record - an OOP-style instrance that contains mutable state
;;
;; Also instrument records (values in this map) conform to `mut.audio.instr-proto` protocols.
(defonce allocated-instrs
  (ref {}))

;; Factories for creating new instrument objects.
;;
;; Problem: as a music generating code, I don't want to refer to allocated resources.
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
;; So the problem here is: when the code tries to synthesize the generated score, and it spots
;; something like `:instr :guitar-1` in the score, how does it know which instrument to allocte?
;; I mean, we can have multiple implementations of guitar instrument, how do we choose one?
;;
;; For that purpose we have this `instr-factories` map below.
;; It is a map from a "type" to a function, that produces a concrete instrument "instance".
(autoload-namespaces
  (def ^:dynamic instr-factories
    {:click mut.audio.instr.click/map->Click}))

(defn find-instr-factory-fn
  "Given an instrument ID, find a factory function that constructs a new instrument 'object'.

  One little trick with instrument IDs is that we encode instrument type inside the ID, like:
    :piano-1 -> :piano
    :electir-guitar-1 -> :electric-guitar
    :electir-guitar-2 -> :electric-guitar

  So when we see such ID in the musical score, it is trivial to derive the type part from it,
  and then find a corresponding function in the `instr-factories` map."
  [instr-id]
  (let [instr-type (music.instr/id->type instr-id)]
    (or
      (get instr-factories instr-type)
      (throw (ex-info (format "Cannot find factory function for instrument: `%s`." instr-id)
                      {:type :instr-factory-fn-not-found
                       :instr-id instr-id
                       :instr-type instr-type
                       :available-types (keys instr-factories)})))))

;; Ok, now it should be more or less clear how instrument allocation works:
;; when you play music, you look for instrument IDs in the score, and:
;; If the ID is already present in the `allocated-instrs` map, then you're done.
;; Otherwise, you use `instr-factories` to find a function that spawns a new instrument 'instance',
;; and once it is spawned - you add it to the `allocated-instrs` map for future use.
;;
;; But then, how instruments are de-allocated? Instrument consume a significant amount of CPU and
;; memory, so we can't let them live forever, and have to implement some sort of de-allocation.
;;
;; So the code below is dedicated to de-allocation.
;; The idea is the following: we attach a timestamp called "expire beat" to each instrument.
;; Once this expire time is reached, the instrument is de-allocated automatically.
;;
;; So when you call (play! music-object), under the hood it:
;;  - analyzes the passed music object - splits it into parts by instrument
;;  - pre-allocates each found instrument
;;  - calculates when the music object should end, and sets "expire beat" to that calculated time
;;
;; So this way, you don't have to manage instrument state by hands.
;; All instrument instances are allocated/deallocated automatically as the music is being played.

(defn get-current-beat
  []
  (-> global-pink-audio-engine
      .event-list
      .getCurBeat))

(defn- get-expire-beat [instr]
  @(:expire-beat-atom instr))

(defn- expired? [instr]
  (> (get-current-beat)
     (get-expire-beat instr)))

(defn- prolong-expire-beat! [instr new-expire-beat]
  (-!>
    instr
    :expire-beat-atom
    (swap! max new-expire-beat)))

;; Ok, now the allocation code: how instrument "instances" are actually created and initialized.

(defn add-instr-node-to-audio-synth-graph!
  [instr]
  (pink.engine/engine-add-afunc (:engine instr) (:node instr))
  instr)

(defn remove-instr-node-from-audio-synth-graph!
  [instr]
  (pink.engine/engine-remove-afunc (:engine instr) (:node instr))
  instr)

(defn- new-instr [instr-id]
  (let [map->instr (find-instr-factory-fn instr-id)
        new-mixer-node (pink.node/mixer-node)]
    (map->instr {:id instr-id
                 :engine global-pink-audio-engine
                 :node new-mixer-node
                 :expire-beat-atom (atom 0)})))

;; The two main instrument resource management functions below: allocate + deallocate.

(defn alloc-instr! [instr-id]
  (dosync
    (ensure allocated-instrs)
    (or
      ;; get already allocated instrument
      (get @allocated-instrs instr-id)
      ;; create new instrument and add it to the ``allocated-instrs`` map
      (let [instr (new-instr instr-id)]
        (alter allocated-instrs assoc instr-id instr)
        (add-instr-node-to-audio-synth-graph! instr)))))

(defn dealloc-instr! [instr]
  (dosync
    (remove-instr-node-from-audio-synth-graph! instr)
    (alter allocated-instrs dissoc (:id instr))))

;; Plus, allocate/deallocate functions enhanced with "garbage collection" of expired instruments.

(defn alloc-instr-with-expire-beat! [instr-id expire-beat]
  (-> (alloc-instr! instr-id)
      (prolong-expire-beat! expire-beat)))

(defn dealloc-all-expired-instrs! []
  (doseq [[instr-id instr] @allocated-instrs]
    (when (expired? instr)
      (dealloc-instr! instr))))

;; By the way, why did I use nasty global variables here?
;; Wouldn't it be better to have some state object that you pass explicitly as an argument?
;;
;; The answer is: I already had that it in the past, it was called `Orchestra`, and it was an
;; OOP-style object responsible for allocating instruments. And although that was good for unit
;; tests, the REPL workflow became much harder.  In REPL, I always had to save instance of
;; `orchestra` in a variable, and pass it everywhere. And the application code wasn't much better:
;; the `orchestra` always ended up in a `let` block somewhere in a main loop - this is almost same
;; as a global variable, except that you also have to pass it everywhere along the call stack.
;;
;; Perhaps I did it the wrong way, but I saw more drawbacks than benefits, and eventually I gave up
;; and decided to remove `Orchestra`.  Now the audio engine state is stored in global `var`s.
;;
;; Then all functions in this file literally lost its 1st parameter.
;; And although this is bad for unit tests, now it is much easier to use with REPL.
;; And I play with REPL a lot over last couple of moths, so I choose the REPL-friendly approach.
;; And the test-unfriendliness is mitigated by the helper macro below.

(defmacro with-fresh-ass
  "Create a new fresh audio system state, evaluate body, wipe it afterwards.

  This is useful for unit tests, where you want to initialize a fresh state before each test,
  and clean up the state after test is finished.
  "
  [new-instr-factories-map & body]
  `(with-redefs [global-pink-audio-engine (new-engine)
                 allocated-instrs (ref {})
                 instr-factories ~new-instr-factories-map]
     (try
       (do ~@body)
       (finally (kill-em-all!)))))

(defn kill-em-all!
  "Remove all allocated instruments and stop the audio engine.

  Useful if you got stuck with loud noise in your headphones."
  []
  (doseq [instr (vals @allocated-instrs)]
    (dealloc-instr! instr))
  (have empty? @allocated-instrs)
  (pink.engine/engine-clear global-pink-audio-engine)
  (pink.engine/engine-stop global-pink-audio-engine))


(defn play! [mo]
  (dealloc-all-expired-instrs!)
  (pink.engine/engine-start global-pink-audio-engine)
  (let [offset (or (:offset mo) 0)
        duration (or (:duration mo) 1)
        instr-id (or (:instr mo) :click)
        curr-beat (get-current-beat)
        sched-beat (+ curr-beat offset)
        expire-beat (+ sched-beat duration)
        instr (alloc-instr-with-expire-beat! instr-id expire-beat)]
    (instr-proto/schedule-play-instrument! instr sched-beat mo)))
