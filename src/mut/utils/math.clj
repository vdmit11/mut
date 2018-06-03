(ns mut.utils.math
  (:require [clojure.math.numeric-tower :as math]))

(defn distance [x y]
  (math/abs (- x y)))

(defn closest [coll x]
  (letfn [(distance-to-x [y] (distance x y))]
    (apply min-key distance-to-x coll)))
