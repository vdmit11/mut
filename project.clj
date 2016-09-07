(defproject cear "SNAPSHOT"
  :description "Music Tool"
  :url "https://github.com/vdmit11/mut"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.generators "0.1.2"]]
  :target-path "target/%s/"
  :src-paths ["src/"]
  :test-paths ["src/"]
  :main mut.core)
