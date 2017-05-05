(ns mut.instruments.engine-demo
  (:require [mut.instruments.engine :as engine]
            [pink.instruments.piano :refer [piano]]))

(comment
  (do
    (defn beat [n]
      (+ (engine/now) n))

    (def e1 (engine/at (beat 0.2) (piano :keynum 60)))
    (def e2 (engine/at (beat 0.6) (piano :keynum 64)))
    (def e3 (engine/at (beat 1.0) (piano :keynum 67)))

    (def e4 (engine/at (beat 1.4) (piano :keynum 60)))
    (def e5 (engine/at (beat 1.4) (piano :keynum 64)))
    (def e6 (engine/at (beat 1.4) (piano :keynum 67)))

    (engine/start)
    (engine/add-events [e1 e2 e3 e4 e5 e6])
    (Thread/sleep 4000)
    (engine/clear)
    (engine/stop))
  )
