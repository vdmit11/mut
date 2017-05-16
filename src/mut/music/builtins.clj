(ns mut.music.builtins
  (:require [mut.music.pitch :as pitch]
            [mut.music.duration :as duration]))


(extend-type java.lang.Number
  pitch/HasPitch
  (hz-of [n] (pitch/keynum->hz n))
  (keynum-of [n] n)

  duration/HasDuration
  (beats-of [n] 1))
