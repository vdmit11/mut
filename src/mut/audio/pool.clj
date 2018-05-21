(ns mut.audio.pool
  "Pool of instruments and engines.

  Problem: in order to play audio, you need to allocate resources, like:
   - audio engine (`pink.engine.Engine`) running in a separate Thread
   - audio node (`pink.node.Node`) for each instrument
   - mutable instrument state (low-level audio buffers)

  And in addition, you have to free all those resources manually
  (because GC doesn't know how to stop Threads and close audio outputs).

  So I (as a music generator, let't say) don't want to do that manually.
  I would like these things to happen automatically under the hood,
  while music is being synthesized/played.

  I would like to be able to just write some music score that looks as pure data,
  that doesn't refer to any stateful objects (like threads or I/O ports).
  I imagine it looking like this:

    (def score #{{:instr :guitar-1 :contents [...]}
                 {:instr :guitar-2 :contents [...]}
                 {:instr :bass :contents [...]}
                 {:instr :drums :contents [...]}})

  Here I'm referring instruments by name, like `:guitar-1` and `:guitar-2`,
  and I expect them to be allocated/deallocated automatically as the code enters/exits
  corresponding sections of the music score during audio synthesis process.

  So this is what this module is about - resolving IDs into instrument objects,
  and accounting resources in global pools (with automatic freeing).
  "
  (:require pink.engine
            pink.node
            [clojure.spec.alpha :as s]
            [mut.music.instr :as music.instr]))

(defonce ^:dynamic *current-pool* (ref nil))

(defrecord AudioPool [engine instrs])

(defn new-pool []
  (map->AudioPool
    {:engine (pink.engine/engine-create :nchnls 2)
     :instrs {}}))

(defn get-instr- [pool id]
  (get (:instrs pool) id))

(defn get-instr [id]
  (get-instr- @*current-pool* id))

(defn- assoc-instr- [pool id instr]
  (assoc-in pool [:instrs id] instr))

;; Instrument allocation

(defmulti map->instr :type)

(defn- new-instr [id engine]
  (map->instr {:id id
               :type (music.instr/id->type id)
               :engine engine
               :node (pink.node/mixer-node)}))

(defn alloc-instr [id]
  (dosync
    (let [pool (ensure *current-pool*)]
      (or
        (get-instr- pool id)
        (let [instr (new-instr id (:engine pool))]
          (alter *current-pool* #(assoc-instr- % id instr))
          (pink.engine/engine-add-afunc (:engine pool) (:node instr))
          instr)))))


;; auto-initialize *current-pool* (create `pink.engine.Engine` object, but not yet start the Thread)
(dosync
  (when-not @*current-pool*
    (ref-set *current-pool* (new-pool))))

;; Utils for testing

(defmacro with-new-pool [& body]
  `(binding [*current-pool* (ref (new-pool))]
     ~@body))
