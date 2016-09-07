(ns mut.notation.music
  "A set of tools for describing music.

  When people try to describe and analyze music, they define a musical notation
  like tabulature or letters or so called 'Modern Notation'.
  These systems are good for humans, but not that good for computers.
  This module solves the problem by defining its own notation which is good for
  use with clojure.

  The responsibility of this module is not to play music, but to describe it.
  The description should be stateless, and the module should contain only data types
  and pure functions that allow to transform these data types.

  Although this module describes a notation, it uses some conventional terms
  like Beat, Duration, Pitch, Note, Interval, Chord, etc.
  So you need some minimal knowledge of music theory to understand it."
)

(defn dispatch-musical-object [mo & _ ] :type)


(defmulti duration-of dispatch-musical-object)
(defmulti duration-change dispatch-musical-object)
(defmulti duration-scale dispatch-musical-object)
(defmulti pitch-of dispatch-musical-object)
(defmulti pitch-change dispatch-musical-object)
(defmulti pitch-shift dispatch-musical-object)


(defn duration-is-valid? [val]
  (and (number? val) (pos? val)))

(defmethod duration-of :default [mo]
  {:post [(duration-is-valid? %)]}
  (get mo :duration 1))

(defmethod duration-change :default [mo val]
  {:pre [(duration-is-valid? val)]} 
  (assoc mo :duration val))

(defmethod duration-scale :default [mo factor]
  (duration-change mo (* factor (duration-of mo))))


(defn pitch-is-valid? [val]
  (or (nil? val)
      (and (number? val) (<= 0 val 127))))

(defmethod pitch-of :default [mo]
  {:post [(pitch-is-valid? %)]}
  (get mo :pitch))

(defmethod pitch-change :default [mo val]
  {:pre [(pitch-is-valid? val)]}
  (assoc mo :pitch val))

(defmethod pitch-shift :default [mo offset]
  (if (nil? (pitch-of mo))
    mo
    (pitch-change mo (+ offset (pitch-of mo)))))


(defrecord Pause [duration])

(defn pause [& {:keys [duration] :or {duration 1}}]
   (-> (->Pause nil)
       (duration-change duration)))


(defrecord Note [duration pitch])

(defn note [& {:keys [duration pitch] :or {duration 1}}]
  (-> (->Note nil nil)
      (duration-change duration)
      (pitch-change pitch)))
