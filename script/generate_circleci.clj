#!/usr/bin/env bb

(ns generate-circleci
  (:require [flatland.ordered.map :refer [ordered-map]]))

(def linux
  (ordered-map :docker [{:image "circleci/clojure:lein-2.8.1"}]
               :working_directory "~/repo"
               :environment (ordered-map :LEIN_ROOT "true"
                                         :GRAALVM_HOME "/home/circleci/graalvm-ce-java11-22.3.1"
                                         :BABASHKA_PLATFORM "linux"
                                         :BABASHKA_TEST_ENV "native"
                                         :BABASHKA_STATIC true
                                         :BABASHKA_MUSL true
                                         :BABASHKA_XMX "-J-Xmx7g"
                                         :POD_TEST_ENV "native")
               :resource_class "large"
               :steps ["checkout"
                       {:run {:name "Pull Submodules",
                              :command "git submodule init\ngit submodule update\n"}}
                       {:restore_cache {:keys ["linux-{{ checksum \"project.clj\" }}-{{ checksum \".circleci/config.yml\" }}"]}}
                       {:run {:name "Install Clojure",
                              :command "
wget https://download.clojure.org/install/linux-install-1.11.1.1224.sh
chmod +x linux-install-1.11.1.1224.sh
sudo ./linux-install-1.11.1.1224.sh"}}
                       {:run {:name "Install lsof",
                              :command "sudo apt-get install lsof\n"}}
                       {:run {:name "Install native dev tools",
                              :command "sudo apt-get update\nsudo apt-get -y install gcc g++ zlib1g-dev make\n"}}
                       {:run {:name "Install musl",
                              :command "sudo -E script/setup-musl"}}
                       {:run {:name "Download GraalVM",
                              :command "
cd ~
if ! [ -d graalvm-ce-java11-22.3.1 ]; then
  curl -O -sL https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.1/graalvm-ce-java11-linux-amd64-22.3.1.tar.gz
  tar xzf graalvm-ce-java11-linux-amd64-22.3.1.tar.gz
fi"}}
                       {:run {:name "Build binary",
                              :command "# script/uberjar\nscript/compile\n",
                              :no_output_timeout "30m"}}
                       {:run {:name "Run tests",
                              :command "script/test\n"}}
                       {:run {:name "Release",
                              :command ".circleci/script/release\n"}}
                       {:save_cache {:paths ["~/.m2"
                                             "~/graalvm-ce-java11-22.3.1"],
                                     :key "linux-{{ checksum \"project.clj\" }}-{{ checksum \".circleci/config.yml\" }}"}}
                       {:store_artifacts {:path "/tmp/release",
                                          :destination "release"}}]))

(def mac
  (ordered-map :macos {:xcode "14.0.0"},
               :environment (ordered-map :GRAALVM_HOME "/Users/distiller/graalvm-ce-java11-22.3.1/Contents/Home",
                                         :BABASHKA_PLATFORM "macos",
                                         :BABASHKA_TEST_ENV "native",
                                         :BABASHKA_XMX "-J-Xmx7g"
                                         :POD_TEST_ENV "native"),
               :resource_class "large",
               :steps ["checkout"
                       {:run {:name "Pull Submodules",
                              :command "git submodule init\ngit submodule update\n"}}
                       {:restore_cache {:keys ["mac-{{ checksum \"project.clj\" }}-{{ checksum \".circleci/config.yml\" }}"]}}
                       {:run {:name "Install Clojure",
                              :command "script/install-clojure /usr/local\n"}}
                       {:run {:name "Install Leiningen",
                              :command "script/install-leiningen\n"}}
                       {:run {:name "Download GraalVM",
                              :command "
cd ~
ls -la
if ! [ -d graalvm-ce-java11-22.3.1 ]; then
  curl -O -sL https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.1/graalvm-ce-java11-darwin-amd64-22.3.1.tar.gz
  tar xzf graalvm-ce-java11-darwin-amd64-22.3.1.tar.gz
fi"}}
                       {:run {:name "Build binary",
                              :command "# script/uberjar\nscript/compile\n",
                              :no_output_timeout "30m"}}
                       {:run {:name "Run tests",
                              :command "script/test\n"}}
                       {:run {:name "Release",
                              :command ".circleci/script/release\n"}}
                       {:save_cache {:paths ["~/.m2"
                                             "~/graalvm-ce-java11-22.3.1"],
                                     :key "mac-{{ checksum \"project.clj\" }}-{{ checksum \".circleci/config.yml\" }}"}}
                       {:store_artifacts {:path "/tmp/release",
                                          :destination "release"}}]))

(def config
  (ordered-map
   :version 2.1,
   :jobs (ordered-map
          :linux linux
          :mac mac)
   :workflows (ordered-map
               :version 2
               :ci {:jobs ["linux"
                           "mac"]})))

(require '[clj-yaml.core :as yaml])
(spit ".circleci/config.yml"
      (str "# This file is generated by script/generate_circleci.clj. Please do not edit here.\n"
           (yaml/generate-string config)))
