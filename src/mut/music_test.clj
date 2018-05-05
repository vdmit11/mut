(ns mut.music-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is]]
            [mut.utils.test :refer [is-not]]
            [mut.music]))

(deftest nested-mo-must-have-start-end-time
  (is
    (s/valid? :mut.music/container
      {:contents #{{:start-time 0 :end-time 1}
                   {:start-time 1 :end-time 2}}})
    "should be valid because both :start-time/:end-time are present in nested object")

  (is-not
    (s/valid? :mut.music/container
      {:contents #{{:start-time 0 :end-time 1}
                   {:end-time 2}}})
    "must be invalid because :start-time is missing")

  (is-not
    (s/valid? :mut.music/container
      {:contents #{{:start-time 0 :end-time 1}
                   {:start-time 1}}})
    "must be invalid because :end-time is missing"))
