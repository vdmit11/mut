(ns mut.utils.test
  (:require [clojure.test :as test]))

(defmacro is-not
  ([form] `(test/is (not ~form)))
  ([form msg] `(test/is (not ~form) ~msg)))
