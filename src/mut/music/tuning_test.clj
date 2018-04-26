(ns mut.music.tuning-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [mut.test-utils :refer [approx=]]
            [mut.music.tuning :refer [distance->ratio ratio->distance
                                      EDO-12 AUDIBLE_HZ_MIN AUDIBLE_HZ_MAX]]))

(def gen-hz
  (gen/double* {:min AUDIBLE_HZ_MIN
                :max AUDIBLE_HZ_MAX
                :infinite? false
                :NaN? false}))

(defn- tuning-roundtrip-equiv
  [hz1 hz2]
  (let [ratio (/ hz1 hz2)]
      (approx= ratio (distance->ratio EDO-12 (ratio->distance EDO-12 ratio)))))

(defspec tuning-roundtrip
  100
  (prop/for-all [hz1 gen-hz
                 hz2 gen-hz]
    (tuning-roundtrip-equiv hz1 hz2)))
