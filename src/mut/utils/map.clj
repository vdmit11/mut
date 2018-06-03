(ns mut.utils.map
  (:require [mut.utils.math :as utils.math]))

(defn get-closest [map x]
  (get map (utils.math/closest (keys map) x)))
