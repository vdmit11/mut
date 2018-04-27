(ns mut.music.builtins-test
  (:require [clojure.test :refer [are deftest]]
            [mut.utils.test :refer [approx=]]
            mut.music.builtins
            [mut.music.pitch :as pitch]))

(deftest number-as-pitch
  (are [n hz]
      (and (= (pitch/keynum-of n) n)
           (approx= (pitch/hz-of n) hz))
    0   8.1757989156
    1   8.6619572180
    2   9.1770239974
    68  415.3046975799
    69  440.0000000000
    70  466.1637615181
    125 11175.3034058561
    126 11839.8215267723
    127 12543.8539514160))
