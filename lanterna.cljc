#?(:bb (require '[babashka.pods :refer [load-pod]]))

#?(:bb
   ;; test pod with JVM:
   ;; (load-pod ["lein" "trampoline" "run" "-m" "pod.babashka.lanterna"]
   ;;           {:socket true
   ;;            :inherit-io true})
   (load-pod ["./pod-babashka-lanterna"] {:socket true
                                          :inherit-io true})
   :clj nil
   )

#?(:bb (require '[pod.babashka.lanterna.terminal :as terminal])
   ;; with clj, we require normal lanterna, to compare
   :clj (require '[lanterna.terminal :as terminal]))

#_:clj-kondo/ignore terminal/text-terminal

(def terminal (terminal/text-terminal))

(terminal/start terminal)
(terminal/put-string terminal "Hello TUI Babashka!" 10 5)
(terminal/flush terminal)

(terminal/get-key-blocking terminal)
