(ns mut.audio.instr-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is]]
            pink.engine
            pink.node
            mut.audio.instr
            [mut.utils.test :refer [is-not]]))

(deftest instrument-must-be-linkded-with-pink-engine-and-mixer-node
  (defrecord TestInstrument [engine node])
  (let [engine (pink.engine/engine-create)
        audio-node (pink.node/audio-node)
        mixer-node (pink.node/mixer-node)
        instr1 (->TestInstrument nil nil)
        instr2 (->TestInstrument engine audio-node)
        instr3 (->TestInstrument engine mixer-node)]
    (is-not
      (s/valid? :mut.audio.instr/instrument instr1))
    (is-not
      (s/valid? :mut.audio.instr/instrument instr2))
    (is
      (s/valid? :mut.audio.instr/instrument instr3))))
