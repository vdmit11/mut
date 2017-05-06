(ns mut.instruments.pink-horn
  (:require [mut.instruments :as instruments]
            [pink.instruments.horn :refer [horn]]))

(defrecord PinkHorn
    [^clojure.lang.Atom loc
     ^clojure.lang.Atom vol]

  instruments/VolumeKnob
  (get-volume [_] @vol)
  (set-volume! [_ val] (reset! vol val))

  instruments/LocationKnob
  (get-location [_] @loc)
  (set-location! [_ val] (reset! loc val))

  instruments/WaveInstrument
  (play-wave [_ amp freq]
    (horn (* amp @vol) freq @loc)))


;; demo
(comment
  (require '[mut.engine :refer [start clear stop add-afunc]])
  (require '[mut.instruments :refer [play-wave set-volume! set-location!]])

  (def h (->PinkHorn (atom 0.5) (atom 1)))

  (start)

  ;; adjust volume
  (add-afunc (play-wave h 0.5 440))
  (set-volume! h 0.5)
  (add-afunc (play-wave h 0.5 440))
  (set-volume! h 1)

  ;; adjust location
  (add-afunc (play-wave h 0.5 440))
  (set-location! h -1)
  (add-afunc (play-wave h 0.5 440))
  (set-location! h 0)
  (add-afunc (play-wave h 0.5 440))
  (set-location! h 1)
  (add-afunc (play-wave h 0.5 440))

  (clear)
  (stop)
  )
