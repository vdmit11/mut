(defproject mut "SNAPSHOT"
  :jvm-opts
  ^:replace
  [
   ;; tweaks to minimize GC times, for Pink audio engine
   "-server"
   "-Xms512m" "-Xmx1g"
   "-XX:+UseG1GC"
   "-XX:MaxGCPauseMillis=1"
   "-XX:+UseTLAB"
   ;; start time improvements
   "-XX:+AggressiveOpts"
   "-XX:+TieredCompilation"
   "-XX:TieredStopAtLevel=1"
   "-XX:+CMSClassUnloadingEnabled"
   "-Xverify:none"
   ]

  :description "Music Tool"
  :url "https://github.com/vdmit11/mut"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}
  :target-path "target/%s/"
  :src-paths ["src/"]
  :test-paths ["src/"]
  :main mut.core
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.generators "0.1.2"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/test.check "0.10.0"]
                 [acyclic/squiggly-clojure "0.1.8"]
                 [bultitude "0.2.8"]
                 [clj-kondo "2019.10.11-alpha"]
                 [coercer "0.2.0"]
                 [com.rpl/specter "1.1.3"]
                 [com.taoensso/truss "1.5.0"]
                 [kunstmusik/pink "0.4.1"]
                 [medley "1.2.0"]
                 [swiss-arrows "1.0.0"]]
  :plugins [[lein-ancient "0.6.15"]
            [cider/cider-nrepl "0.23.0-SNAPSHOT"]
            [refactor-nrepl "2.5.0-SNAPSHOT"]
            [jonase/eastwood "0.3.6"]
            [lein-kibit "0.1.7"]
            [lein-bikeshed "0.5.2"]
            [lein-environ "1.0.2"]
            [lein-marginalia "0.9.1"]]
  :aliases {"clj-kondo" ["run" "-m" "clj-kondo.main"]}
  :env {:squiggly {:checkers [:eastwood :kibit]}})
