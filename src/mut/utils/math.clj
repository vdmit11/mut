(ns mut.utils.math
  (:require [clojure.math.numeric-tower :as math]))

(defn distance [x y]
  (math/abs (- x y)))

(defn closest [coll x]
  (letfn [(distance-to-x [y] (distance x y))]
    (apply min-key distance-to-x coll)))

(def ^:private DEFAULT-APPROX-DELTA 0.000001)

(defn approx=
  ([x y] (approx= x y DEFAULT-APPROX-DELTA))
  ([x y delta] (< (distance x y) delta)))

(defn approx-zero?
  [x]
  (approx= x 0))
