(ns mut.music.tuning-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [mut.utils.math :refer [approx=]]
            [mut.music.tuning :refer [distance->ratio ratio->distance
                                      TUNINGS AUDIBLE_HZ_MIN AUDIBLE_HZ_MAX]]))

(def gen-hz
  (gen/double* {:min AUDIBLE_HZ_MIN
                :max AUDIBLE_HZ_MAX
                :infinite? false
                :NaN? false}))

(defn- tuning-roundtrip-equiv
  [tuning hz1 hz2]
  (let [ratio (/ hz1 hz2)]
      (approx= ratio (distance->ratio tuning (ratio->distance tuning ratio)))))

(defspec tuning-roundtrip
  100
  (prop/for-all [tuning (gen/elements (vals TUNINGS))
                 hz1 gen-hz
                 hz2 gen-hz]
    (tuning-roundtrip-equiv tuning hz1 hz2)))
