(ns mut.music.pitch
  (:require [mut.music.tuning :as tuning]))

(def default-tuning tuning/EDO-12)
(def reference-hz 440)
(def reference-name "A4")
(def reference-keynum 69)

(defn distance->hz [distance]
  (* reference-hz (tuning/distance->ratio default-tuning distance)))

(defn hz->distance [hz]
  (tuning/ratio->distance default-tuning (/ hz reference-hz)))

(defn keynum->hz [keynum]
  (distance->hz (- keynum reference-keynum)))

(defn hz->keynum [hz]
  (+ reference-keynum (hz->distance hz)))
