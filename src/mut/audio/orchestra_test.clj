(ns mut.audio.orchestra-test
  (:require [mut.audio.orchestra :as orchestra]
            [clojure.reflect :as r]
            [clojure.test :refer [deftest is]]))

(defn contains-map? [super-map sub-map]
  (every?
    (fn [key]
      (and (contains? super-map key)
           (= (get super-map key) (get sub-map key))))
    (keys sub-map)))

(defn class-name [obj]
  (r/typename (class obj)))

(defrecord TestPiano [id type engine node])

(defmethod orchestra/map->instr :test-piano
  [m]
  (map->TestPiano m))

(deftest allocate-instr-creates-new-instr-by-id-if-not-exists
  (orchestra/with-new-orchestra
    (let [before (orchestra/get-instr :test-piano)
          alloc1 (orchestra/alloc-instr :test-piano)
          alloc2 (orchestra/alloc-instr :test-piano)
          after  (orchestra/get-instr :test-piano)]
      (is (nil? before))
      (is (identical? alloc1 alloc2))
      (is (identical? alloc1 after)))))


(defrecord TestGuitar [id type engine node])

(defmethod orchestra/map->instr :test-guitar
  [m]
  (map->TestGuitar m))

(deftest allocate-instr-constructs-records-guessing-type-from-id
  (orchestra/with-new-orchestra
    (let [piano   (orchestra/alloc-instr :test-piano)
          guitar1 (orchestra/alloc-instr :test-guitar-1)
          guitar2 (orchestra/alloc-instr :test-guitar-2)]
      (is (= "mut.audio.orchestra_test.TestPiano" (class-name piano)))
      (is (= "mut.audio.orchestra_test.TestGuitar" (class-name guitar1)))
      (is (= "mut.audio.orchestra_test.TestGuitar" (class-name guitar2)))
      (is (contains-map? piano   {:type :test-piano,  :id :test-piano}))
      (is (contains-map? guitar1 {:type :test-guitar, :id :test-guitar-1}))
      (is (contains-map? guitar2 {:type :test-guitar, :id :test-guitar-2})))))
