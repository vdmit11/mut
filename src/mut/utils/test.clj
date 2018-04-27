(ns mut.utils.test)

(def ^:private DEFAULT-APPROX-DELTA 0.001)

(defn approx=
  ([n1 n2] (approx= n1 n2 DEFAULT-APPROX-DELTA))
  ([n1 n2 delta] (> delta (Math/abs (- n1 n2)))))
