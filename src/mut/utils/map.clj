(ns mut.utils.map
  (:require [mut.utils.math :as utils.math]))

(defn get-closest [map x]
  (get map (utils.math/closest (keys map) x)))

(defn map-contains-submap? [super-map sub-map]
  (every?
    (fn [key]
      (and (contains? super-map key)
           (= (get super-map key) (get sub-map key))))
    (keys sub-map)))
