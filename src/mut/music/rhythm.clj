(ns mut.music.rhythm)

(defrecord Pattern [duration contents])
(defrecord Beat [onset stress duration])

(defn beat [onset stress]
  (->Beat onset stress 1))

(defn stresses->beats [stress-levels]
  (set (map-indexed beat stress-levels)))

(defn pattern [stress-levels]
  (let [beats (stresses->beats stress-levels)
        duration (count beats)]
    (->Pattern duration beats)))
