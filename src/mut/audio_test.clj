(ns mut.audio-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is]]
            [mut.audio :as audio]
            [mut.audio.instr-proto :as audio.instr-proto]
            [mut.utils.map :refer [map-contains-submap?]]
            [mut.utils.test :refer [is-not]]
            [mut.utils.type :refer [class-name]]
            pink.engine
            pink.node))

(defrecord TestInstrument [engine node]
  audio.instr-proto/Instrument
  (mo->afn [instr mo] nil))

(deftest instrument-must-be-linkded-with-pink-engine-and-mixer-node
  (let [engine (pink.engine/engine-create)
        audio-node (pink.node/audio-node)
        mixer-node (pink.node/mixer-node)
        instr1 (->TestInstrument nil nil)
        instr2 (->TestInstrument engine audio-node)
        instr3 (->TestInstrument engine mixer-node)]
    (is-not
      (s/valid? :mut.audio.instr-proto/instrument instr1))
    (is-not
      (s/valid? :mut.audio.instr-proto/instrument instr2))
    (is
      (s/valid? :mut.audio.instr-proto/instrument instr3))))


(defrecord TestPiano [id type engine node]
  audio.instr-proto/Instrument
  (mo->afn [instr mo] nil))

(defrecord TestGuitar [id type engine node]
  audio.instr-proto/Instrument
  (mo->afn [instr mo] nil))

(def instr-factories
  {:test-piano map->TestPiano
   :test-guitar map->TestGuitar})

(deftest allocate-instr-creates-new-instr-by-id-if-not-exists
  (let [orchestra (audio/new-orchestra instr-factories)
        before (audio/get-instr orchestra :test-piano)
        alloc1 (audio/alloc-instr! orchestra :test-piano ##Inf)
        alloc2 (audio/alloc-instr! orchestra :test-piano ##Inf)
        after  (audio/get-instr orchestra :test-piano)]
    (is (nil? before))
    (is (identical? alloc1 alloc2))
    (is (identical? alloc1 after))))

(deftest allocate-instr-constructs-records-guessing-type-from-id
  (let [orchestra (audio/new-orchestra instr-factories)
        piano   (audio/alloc-instr! orchestra :test-piano ##Inf)
        guitar1 (audio/alloc-instr! orchestra :test-guitar-1 ##Inf)
        guitar2 (audio/alloc-instr! orchestra :test-guitar-2 ##Inf)]
      (is (= "mut.audio_test.TestPiano" (class-name piano)))
      (is (= "mut.audio_test.TestGuitar" (class-name guitar1)))
      (is (= "mut.audio_test.TestGuitar" (class-name guitar2)))
      (is (map-contains-submap? piano   {:id :test-piano}))
      (is (map-contains-submap? guitar1 {:id :test-guitar-1}))
      (is (map-contains-submap? guitar2 {:id :test-guitar-2}))))
