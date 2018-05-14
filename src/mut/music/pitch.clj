(ns mut.music.pitch
  (:require [mut.music.tuning :as tuning]))

(def DEFAULT-TUNING tuning/EDO-12)
(def REFERENCE-HZ 440)
(def REFERENCE-NAME "A4")
(def REFERENCE-KEYNUM 69)

(defn distance->hz [distance]
  (* REFERENCE-HZ (tuning/distance->ratio DEFAULT-TUNING distance)))

(defn hz->distance [hz]
  (tuning/ratio->distance DEFAULT-TUNING (/ hz REFERENCE-HZ)))

(defn keynum->hz [keynum]
  (distance->hz (- keynum REFERENCE-KEYNUM)))

(defn hz->keynum [hz]
  (+ REFERENCE-KEYNUM (hz->distance hz)))
