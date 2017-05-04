(ns mut.music.pitch
  (:require [mut.music.tuning :as tuning]))

(defprotocol Pitch
  (hz-of [mo])
  (name-of [mo])
  (keynum-of [mo]))


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

(extend-type java.lang.Number
  Pitch
  (hz-of [n] (keynum->hz n))
  (keynum-of [n] n))
