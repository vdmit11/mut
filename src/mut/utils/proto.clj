(ns mut.utils.proto)

(defn satisfies-every?
  [protocols obj]
  (every? #(satisfies? % obj) protocols))
