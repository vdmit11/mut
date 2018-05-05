(ns mut.music
  (:require [clojure.spec.alpha :as s]))

(s/def ::object map?)

(s/def ::container
  (s/keys :req-un [::contents]))

(s/def ::contents
  (s/and set? (s/coll-of ::contained-object)))

(s/def ::contained-object
  (s/keys
    :req-un [::start-time ::end-time]))
