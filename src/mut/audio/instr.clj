(ns mut.audio.instr
  "spec/protocol for Pink-based instruments.

  Pink is an audio engine (see 3rd-party `pink` package) that provides ability to synthesize
  audio, and we use that to define different kinds of instruments for playing music.

  In terms of Pink, an instrument is a function that returns an audio function
  (also in other audio engines, the common name for that is UGen, or Unit Generator).

  But, we don't use Pink's notion of instrument (a function).
  For us, an instrument is rather a record (as in `defrecord`).

  That allows us to be more flexible, like:
   - have more than 1 function per instrument (produce different sounds for same instrument)
   - attach arbitrary properties to instrument, like: settings, mutable state, mixer node, etc
   - define abstract protocols and methods implemented by instrument code (use type-based dispatch)

  So we implement instruments as a records (not functions),
  and this module contains things common to all such instrument records,
  that include:
    - specs for the instrument record, that should be respected by instrument implementations
    - protocols and multimethods that can be implemented (or overriden) by instruments
    - various utility functions, re-used over instrument implementations
  "
  (:require [clojure.spec.alpha :as s]
            [mut.audio.pink-utils :as pink-utils]))

;; An Instrument is a record that, has several required fields:
;;   1. `:engine` - the `pink.engine.Engine`, that runs a background Thread that produces audio.
;;   2. `:node` - the `pink.node/mixer-node`, that can be used to adjust gain/panning.
;;
;; Different instruments may share the same `:engine` (and thus be executed in the same Thread),
;; but each instrument has its own private `:node`, to which the instrument outputs the sound.
;;
;; Instruments are free to define any additional fields (including ones that store mutable state).
(s/def ::instrument
  (s/and
    record?  ; instrument must be a record because type-based dispatch is needed
    (s/keys :req-un [::node ::engine])))

(s/def ::engine pink-utils/engine?)

(s/def ::node pink-utils/mixer-node?)
