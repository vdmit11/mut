(ns mut.instruments.synth
  (:require
   [pink.space :refer [pan]]
   [pink.envelopes :refer [xar]]
   [pink.oscillators :refer [blit-saw sine]]
   [pink.filters :refer [lpf18]]
   [pink.util :refer [sum mul reader]]
   [mut.instruments :as instruments]))


(deftype Synth
    [^:double attack
     ^:double release

     ^:clojure.lang.Atom volume
     ^:clojure.lang.Atom tone
     ^:clojure.lang.Atom location]

  clojure.lang.IFn
  (invoke [_ freq]
    (let [volume (reader volume)
          tone (reader tone)
          location (reader location)
          envelope (xar attack release)
          wave (sum (blit-saw freq)
                    (blit-saw (* 0.9995 freq))
                    (mul 0.20 (sine (* 1/2 freq)))
                    (mul 0.10 (sine (* 2/3 freq)))
                    (mul 0.10 (sine (* 4/3 freq)))
                    (mul 0.05 (sine (* 6/5 freq)))
                    )]
      (pan
       (mul volume
            envelope
            (lpf18 wave
                   (mul tone 2000)
                   (mul tone 2)
                   (mul tone 2))
           )
       location)))

  instruments/LocationControl
  (get-location [_] @location)
  (set-location! [_ val] (reset! location val))

  instruments/ToneControl
  (get-tone [_] @tone)
  (set-tone! [_ val] (reset! tone val))

  instruments/VolumeControl
  (get-volume [_] @volume)
  (set-volume! [_ val] (reset! volume val)))


(defn make-synth
  [& {:keys [attack release volume tone location]
      :or {attack 0.001
           release 5
           volume 0.5
           tone 0.5
           location 0}}]
  (Synth. attack release (atom volume) (atom tone) (atom location)))


;; demo
(comment
  (require '[mut.engine :refer [add-afunc start stop clear]])

  (def synth1 (make-synth :tone 0.2))
  (def synth2 (make-synth :tone 0.8))

  (start)

  (Thread/sleep 50)

  (add-afunc (synth1 440))
  (Thread/sleep 300)
  (add-afunc (synth2 880))
  (Thread/sleep 300)
  (add-afunc (synth1 660))
  (Thread/sleep 300)
  (add-afunc (synth2 586))
  (Thread/sleep 300)
  (add-afunc (synth1 550))
  (Thread/sleep 300)

  (Thread/sleep 600)
  (instruments/set-location! synth1 -0.5)
  (instruments/set-location! synth2 +0.5)
  (instruments/set-tone! synth1 0.5)
  (instruments/set-tone! synth2 0.5)

  (add-afunc (synth1 440))
  (Thread/sleep 300)
  (add-afunc (synth2 880))
  (Thread/sleep 300)
  (add-afunc (synth1 660))
  (Thread/sleep 300)
  (add-afunc (synth2 586))
  (Thread/sleep 300)
  (add-afunc (synth1 550))
  (Thread/sleep 300)

  (Thread/sleep 1000)

  (clear)
  (stop)
)
