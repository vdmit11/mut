(ns mut.audio.pool-test
  (:require [clojure.test :refer [deftest are]]
            [mut.audio.pool :as pool]))

(deftest instrument-type-is-part-of-id-keyword
  (are [kw type]
      (= type (pool/id->type kw))
    :guitar-0 :guitar
    :guitar-1 :guitar
    :guitar :guitar
    :guitar2-1 :guitar2
    :tes.t1-2-3 :tes.t1-2))
