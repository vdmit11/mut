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

    (def score #{{:instrument :guitar-1 :contents [...]}
                 {:instrument :guitar-2 :contents [...]}
                 {:instrument :bass :contents [...]}
                 {:instrument :drums :contents [...]}})

  Here I'm referring instruments by name, like `:guitar-1` and `:guitar-2`,
  and I expect them to be allocated/deallocated automatically as the code enters/exits
  corresponding sections of the music score during audio synthesis process.

  So this is what this module is about - resolving IDs into instrument objects,
  and accounting resources in global pools (with automatic freeing).
  ")

(defn- remove-kw-digits-suffix [kw]
  (let [str (name kw)
        [_ str-without-suffix] (re-find #"^(.+)-\d+$" str)
        kw-without-suffix (keyword str-without-suffix)]
    (or kw-without-suffix kw)))

(defn get-instrument-type
  "Extract type from instrument ID keyword.

  Examples:
     (get-instrument-type :electric-guitar) => :electric-guitar
     (get-instrument-type :electric-guitar-1) => :electric-guitar
     (get-instrument-type :electric-guitar-2) => :electric-guitar

  Basically that removes the number from the end of the keyword.

  But the sense of this operation is that the input keyword is instrument ID,
  used in a musical score. And we define it to have the following format:

    :[TYPE]-[NUMBER]

  So for example `:guitar-1` can be split to `:guitar` (type) and `1` (number of the instrument).

  This is a bit silly from programmer's point of view, but this is the usual way for wirting
  musical score, like saying 'Gutar #1 plays this part, and Guitar #2 plays that part'.
  And that works for us (and we're free to switch to something more complex in future if needed).

  So essentially, this function extracts the instrument type that is encoded inside ID keyword.
  "
  [id-kw]
  (remove-kw-digits-suffix id-kw))
