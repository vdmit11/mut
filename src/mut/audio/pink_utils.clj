(ns mut.audio.pink-utils
  "Utils for Pink audio engine."
  (:require pink.event))


(defmacro at
  "Produce a pink Event that fires an instrument function.

  Example:
    (at 16 (piano :keynum 60 :duration 2))

  - `piano` is the instrument function (that returns an audio function)
  - `:keynum/:duration` are arguments for the instrument function
  - `16` is the event start time (measured in beats since engine start)

  So the macro produces the `pink.event/Event` object that you can add to the engine.
  This Event, when fired, will evaluate the specified instrument function,
  and add the resulting audio function to the current engine.
  "
  [time [instrfn & args]]
  `(pink.event/event fire-instrfn ~time ~instrfn ~@args))

(defn fire-instrfn
  [instrfn & args]
  (let [afunc (apply instrfn args)]
    (add-afunc afunc)))
