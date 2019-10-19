(ns mut.audio.pink-utils
  "Utils for Pink audio engine, that didn't fell into any other module."
  (:require pink.event
            pink.engine
            pink.node
            [mut.utils.type :as utils.type]
            [mut.utils.math :as utils.math]))

(defn engine? [obj]
  (instance? pink.engine.Engine obj))

(defn mixer-node? [obj]
  (utils.type/satisfies-every?
    [pink.node/Node
     pink.node/GainNode
     pink.node/StereoMixerNode]
    obj))

(defn switch-engine-to-absolute-time!
  "Switch pink.engine.Engine events to absolute time.

  By default, the Engine uses relative time for events.
  That is, when you add an event, you say something like 'it starts in 2 beats from now'.

  This is handy for hand-coding scores, but not very handy for generating music.
  Because we usually generate music in advance, and we know when each note sounds.
  So for us, it is more handy to say 'these notes start at beats 32, 33, 34, 35'.

  So this function is a tool to switch pink's Engine to the more handy absolute time."
  [engine]
  (pink.event/use-absolute-time! (.event-list engine)))

(defn new-engine
  "A nullary constructor for `pink.engine/Engine` object, with pre-defined settings."
  []
  (let [engine (pink.engine/engine-create :nchnls 2)]
    (switch-engine-to-absolute-time! engine)
    engine))

(defn end-when-silent
  "Execute audio function, examine the output buffer, and return `nil` if it contains silence.

  This is a fixed version (almost copy-paste) of `pink.instruments.drums/end-when-silent`.
  The difference is in comparison: originally it is `zero?`, replaced with `approx-zero?` here.

  The problem with just `zero?` is that some audio effects may produce noise
  (caused by limited precision of floating point arithmetics that we use for computation).
  This is the case for `pink.filters/zdf-2pole` and I suspect that generally all IIR filters
  (Infinite Impulse Response) may have this issue by design.

  So here we use `approx-zero?` that basically acts as a noise gate.
  When output signal reaches a (hardcoded) threshold, we terminate the audio function."
  [afn]
  (fn []
    (when-let [^doubles sig (afn)]
      (when-not (and (utils.math/approx-zero? (aget sig 0))
                     (utils.math/approx-zero? (aget sig (dec (alength sig)))))
        sig))))
