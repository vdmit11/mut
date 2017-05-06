(ns mut.instruments)

(defprotocol LocationKnob
  (get-location [instrument])
  (set-location! [instrument new-location-val]))

(defprotocol VolumeKnob
  (get-volume [instrument])
  (set-volume! [instrument new-volume-val]))

(defprotocol WaveInstrument
  (play-wave [instrument amp freq]))
