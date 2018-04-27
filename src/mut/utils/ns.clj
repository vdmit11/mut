(ns mut.utils.ns)

(defn find-defs [pred ns]
  (into {}
    (for [[name var] (ns-publics ns)
          :let [value (var-get var)]
          :when (pred value)]
      [(str name) value])))
