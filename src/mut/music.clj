(ns mut.music
  "Music notation and theory library.

  This module implements tools for describing music.

  Here we define such things as Note, Chord, Phrase, Song, etc.
  All these things (we call them music objects) are building blocks for music.
  You can compose songs from music objects defined in this module.

  However, this module doesn't compose music by itself (it doesn't have functions that involve
  random generators or something), and it doesn't play the music (it doesn't do any I/O);
  generally it tries to avoid any non-pure functions and provide only tools.

  The module defines the data model - various objects and how they are related in music theory,
  but it doesn't apply this model to anything. The real work happens in higher-level modules
  who use this library to compose music objects together into something sensible.

  Why?
  ====

  There are other music packages available for Clojure, why not use them?

  Because other libraries are either:
   - abandoned
   - focused on human-readable notations and DSLs

  And for us, notations and notes are not the subject of major interest.
  We are more interested in containers (phrases, chord progressions, song sections, etc),
  and representing them in machine-oriented way, so that it would be easy for the code
  to analyze and navigate across them.

  And of course, I have a NIH-syndrome (Not Invented Here).
  Why use other's tools when you can create yours for fun?
  "
  (:require [clojure.spec.alpha :as s]))

;; Music Object
;; ============
;;
;; In music, there are a lot of things that have same properties.
;; Some most basic properties are pitch, duration and volume:
;;
;;  - Note usually has pitch, duration and volume (loudness)
;;  - Chord also has pitch (the root note), duration and volume
;;  - Phrase may have pitch (the tonic), and usually has duration and volume
;;  - entire Song definitely has duration and volume, and maybe even pitch
;;
;; And if you dive into music theory, you can find definitions for lots of objects like:
;; Chord, Arpeggio, Phrase, Figure, Period, Section, Voice, Part, Row, Measure, lots of them.
;;
;; And if you look into properties, you can find things like:
;; Scale Degree, Harmonic Function, Consonance, Tension, Accent, Articulation.
;;
;; And both lists (objects vs properties) are potentially open, as you may decide to add more terms.
;;
;; For this reason, we say that Music Object is a map.
;; Usually that means `(defrecord)`, but the point is that any object may have arbitrary properties.
;;
;;  - XXX: Previously, I tried to avoid this map assumption and define objects only using protocols.
;;    But, that quickly exploted to a lot of getter-setter protocols.
;;    Because most of the time, I want to somehow attach or get some information about the object.
;;    So fuck you, object is a map. Primitive types like `String` won't work as music objects.
;;    Deal with it.
(s/def ::object
  (s/keys :opt-un [::pitch ::duration ::volume]))

;; Containers
;; ==========
;;
;; A "container" is an object that can have other objects nested under the `:contents` key.
;;
;; Many objects may be containers for others, for example:
;;  - Song may contain Sections
;;  - Section may contain Phrases
;;  - Phrase may contain Notes
;;
;; Some objects (like a Chord) may be interpreted differently, depending on the context.
;; That is, you may decide to split a Chord to Nots, or interpret Chord as one atomic unit.
;;
;; So any Music Object that has `:contents` may act as a container for nested objects.
;;
;; XXX: Many good names (like "part" and "piece") are already occupied by the music theory.
;;      And many names (like "component" and "chunk") have meanings in programming.
;;      So naming may seem strange, but I didn't found anything better than "contained-object".
(s/def ::container
  (s/keys :req-un [::contents]))

;; The `:contents` is a set, where each object has `:start-time`/`:end-time` keys.
;;
;; The choice may seem odd. Why `set`? And why the time is stored inside nested objects,
;; and not is a citizen of the parent collection (that determines position of object)?
;;
;; Well, we (humans) percept music as a stream of sounding events.
;; If you try to recall a song, you almost certainly have to play it as a sequence in your head.
;; So this is why you naturally asssume a sequence - because you can't percept it in other way.
;; But machines can. They can remember all notes at once, and iterate over them in any chosen order.
;; So there is no need for a sequence.
;;
;; And also, sequences are bad at representing overlapping events.
;; How would you represent multiple voices sounding simultaneously using sequential collections?
;; The only way is to use special data structures like Interval Trees, but I'm not going to
;; complicate things with them.
;;
;; And finally, why would you prefer time over other properties?
;; For analysis purposes, you may decide to sort things by some kind of importance, not just time.
;; So this is why `:start-time`/`:end-time` are properties of the object and not container,
;; because you are free to choose any properties, not just time, so we just don't emphasize time.
(s/def ::contents
  (s/and set? (s/coll-of ::contained-object)))

(s/def ::contained-object
  (s/keys
    :req-un [::start-time ::end-time]))
