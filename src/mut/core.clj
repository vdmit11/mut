(ns mut.core
  (:gen-class))

;; playground here
;;
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
(require '[mut.audio.engine :as engine])

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
  engine/Instrument
  (mo->afn [_ mo] (synth-click (get-click-hz mo))))

(do
  (def instr-factories
    {:click map->Click})

  (def orchestra (engine/new-orchestra instr-factories))
  (def click-instr (engine/alloc-instr! orchestra :click-1 4))
  (engine/start! orchestra)

  (for [n (range 4)]
    (do
      (engine/schedule-play-instrument! click-instr (+ n 1/4) {:stress 1})
      (engine/schedule-play-instrument! click-instr (+ n 2/4) {:stress 0})
      (engine/schedule-play-instrument! click-instr (+ n 3/4) {:stress 0})
      (engine/schedule-play-instrument! click-instr (+ n 4/4) {:stress 0})
      ))

  (engine/get-current-beat orchestra)
  ;;(engine/dealloc-expired-instrs! orchestra)
  )
