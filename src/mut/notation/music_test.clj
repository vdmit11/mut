(ns ^:test mut.notation.music-test
  (:require [clojure.test :refer :all]
            [mut.notation.music :refer :all]))


(deftest duration-is-1-beat-unless-it-is-specified
  (is (= 1 (duration-of {:type :chord :pitch 42})))
  (is (= 2 (duration-of {:type :chord :pitch 42 :duration 2}))))

(deftest duration-can-be-changed
  (let [mo1 {:type :note :pitch 60}
        mo2 (duration-change mo1 2/3)
        mo3 (duration-change mo2 3.0)]
    (is (= 1 (duration-of mo1)))
    (is (= 2/3 (duration-of mo2)))
    (is (= 3.0 (duration-of mo3)))
    (is (= mo1 {:type :note :pitch 60}))
    (is (= mo2 {:type :note :pitch 60 :duration 2/3}))
    (is (= mo3 {:type :note :pitch 60 :duration 3.0}))))

(deftest duration-can-be-scaled
  (is (= 4/3 (duration-of
              (duration-scale {:duration 2/3} 2)))))

(deftest duration-must-be-positive-number
  (is (thrown? Error (duration-of {:type :chord :duration -1})))
  (is (thrown? Error (duration-change {} 0)))
  (is (thrown? Error (duration-change {} -0.001)))
  (is (thrown? Error (duration-change {} "42")))
  (is (thrown? Error (duration-change {} nil)))
  (is (thrown? Error (duration-scale {} 0)))
  (is (thrown? Error (duration-scale {} -2/3))))



(deftest pitch-is-nil-unless-it-is-specified
  (is (nil? (pitch-of {:type :note})))
  (is (= 48.5 (pitch-of {:type :note :pitch 48.5}))))

(deftest pitch-can-be-changed
  (let [mo1 {:foo :bar :duration 21}
        mo2 (pitch-change mo1 69)
        mo3 (pitch-change mo2 71.5)]
    (is (= nil (pitch-of mo1)))
    (is (= 69 (pitch-of mo2)))
    (is (= 71.5 (pitch-of mo3)))
    (is (= mo1 {:foo :bar :duration 21}))
    (is (= mo2 {:foo :bar :duration 21 :pitch 69}))
    (is (= mo3 {:foo :bar :duration 21 :pitch 71.5}))))

(deftest pitch-may-be-shifted
  (let [note {:type :note :pitch 21}]
    (is (= 21 (pitch-of note)))
    (is (= 26 (pitch-of (pitch-shift note +5))))
    (is (= 16 (pitch-of (pitch-shift note -5))))))


(deftest pitch-must-be-in-range-0-127
  (is (thrown? Error (pitch-of {:type :note :pitch -0.001})))
  (is (thrown? Error (pitch-of {:type :note :pitch false})))
  (is (thrown? Error (pitch-change {} 128)))
  (is (thrown? Error (pitch-change {} "18")))
  (is (and (pitch-change {} 0) (pitch-change {} 127) )))




(deftest pause-has-duration
  (is (= 1 (duration-of (pause))))
  (is (= 2 (duration-of (pause :duration 2))))
  (is (thrown? Error (pause :duration -2))))


(deftest note-has-duration
  (is (= 1 (duration-of (note))))
  (is (= 0.0001 (duration-of (note :duration 0.0001))))
  (is (= 2/3 (duration-of (duration-change (note) 2/3))))  
  (is (thrown? Error (note :duration -42))))


(deftest note-has-pitch
  (is (= nil (pitch-of (note))))
  (is (= 100 (pitch-of (note :pitch 100))))
  (is (= 101 (pitch-of (pitch-change (note :pitch 100) 101))))
  (is (thrown? Error (pitch-change (note) -1)))
  (is (thrown? Error (note :pitch 128))))
