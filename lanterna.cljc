#?(:bb (require '[babashka.pods :refer [load-pod]]))

;; test pod with JVM:
#_(load-pod ["lein" "trampoline" "run" "-m" "pod.babashka.lanterna"]
            {:socket true
             :inherit-io true})

#?(:bb (load-pod ["./pod-babashka-lanterna"] {:socket true
                                              :inherit-io true}))

#?(:bb (require '[pod.babashka.lanterna.terminal :as terminal])
   ;; with clj, we require normal lanterna, to compare
   :clj (require '[lanterna.terminal :as terminal]))

#_:clj-kondo/ignore terminal/text-terminal

(def terminal (terminal/text-terminal))

(terminal/start terminal)
(terminal/put-string terminal "Hello TUI Babashka!" 10 5)
(terminal/flush terminal)

;; TODO: why doesn't get-key-blocking work in the bb pod?
#?(:bb (read-line)
   :clj (terminal/get-key-blocking terminal))
