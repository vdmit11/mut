(ns mut.music.tuning
  (:require [mut.utils.ns :as ns]))

(def AUDIBLE_HZ_MIN 20)
(def AUDIBLE_HZ_MAX 20000)

(defprotocol Tuning
  (ratio->distance [_ hz-ratio])
  (distance->ratio [_ tone-distance]))

(defrecord EqualOctaveDivision [octave-ratio tones-per-octave]
  Tuning
  (ratio->distance [_ hz-ratio]
    (* tones-per-octave
       (/ (Math/log hz-ratio)
          (Math/log octave-ratio))))
  (distance->ratio [_ tone-distance]
    (Math/pow octave-ratio
              (/ tone-distance tones-per-octave))))

(def EDO-12 (->EqualOctaveDivision 2/1 12))
(def EDO-17 (->EqualOctaveDivision 2/1 17))
(def EDO-19 (->EqualOctaveDivision 2/1 19))
(def EDO-31 (->EqualOctaveDivision 2/1 31))

(def TUNINGS
  (let [this-ns (:ns (meta #'Tuning))
        is-tuning? (partial satisfies? Tuning)]
    (ns/find-defs is-tuning? this-ns)))
