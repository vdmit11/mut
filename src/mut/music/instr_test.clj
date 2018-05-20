(ns mut.music.instr-test
  (:require [clojure.test :refer [deftest are]]
            [mut.music.instr :as music.instr]))

(deftest instrument-type-is-part-of-id-keyword
  (are [kw type]
      (= type (music.instr/id->type kw))
    :guitar-0 :guitar
    :guitar-1 :guitar
    :guitar :guitar
    :guitar2-1 :guitar2
    :tes.t1-2-3 :tes.t1-2))
