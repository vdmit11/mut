(ns mut.music-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is]]
            [mut.utils.test :refer [is-not]]
            [mut.music]))

(deftest nested-mo-must-have-offset-and-duration
  (is
    (s/valid? :mut.music/container
      {:contents #{{:onset 0 :duration 1}
                   {:onset 1 :duration 2}}})
    "should be valid because both :offset/:duration are present in nested object")

  (is-not
    (s/valid? :mut.music/container
      {:contents #{{:onset 0 :duration 1}
                   {:duration 2}}})
    "must be invalid because :onset is missing")

  (is-not
    (s/valid? :mut.music/container
      {:contents #{{:onset 0 :duration 1}
                   {:onset 1}}})
    "must be invalid because :duration is missing"))
