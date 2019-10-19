;; Just some random prototype code that should be finished and evicted to other namespaces.
(ns mut.playground
  (:require [mut.audio :as audio]))


(for [n (range 4)]
  (do
    (audio/play! {:offset (+ n 1/4) :stress 1})
    (audio/play! {:offset (+ n 2/4) :stress 0})
    (audio/play! {:offset (+ n 3/4) :stress 0})
    (audio/play! {:offset (+ n 4/4) :stress 0})))
