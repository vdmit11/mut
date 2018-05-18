(ns mut.audio.pink-utils
  "Utils for Pink audio engine, that didn't fell into any other module."
  (:require pink.event
            pink.engine
            pink.node
            [mut.utils.proto :as proto]))

(defn engine? [obj]
  (instance? pink.engine.Engine obj))

(defn mixer-node? [obj]
  (proto/satisfies-every?
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

(comment
  "FIXME: this macro can only be used with `pink.simple` (one global engine),
   while I want multiple engines in parallel. Need to somehow rework it."
  (defmacro at
    "Produce a pink Event that fires an instrument function.

     Example:
       (at 16 (piano :keynum 60 :duration 2))

     - `piano` is the instrument function (that returns an audio function)
     - `:keynum/:duration` are arguments for the instrument function
     - `16` is the event start time (measured in beats since engine start)

     So the macro produces the `pink.event/Event` object that you can add to the engine.
     This Event, when fired, will evaluate the specified instrument function,
     and add the resulting audio function to the current engine."
    [time [instrfn & args]]
    `(pink.event/event fire-instrfn ~time ~instrfn ~@args))

  (defn fire-instrfn
    [instrfn & args]
    (let [afunc (apply instrfn args)]
      (add-afunc afunc))))
