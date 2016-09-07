(ns mut.core
  (:gen-class)
  (:require mut.midi.output))

(defn -main []
  (mut.midi.output/play-midi-chromatic-demo))
