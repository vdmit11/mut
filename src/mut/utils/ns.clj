(ns mut.utils.ns
  (:require [clojure.string :refer [split join]]
            [coercer.core :refer [coerce]]
            [bultitude.core :refer [namespaces-on-classpath]]))

(def Symbol clojure.lang.Symbol)
(def Namespace clojure.lang.Namespace)

(defmethod coerce [Symbol Namespace]
  [sym _]
  (find-ns sym))

(defmethod coerce [String Namespace]
  [s _]
  (find-ns (symbol s)))

(defmethod coerce [Namespace Symbol]
  [ns _]
  (ns-name ns))

(defmethod coerce [Namespace String]
  [ns _]
  (str (ns-name ns)))

(defn find-defs [pred ns]
  (into {}
    (for [[name var] (ns-publics (coerce ns Namespace))
          :let [value (var-get var)]
          :when (pred value)]
      [(str name) value])))

(defn require-and-find-defs [pred ns-name]
  (require ns-name)
  (find-defs pred ns-name))

(defn split-ns-name
  [ns]
  (split (coerce ns String) #"\."))

(defn discover-sub-namespaces
  [parent-ns]
  (namespaces-on-classpath :prefix (coerce parent-ns String)))

(defn load-and-find-defs-in-sub-namespaces [pred parent-ns]
  (mapcat
    (partial require-and-find-defs pred)
    (discover-sub-namespaces parent-ns)))

(defn flatten-recursively [obj]
  (if-not (seqable? obj)
    (list obj)
    (mapcat flatten-recursively (seq obj))))

(defn find-symbols-in-form [form]
  (filter symbol? (flatten-recursively form)))

(defn load-namespace-of-symbol [sym]
  (some-> (namespace sym) symbol require))

(defn load-namespaces-of-symbols [seq-of-symbols]
  (doall (map load-namespace-of-symbol seq-of-symbols)))

(defn load-all-namespaces-of-form [form]
  (load-namespaces-of-symbols (find-symbols-in-form form)))

(defmacro autoload-namespaces
  [& body]
  `(do
     (load-all-namespaces-of-form (quote ~body))
     ~@body))
