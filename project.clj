(defproject mut "SNAPSHOT"
  :jvm-opts
  ^:replace
  ["-server"
   "-Xms512m" "-Xmx1g"
   "-XX:+UseG1GC"
   "-XX:MaxGCPauseMillis=1"
   "-XX:TieredStopAtLevel=1"
   "-XX:+UseTLAB"]

  :description "Music Tool"
  :url "https://github.com/vdmit11/mut"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}
  :target-path "target/%s/"
  :src-paths ["src/"]
  :test-paths ["src/"]
  :main mut.core
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.generators "0.1.2"]
                 [kunstmusik/pink "0.4.1"]
                 [acyclic/squiggly-clojure "0.1.8"]
                 [swiss-arrows "1.0.0"]
                 [com.rpl/specter "1.1.0"]]
  :plugins [[lein-ancient "0.6.15"]
            [cider/cider-nrepl "0.16.0"]
            [jonase/eastwood "0.2.5"]
            [lein-kibit "0.1.6"]
            [lein-bikeshed "0.5.0"]
            [lein-environ "1.0.2"]]
  :env {:squiggly {:checkers [:eastwood :kibit]}})
