(ns mut.music.pitch-test
  (:require [clojure.test :refer [are deftest]]
            [mut.utils.math :refer [approx=]]
            [mut.music.pitch :as pitch]))

(deftest midi-keynum-to-hz-conversion
  (are [n hz]
      (and (approx= (pitch/hz->keynum hz) n)
           (approx= (pitch/keynum->hz n) hz))
    0   8.1757989156
    1   8.6619572180
    2   9.1770239974
    68  415.3046975799
    69  440.0000000000
    70  466.1637615181
    125 11175.3034058561
    126 11839.8215267723
    127 12543.8539514160))
