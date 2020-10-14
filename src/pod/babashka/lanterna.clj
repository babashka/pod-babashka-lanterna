(ns pod.babashka.lanterna
  {:clj-kondo/config '{:lint-as {pod.babashka.lanterna/def-terminal-fn clojure.core/def}}}
  (:refer-clojure :exclude [read read-string flush])
  (:require [bencode.core :as bencode]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk]
            [lanterna.terminal :as terminal])
  (:import [java.io PushbackInputStream]
           [java.net ServerSocket])
  (:gen-class))

(set! *warn-on-reflection* true)

(def stdin (PushbackInputStream. System/in))

(def debug? false)

(defn debug [& strs]
  (when debug?
    (binding [*out* (io/writer System/err)]
      (apply prn strs))))

(defn write [stream v]
  (debug :writing v)
  (bencode/write-bencode stream v))

(defn read-string [^"[B" v]
  (String. v))

(defn read [stream]
  (bencode/read-bencode stream))

(def terminals (atom {}))

(defn resolve-terminal [m]
  (get @terminals (::terminal m)))

(defn get-terminal []
  (let [t (terminal/get-terminal)
        id (str (java.util.UUID/randomUUID))]
    (swap! terminals assoc id t)
    {::terminal id}))

(defmacro def-terminal-fn
  ([f]
   `(defn ~(symbol (name f)) [m# & args#]
      (let [t# (resolve-terminal m#)]
        (let [v# (apply ~(symbol "terminal" (str f)) t# args#)]
          v#))))
  ([f ret]
   `(defn ~(symbol (name f)) [m# & args#]
      (let [t# (resolve-terminal m#)]
        (apply ~(symbol "terminal" (str f)) t# args#)
        ~ret))))

(def-terminal-fn start nil)
(def-terminal-fn stop nil)
(def-terminal-fn put-string nil)
(def-terminal-fn flush nil)
(def-terminal-fn get-key)
(def-terminal-fn get-key-blocking)
(def-terminal-fn get-size)

(def lookup*
  {'pod.babashka.lanterna.terminal
   {'get-terminal     get-terminal
    'start            start
    'stop             stop
    'put-string       put-string
    'flush            flush
    'get-key          get-key
    'get-key-blocking get-key-blocking
    'get-size         get-size}})

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
                   :vars ~(mapv (fn [[k _]]
                                  {:name k})
                              (get lookup* 'pod.babashka.lanterna.terminal))}]
     :opts {:shutdown {}}}))

(debug describe-map)

(defn create-server
  "Initialise a ServerSocket on localhost using a port.
  Passing in 0 for the port will automatically assign a port based on what's
  available."
  [^Integer port]
  (ServerSocket. port))

(defn -main [& _args]
  (try
    (let [server (ServerSocket. 0)
          port (.getLocalPort server)
          pid (.pid (java.lang.ProcessHandle/current))
          port-file (io/file (str ".babashka-pod-" pid ".port"))
          _ (.addShutdownHook (Runtime/getRuntime)
                              (Thread. (fn [] (.delete port-file))))
          _ (spit port-file
                  (str port "\n"))
          socket (.accept server)
          in (PushbackInputStream. (.getInputStream socket))
          out (.getOutputStream socket)
          _message (try (read in)
                        (catch java.io.EOFException _
                          ::EOF))
          _ (write out (assoc describe-map
                              "port" 1888))]
      (loop []
        (let [message (try (read in)
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
                :describe (do (write out describe-map)
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
                                    (write out reply))
                                  (throw (ex-info (str "Var not found: " var) {}))))
                              (catch Throwable e
                                (debug e)
                                (let [reply {"ex-message" (ex-message e)
                                             "ex-data" (pr-str
                                                        (assoc (ex-data e)
                                                               :type (class e)))
                                             "id" id
                                             "status" ["done" "error"]}]
                                  (write out reply))))
                            (recur))
                :shutdown (System/exit 0)
                (do
                  (let [reply {"ex-message" "Unknown op"
                               "ex-data" (pr-str {:op op})
                               "id" id
                               "status" ["done" "error"]}]
                    (write out reply))
                  (recur))))))))
    (catch Throwable e
      (binding [*out* *err*]
        (prn e)
        #_(spit "/tmp/exception.log" e)))))
