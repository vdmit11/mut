(ns mut.playground)

;; Just some random prototype code that should be finished and evicted to other namespaces.

(require 'pink.engine)
(require 'pink.node)
(require '[mut.audio :as audio])
(require '[mut.audio.instr-proto :as instr-proto])
(use 'mut.audio.instr.click)

(def instr-factories
  {:click map->Click})

(def orchestra (audio/new-orchestra instr-factories))
(def click-instr (audio/alloc-instr! orchestra :click-1 4))
(audio/start! orchestra)

(for [n (range 1)]
  (do
    (instr-proto/schedule-play-instrument! click-instr (+ n 1/4) {:stress 1})
    (instr-proto/schedule-play-instrument! click-instr (+ n 2/4) {:stress 0})
    (instr-proto/schedule-play-instrument! click-instr (+ n 3/4) {:stress 0})
    (instr-proto/schedule-play-instrument! click-instr (+ n 4/4) {:stress 0})
    ))

  ;;(audio/get-current-beat orchestra)
;;(audio/dealloc-expired-instrs! orchestra)
