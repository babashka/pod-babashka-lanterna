(ns pod.babashka.lanterna
  (:refer-clojure :exclude [read read-string flush])
  (:require [bencode.core :as bencode]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [lanterna.terminal :as terminal])
  (:import [java.io PushbackInputStream])
  (:gen-class))

(def stdin (PushbackInputStream. System/in))

(def debug? true)

(defn debug [& strs]
  (when debug?
    (binding [*out* (io/writer System/err)]
      (apply prn strs))))

(defn write [v]
  (debug :writing v)
  (bencode/write-bencode System/out v)
  (.flush System/out))

(defn read-string [^"[B" v]
  (String. v))

(defn read []
  (bencode/read-bencode stdin))

(def terminals (atom {}))

(defn text-terminal
  ([] (let [t (terminal/text-terminal)
            id (str (java.util.UUID/randomUUID))]
        (swap! terminals assoc id t)
        {::terminal id}))
  ([m]
   (get @terminals (::terminal m))))

(defn start [m]
  (debug :starting m)
  (let [t (text-terminal m)]
    #_(terminal/start t)
    nil))

(defn stop [m]
  (debug :stopping m)
  (let [t (text-terminal m)]
    (terminal/stop t)
    {}))

(defn put-string [m & args]
  (apply debug :put-string m args)
  (let [t (text-terminal m)]
    #_(apply terminal/put-string t args)
    nil))

(defn flush [m]
  (debug :flush m)
  (let [t (text-terminal m)]
    (terminal/flush t)
    nil))

(def lookup*
  {'pod.babashka.lanterna.terminal
   {'text-terminal text-terminal
    'start         start
    'stop          stop
    'put-string    put-string
    'flush         flush}})

(defn lookup [var]
  (let [var-ns (symbol (namespace var))
        var-name (symbol (name var))]
    (get-in lookup* [var-ns var-name])))

(def describe-map
  (walk/postwalk
   (fn [v]
     (if (ident? v) (name v)
         v))
   `{:format :edn
     :namespaces [{:name pod.babashka.lanterna.terminal
                   :vars [{:name text-terminal}
                          {:name start}
                          {:name put-string}
                          {:name flush}
                          {:name stop}]}]
     :opts {:shutdown {}}}))

(debug describe-map)

(defn -main [& _args]
  (loop []
    (let [message (try (read)
                       (catch java.io.EOFException _
                         ::EOF))]
      (when-not (identical? ::EOF message)
        (let [op (get message "op")
              op (read-string op)
              op (keyword op)
              id (some-> (get message "id")
                         read-string)
              id (or id "unknown")]
          (case op
            :describe (do (write describe-map)
                          (recur))
            :invoke (do (try
                          (let [var (-> (get message "var")
                                        read-string
                                        symbol)
                                args (get message "args")
                                args (read-string args)
                                args (edn/read-string args)]
                            (if-let [f (lookup var)]
                              (let [value (pr-str (apply f args))
                                    _ (debug "return value" value)
                                    reply {"value" value
                                           "id" id
                                           "status" ["done"]}]
                                (write reply))
                              (throw (ex-info (str "Var not found: " var) {}))))
                          (catch Throwable e
                            (debug e)
                            (let [reply {"ex-message" (ex-message e)
                                         "ex-data" (pr-str
                                                    (assoc (ex-data e)
                                                           :type (class e)))
                                         "id" id
                                         "status" ["done" "error"]}]
                              (write reply))))
                        (recur))
            :shutdown (System/exit 0)
            (do
              (let [reply {"ex-message" "Unknown op"
                           "ex-data" (pr-str {:op op})
                           "id" id
                           "status" ["done" "error"]}]
                (write reply))
              (recur))))))))
