(ns mut.music.duration)

(defprotocol HasDuration
  (beats-of [mo])
  (beats-change [mo new-beats-val]))
