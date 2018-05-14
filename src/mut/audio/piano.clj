(ns mut.audio.piano
  (:require pink.instruments.piano
            [mut.audio :refer [at MusicObject->AudioEngineEvents]]
            [mut.music.pitch :as pitch]))

(comment "WIP"
(defrecord Piano [])

(defn note->event
  [_ time note]
  (at time
      (pink.instruments.piano/piano
       :keynum (pitch/keynum-of note))))

(defn mo->events
  [_ time mo]
  (if-not (seqable? mo)
    (note->event _ time mo)
    (map-indexed #(mo->events _ (+ time %1) %2) (seq mo))))

(extend Piano MusicObject->AudioEngineEvents {:mo->events mo->events})

(def piano (->Piano))
)

;; demo
(comment
  (require '[mut.audio :refer [start-engine stop-engine clear-engine play]])

  (start-engine)
  (Thread/sleep 100)

  (do
    (play piano 60)
    (Thread/sleep 1000)
    (play piano 62)
    (Thread/sleep 1000)
    (play piano 65)
    (Thread/sleep 1000))

  (play piano [60 62 65])
  (Thread/sleep 3000)

  (Thread/sleep 1000)
  (clear-engine)
  (stop-engine))
