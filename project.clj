(defproject babashka/pod-babashka-lanterna
  #=(clojure.string/trim
     #=(slurp "resources/POD_BABASHKA_LANTERNA_VERSION"))
  :description "babashka pod for lanterna"
  :url "https://github.com/babashka/pod-babashka-lanterna"
  :scm {:name "git"
        :url "https://github.com/babasha/pod-babashka-lanterna"}
  :license {:name "Eclipse Public License 1.0"
            :url "http://opensource.org/licenses/eclipse-1.0.php"}
  :source-paths ["src"]
  :resource-paths ["resources"]
  :dependencies [[org.clojure/clojure "1.10.2-alpha2"]
                 [babashka/clojure-lanterna "0.9.8-SNAPSHOT"]
                 [nrepl/bencode "1.1.0"]]
  :profiles {:uberjar {:global-vars {*assert* false}
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]
                       :aot :all
                       :main pod.babashka.lanterna}}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_user
                                    :password :env/clojars_pass
                                    :sign-releases false}]])
