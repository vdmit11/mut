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
  ")
