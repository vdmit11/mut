(ns mut.instruments)

(defprotocol LocationControl
  (get-location [instrument])
  (set-location! [instrument new-location-val]))

(defprotocol VolumeControl
  (get-volume [instrument])
  (set-volume! [instrument new-volume-val]))

(defprotocol ToneControl
  (get-tone [instrument])
  (set-tone! [instrument new-tone-val]))
