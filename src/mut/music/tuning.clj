(ns mut.music.tuning)

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
