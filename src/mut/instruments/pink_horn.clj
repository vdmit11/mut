(ns mut.instruments.pink-horn
  (:require [mut.instruments :as instruments]
            pink.instruments.horn
            [pink.util :refer [mul reader]]))

(defrecord PinkHorn
    [^clojure.lang.Atom location
     ^clojure.lang.Atom volume]

  clojure.lang.IFn
  (invoke [_ amp freq]
    (pink.instruments.horn/horn (* @volume amp) freq @location))

  instruments/VolumeControl
  (get-volume [_] @volume)
  (set-volume! [_ val] (reset! volume val))

  instruments/LocationControl
  (get-location [_] @location)
  (set-location! [_ val] (reset! location val)))


(defn make-horn [& {:keys [volume location]
                    :or {volume 0.5 location 0}}]
  (->PinkHorn (atom location) (atom volume)))


;; demo
(comment
  (require '[mut.engine :refer [start clear stop add-afunc]])

  (def horn (make-horn))

  (start)
  (Thread/sleep 50)

  ;; adjust volume
  (instruments/set-volume! horn 0.25)
  (add-afunc (horn 0.5 440))
  (Thread/sleep 1000)

  (instruments/set-volume! horn 0.75)
  (add-afunc (horn 0.5 440))
  (Thread/sleep 1000)

  ;; adjust location
  (instruments/set-location! horn -1)
  (add-afunc (horn 0.5 440))
  (Thread/sleep 1000)

  (instruments/set-location! horn 0)
  (add-afunc (horn 0.5 440))
  (Thread/sleep 1000)

  (instruments/set-location! horn 1)
  (add-afunc (horn 0.5 440))
  (Thread/sleep 1000)

  (clear)
  (stop)
  )
