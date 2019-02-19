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
  (:require [mut.utils.math :as utils.math]))

(require 'pink.engine)
(require 'pink.node)
(require '[mut.utils.map :as utils.map])
(require '[mut.utils.math :as utils.math])
(require '[pink.instruments.drums :as drums])
(require '[pink.oscillators :as oscillators])
(require '[pink.filters :as filters])
(require '[pink.util :refer [mul sum]])
(require '[mut.audio.pink-utils :refer [end-when-silent]])
(require '[mut.audio.engine :as engine])

(def ^:const zdf-mode-lowpass 0)
(def ^:const zdf-mode-bandpass 2)

(defn synth-click [hz]
  (end-when-silent
      (sum
        (->
          (oscillators/pulse 0 50)
          (filters/zdf-2pole hz 40.0 zdf-mode-bandpass)
          (filters/zdf-2pole 2000 0.6 zdf-mode-lowpass))
        (->
          (drums/g-noise 40)
          (mul (drums/exp-decay 0.001 1000))
          (filters/zdf-2pole hz 4 zdf-mode-bandpass)))))

(def click-hzs
  {-1.0 1100
   0.0 1600
   1.0 2000})

(defn get-click-hz [beat]
  (utils.map/get-closest click-hzs (or (:stress beat) 0)))

(defrecord Click [id type engine node]
  engine/Instrument
  (mo->afn [_ mo] (synth-click (get-click-hz mo))))

(do
  (def instr-factories
    {:click map->Click})

  (def orchestra (engine/new-orchestra instr-factories))
  (def click-instr (engine/alloc-instr! orchestra :click-1))

  (pink.engine/engine-start (:engine orchestra))
  (for [n (range 4)]
    (do
      (engine/schedule-play-instrument! click-instr (+ n 1/4) {:stress 1})
      (engine/schedule-play-instrument! click-instr (+ n 2/4) {:stress 0})
      (engine/schedule-play-instrument! click-instr (+ n 3/4) {:stress 0})
      (engine/schedule-play-instrument! click-instr (+ n 4/4) {:stress 0})
      ))
  )
