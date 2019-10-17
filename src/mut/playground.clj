(ns mut.playground)

;; Just some random prototype code that should be finished and evicted to other namespaces.

(require 'pink.engine)
(require 'pink.node)
(require '[mut.utils.map :as utils.map])
(require '[mut.utils.math :as utils.math])
(require '[pink.instruments.drums :as drums])
(require '[pink.oscillators :as oscillators])
(require '[pink.filters :as filters])
(require '[pink.util :refer [mul sum]])
(require '[mut.audio.pink-utils :refer [end-when-silent]])
(require '[mut.audio :as audio])
(require '[mut.audio.instr-proto :as instr-proto])

(def ^:const zdf-mode-lowpass 0)
(def ^:const zdf-mode-bandpass 2)

(defn synth-click [hz]
  (end-when-silent
    (sum
      (->
        (oscillators/pulse 0 50)
        (filters/zdf-2pole hz 40.0 zdf-mode-bandpass)
        (filters/zdf-2pole 2000 0.6 zdf-mode-lowpass))
      (->
        (drums/g-noise 40)
        (mul (drums/exp-decay 0.001 1000))
        (filters/zdf-2pole hz 4 zdf-mode-bandpass)))))

(def click-hzs
  {-1.0 1100
   0.0 1600
   1.0 2000})

(defn get-click-hz [beat]
  (utils.map/get-closest click-hzs (or (:stress beat) 0)))

(defrecord Click [id type engine node]
  instr-proto/Instrument
  (mo->afn [_ mo] (synth-click (get-click-hz mo))))

(def instr-factories
  {:click map->Click})

(def orchestra (audio/new-orchestra instr-factories))
(def click-instr (audio/alloc-instr! orchestra :click-1 4))
(audio/start! orchestra)

(for [n (range 4)]
  (do
    (instr-proto/schedule-play-instrument! click-instr (+ n 1/4) {:stress 1})
    (instr-proto/schedule-play-instrument! click-instr (+ n 2/4) {:stress 0})
    (instr-proto/schedule-play-instrument! click-instr (+ n 3/4) {:stress 0})
    (instr-proto/schedule-play-instrument! click-instr (+ n 4/4) {:stress 0})
    ))

  ;;(audio/get-current-beat orchestra)
;;(audio/dealloc-expired-instrs! orchestra)
