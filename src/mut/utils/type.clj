(ns mut.utils.type
  (:require [clojure.reflect :as r]))

(defn class-name [obj]
  (r/typename (class obj)))

(defn satisfies-every?
  [protocols obj]
  (every? #(satisfies? % obj) protocols))
