(ns mut.audio.instr.click
  (:require [pink.instruments.drums :as drums])
  (:require [pink.oscillators :as oscillators])
  (:require [pink.filters :as filters])
  (:require [pink.util :refer [mul sum]])
  (:require [mut.utils.map :as utils.map])
  (:require [mut.audio.pink-utils :refer [end-when-silent]])
  (:require [mut.audio.instr-proto :refer [Instrument]]))

(def ^:const zdf-mode-lowpass 0)
(def ^:const zdf-mode-bandpass 2)

(defn synth-click [hz]
  (end-when-silent
    (sum
      (->
        (oscillators/pulse 0 50.0)
        (filters/zdf-2pole hz 40.0 zdf-mode-bandpass)
        (filters/zdf-2pole 2000 0.6 zdf-mode-lowpass))
      (->
        (drums/g-noise 40.0)
        (mul (drums/exp-decay 0.001 1000.0))
        (filters/zdf-2pole hz 4 zdf-mode-bandpass)))))

(def click-hzs
  {-1.0 1100
   0.0 1600
   1.0 2000})

(defn get-click-hz [beat]
  (utils.map/get-closest click-hzs (or (:stress beat) 0)))

(defrecord Click [id type engine node]
  Instrument
  (mo->afn [_ mo] (synth-click (get-click-hz mo))))
