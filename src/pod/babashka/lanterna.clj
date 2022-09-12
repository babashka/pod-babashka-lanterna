(ns pod.babashka.lanterna
  {:clj-kondo/config '{:lint-as {pod.babashka.lanterna/def-terminal-fn clojure.core/def
                                 pod.babashka.lanterna/def-screen-fn clojure.core/def}}}
  (:refer-clojure :exclude [read read-string flush])
  (:require [bencode.core :as bencode]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [lanterna.screen :as screen]
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

(defn get-terminal
  ([] (get-terminal :auto))
  ([kind] (get-terminal kind {}))
  ([kind opts]
   (let [t (terminal/get-terminal kind opts)
         id (str (java.util.UUID/randomUUID))]
     (swap! terminals assoc id t)
     {::terminal id})))

(defmacro def-terminal-fn
  ([f]
   `(defn ~(symbol (name f)) [m# & args#]
      (let [t# (resolve-terminal m#)]
        (let [v# (apply ~(symbol "terminal" (str/replace (str f) #"^t-" ""))
                        t# args#)]
          v#))))
  ([f ret]
   `(defn ~(symbol (name f)) [m# & args#]
      (let [t# (resolve-terminal m#)]
        (apply ~(symbol "terminal" (str/replace (str f) #"^t-" ""))
               t# args#)
        ~ret))))

(def-terminal-fn t-start nil)
(def-terminal-fn t-stop nil)
(def-terminal-fn t-put-string nil)
(def-terminal-fn t-flush nil)
(def-terminal-fn t-get-key)
(def-terminal-fn t-get-key-blocking)
(def-terminal-fn t-get-size)

(def screens (atom {}))

(defn resolve-screen [m]
  (get @screens (::screen m)))

(defn terminal-screen
  [m]
  (let [t (resolve-terminal m)
        s (screen/terminal-screen t)
        id (str (java.util.UUID/randomUUID))]
    (swap! screens assoc id s)
    {::screen id}))

(defmacro def-screen-fn
  ([f]
   `(defn ~(symbol (name f)) [m# & args#]
      (let [t# (resolve-screen m#)]
        (let [v# (apply ~(symbol "screen" (str/replace (str f) #"^s-" "")) t# args#)]
          v#))))
  ([f ret]
   `(defn ~(symbol (name f)) [m# & args#]
      (let [t# (resolve-screen m#)]
        (apply ~(symbol "screen" (str/replace (str f) #"^s-" "")) t# args#)
        ~ret))))

(def-screen-fn s-put-string nil)
(def-screen-fn s-redraw nil)
(def-screen-fn s-get-key)
(def-screen-fn s-get-key-blocking)
(def-screen-fn s-start nil)
(def-screen-fn s-stop nil)
(def-screen-fn s-get-size)

(def lookup*
  {'pod.babashka.lanterna.terminal
   {'get-terminal     get-terminal
    'start            t-start
    'stop             t-stop
    'put-string       t-put-string
    'flush            t-flush
    'get-key          t-get-key
    'get-key-blocking t-get-key-blocking
    'get-size         t-get-size}
   'pod.babashka.lanterna.screen
   {'terminal-screen terminal-screen
    'put-string       s-put-string
    'redraw           s-redraw
    'get-key          s-get-key
    'get-key-blocking s-get-key-blocking
    'start            s-start
    'stop             s-stop
    'get-size         s-get-size}})

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
                                (get lookup* 'pod.babashka.lanterna.terminal))}
                  {:name pod.babashka.lanterna.screen
                   :vars ~(mapv (fn [[k _]]
                                  {:name k})
                                (get lookup* 'pod.babashka.lanterna.screen))}]
     :ops {:shutdown {}}}))

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
                              (Thread. (fn []
                                         (.delete port-file))))
          _ (spit port-file
                  (str port "\n"))
          socket (doto (.accept server)
                   (.setTcpNoDelay true))
          in (PushbackInputStream. (.getInputStream socket))
          out (.getOutputStream socket)
          _message (try (read in)
                        (catch java.io.EOFException _
                          ::EOF))
          _ (write out (assoc describe-map
                              "port" port))]
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
                                (spit "/tmp/exception.log" (pr-str e))
                                (let [reply {"ex-message" (or (ex-message e)
                                                              "")
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
        (spit "/tmp/exception.log" e)))))
