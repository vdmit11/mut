(ns mut.midi.output
  "An interface for playing notes via standard Java MIDI Synthesizer.

  Playing sound implies side effects. It is a slow and stateful process and
  thus is hard for testing. So this module isolates the playing process in order
  to protect entire application from this hazard.

  Other modules don't play MIDI on the fly. Instead, they generate sequences of
  stateless instructions. And then the sequence is passed to the `play` function
  that does the I/O.
  An example of how that happens (very simplified):
  (def song
    [(note-on 60)
     (note-on 67)
     (pause 1000)
     (note-on 60)
     (pause 500)
     (note-on 63)
     (pause 250)
     (note-on 72)
     (pause 1000)
     (note-off 60)
     (note-off 63)
     (note-off 67)
     (note-off 72)])  ; <- pure data, no side effects
  (play-midi song)    ; <- sound output, side effects happen here

  So the process of playing MIDI is split into two steps:
    1. Generating music (creating a sequence of MIDI instructions)
    2. Playing it (making sound output according to these instructions)
  Other modules generate the music, and this module plays it.

  Such approach has several major advantages:
  - Functions that generate music don't need to perform I/O and thus they become
    fast and pure, and they may easily be covered by automated unit tests.
  - Functions that play music may be optimized to reduce I/O latency.
    If other modules would generate music and play it on the fly, they may
    introduce accidental delays and thus make timing not very precise.
    This is not acceptable for ear training software. So we let music generators
    to work arbitrary amounts of time; we just take their output and play it
    here with minimal I/O delays.

  So we separate the processes of generation and playing the music.
  The generation process (done in other modules) may be slow and sophisticated.
  The playing process (done here) must be dumb and fast (minimal I/O delays).

  This module contains three main entities:
  1. `play-midi` function.
     It takes a sequence of instructions and plays the music according to them.
  2. `MidiInstruction` type.
     It describes one MIDI event (NoteOn, NoteOff, pause, etc).
  3. A bunch of helper constructor functions for MidiInstruction:
     note-on, note-off, pitch-bend, control-change, pause, etc.
  "
  (:import [javax.sound.midi MidiChannel MidiSystem Synthesizer]))

; forward declarations
(declare reset-all-channels opcode-of)


;; MidiInstruction represents one action performed by the `play-midi` function.
;;
;; When you want to play music you generate a sequence of MidiInstruction
;; objects and pass it to the `play-midi` function.
;;
;; Fields have primitive type hints to avoid autoboxing overhead and
;; perhaps to save memory because sequences of such instructions may be
;; very long (up to a million of objects for a complex song).
(deftype MidiInstruction [^byte opcode ^byte channel ^short parameter1 ^byte parameter2])


(def pitch-bend-ref-point
  "The reference point for use with the 'pitch-bend` function.
   Example:
   (pitch-bend (+ pitch-bend-ref-point 128))
   (pitch-bend (- pitch-bend-ref-point 1000))

   This value is necessary because the MIDI command takes a value in range
   of 0 to 16384, but usually you want to bend to both sides, to the middle point
   of 8192 is chosen as 'zero'."
  8192)


(def ^:dynamic *velocity*
  "The default velocity for note-on and note-off functions.
   Useful for specifying the velocity for a group of instructions, for example:
    (binding [*velocity* 64]
      (note-on 40)
      (note-on 43)
      (note-off 40)
      (note-off 43))
   is equivalent to:
    (note-on 40 :velocity 64)
    (note-on 43 :velocity 64)
    (note-off 40 :velocity 64)
    (note-off 43 :velocity 64)"
  96)


(def ^:dynamic *channel*
  "The default channel which is used when you don't specify it explicitly
   when you generate MidiInstructions.
   Example:
    (binding [*channel* 9]
      (channel-pressure 20)
      (note-on 69)
      (pause 1000)
      (note-off 69))
   is equivalent to:
    (channel-pressure 20 :channel 9)
    (note-on 69 :channel 9)
    (pause 1000)
    (note-off 69 :channel 9)"
  0)



(defn pause
  [milliseconds]
  (->MidiInstruction (opcode-of :pause) 0 milliseconds 0))

(defn all-notes-on
  [& {:keys [channel] :or {channel *channel*}}]
  (->MidiInstruction (opcode-of :all-notes-off) channel 0 0))

(defn all-sound-off
  [& {:keys [channel] :or {channel *channel*}}]
  (->MidiInstruction (opcode-of :all-sound-off) channel 0 0))

