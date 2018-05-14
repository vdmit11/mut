(ns mut.music.pitch
  (:require [mut.music.tuning :as tuning]))

(def ^:dynamic *tuning* tuning/EDO-12)
(def ^:dynamic *reference-hz* 440)
(def ^:dynamic *reference-name* "A4")
(def ^:dynamic *reference-keynum* 69)

(defn distance->hz [distance]
  (* *reference-hz* (tuning/distance->ratio *tuning* distance)))

(defn hz->distance [hz]
  (tuning/ratio->distance *tuning* (/ hz *reference-hz*)))

(defn keynum->hz [keynum]
  (distance->hz (- keynum *reference-keynum*)))

(defn hz->keynum [hz]
  (+ *reference-keynum* (hz->distance hz)))
