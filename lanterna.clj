(require '[babashka.pods :refer [load-pod]])

;; test with JVM:
#_(load-pod ["lein" "trampoline" "run" "-m" "pod.babashka.lanterna"]
            {:socket true
             :inherit-io true})

(load-pod ["./pod-babashka-lanterna"] {:socket true
                                       :inherit-io true})

(require '[pod.babashka.lanterna.terminal :as terminal])

(def terminal (terminal/text-terminal))

(terminal/start terminal)
(terminal/put-string terminal "Hello TUI Babashka!" 10 5)
(terminal/flush terminal)
#_(terminal/get-key-blocking terminal)

(read-line)