(defn control-change
  [control value & {:keys [channel] :or {channel *channel*}}]
  (->MidiInstruction (opcode-of :control-change) channel control value))

(defn note-off
  [note-number & {:keys [velocity channel] :or {velocity *velocity* channel *channel*}}]
  (->MidiInstruction (opcode-of :note-off) channel note-number velocity))

(defn note-on
  [note-number & {:keys [velocity channel] :or {velocity *velocity* channel *channel*}}]
  (->MidiInstruction (opcode-of :note-on) channel note-number velocity))

(defn program-change
  [program & {:keys [channel] :or {channel *channel*}}]
  (->MidiInstruction (opcode-of :program-change) channel 0 program))

(defn reset-all-controllers
  [& {:keys [channel] :or {channel *channel*}}]
  (->MidiInstruction (opcode-of :reset-all-controllers) channel 0 0))

(defn channel-pressure
  [pressure & {:keys [channel] :or {channel *channel*}}]
  (->MidiInstruction (opcode-of :set-channel-pressure) channel pressure 0))

(defn pitch-bend
  [bend-value & {:keys [channel] :or {channel *channel*}}]
  (->MidiInstruction (opcode-of :set-pitch-bend) channel bend-value 0))

(defn poly-pressure
  [note-number pressure & {:keys [channel] :or {channel *channel*}}]
  (->MidiInstruction (opcode-of :set-poly-pressure) channel note-number pressure))



;; The .open() of the Synthesizer is expensive so we do it once when
;; the module is loaded and keep it in this state in the global variable.
(def ^{:private true :tag Synthesizer} synthesizer (MidiSystem/getSynthesizer))
(def ^{:private true :tag 'objects} channels (.getChannels synthesizer))
(.open synthesizer)


(defn play-midi
  "Play a sequence of MidiInstruction objects."
  [instructions]
  ;; doall is used to make evaluation happen before we do time sensitive operations
  (doseq [^MidiInstruction instruction (doall instructions)]
    (let [opcode (.opcode instruction)
          channel (.channel instruction)
          parameter1 (.parameter1 instruction)
          parameter2 (.parameter2 instruction)
          ^MidiChannel co (aget channels channel)]
      ;; the hardcoded opcodes are ugly, but I can't use symbolic
      ;; names here because of the way "case" handles the clauses
      (case opcode
        0 (Thread/sleep parameter1)
        1  (.allNotesOff co)
        2  (.allSoundOff co)
        3  (.controlChange co parameter1 parameter2)
        4  (.noteOff co parameter1 parameter2)
        5  (.noteOn co parameter1 parameter2)
        6  (.programChange co parameter1 parameter2)
        7  (.resetAllControllers co)
        8  (.setChannelPressure co parameter1)
        9  (.setMono co parameter1)
        10 (.setMute co parameter1)
        11 (.setOmni co parameter1)
        12 (.setPitchBend co parameter1)
        13 (.setPolyPressure co parameter1 parameter2)
        14 (.setSolo co parameter1)
        (throw (new Exception (format "unsupported opcode: %d" opcode))))))
  (reset-all-channels))


(def ^{:private true} opcode-of
  "A function that maps a keyword to an opcode suitable for use in MidiInstruction."
  {:pause 0
   :all-notes-off 1
   :all-sound-off 2
   :control-change 3
   :note-off 4
   :note-on 5
   :program-change 6
   :reset-all-controllers 7
   :set-channel-pressure 8
   :set-mono 9
   :set-mute 10
   :set-omni 11
   :set-pitch-bend 12
   :set-poly-pressure 13
   :set-solo 14 })


(defn- reset-all-channels
  "Reset all channels of the global 'synthesizer' singleton to the default state."
  []
  (amap channels channel-idx _
        (doto ^MidiChannel (aget channels channel-idx)
          (.allNotesOff)
          (.allSoundOff)
          (.resetAllControllers)
          (.programChange 0 0)
          (.setOmni false)
          (.setMono false)
          (.setSolo false)
          (.setMute false)))
  nil)



(defn play-midi-chromatic-demo
  "Play a sequence of all available MIDI notes."
  []
  (play-midi
   (mapcat
    #(list
      (note-on %)
      (pause 100)
      (note-off %))
    (range 0 127))))


(defn midi-benchmark
  "Do NoteOff operation a lot of times.
   Useful for tuning performance and memory usage."
  []
  (let [sequence (doall (repeatedly 10000000 #(note-off 0)))]
    (time (play-midi sequence))))
