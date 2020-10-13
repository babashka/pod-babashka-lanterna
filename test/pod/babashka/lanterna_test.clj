(ns pod.babashka.lanterna-test
  (:require [babashka.pods :as pods]
            [pod.babashka.lanterna :as lanterna]
            [clojure.test :refer [deftest is testing]]))

(pods/load-pod (if (= "native" (System/getenv "POD_TEST_ENV"))
                 "./pod-babashka-lanterna"
                 ["lein" "run" "-m" "pod.babashka.lanterna"])
               {:socket true
                :inherit-io true})

(require '[pod.babashka.lanterna.terminal :as terminal])

(deftest lanterna-test
  (let [terminal (terminal/text-terminal)]
    (terminal/start terminal)
    (terminal/put-string terminal "Hello TUI Babashka!" 10 5)
    (terminal/flush terminal)))
