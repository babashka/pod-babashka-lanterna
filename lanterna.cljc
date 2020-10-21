#?(:bb (require '[babashka.pods :refer [load-pod]]))

#?(:bb
   ;; test pod with JVM:
   (load-pod ["lein" "trampoline" "run" "-m" "pod.babashka.lanterna"] {:transport :socket})
   #_(load-pod ["./pod-babashka-lanterna"] {:transport :socket})
   :clj nil
   )

#?(:bb (require '[pod.babashka.lanterna.terminal :as terminal])
   ;; with clj, we require normal lanterna, to compare
   :clj (require '[lanterna.terminal :as terminal]))

#_:clj-kondo/ignore terminal/get-terminal

(def terminal (terminal/get-terminal))

(terminal/start terminal)

(terminal/put-string terminal
                     (str "Hello TUI Babashka!")
                     10 5)
(terminal/put-string terminal
                     (str "The size of this terminal: "
                          (terminal/get-size terminal))
                     10 6)
(terminal/put-string terminal
                     "Press q to exit."
                     10 7)

(terminal/flush terminal)

(def k (terminal/get-key-blocking terminal))

(terminal/put-string terminal
                     (str "You pressed: " k)
                     10 8)

(Thread/sleep 1000)
