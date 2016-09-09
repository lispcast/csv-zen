(defproject csv-zen "0.1.0-SNAPSHOT"
  :description "Part of the Complete Web App from Scratch course on PurelyFunctional.tv"
  :url "https://purelyfunctional.tv/courses/complete-web-app-from-scratch/"
  :license {:name "CC0 1.0 Universal (CC0 1.0) Public Domain Dedication"
            :url "http://creativecommons.org/publicdomain/zero/1.0/"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring "1.5.0"]
                 [org.clojure/data.csv "0.1.3"]
                 [com.layerware/hugsql "0.4.7"]
                 [org.postgresql/postgresql "9.4.1207"]

                 [org.clojure/core.match "0.3.0-alpha4"]]

  :main csv-zen.core

  :profiles {:dev {:source-paths ["dev-src"]
                   :main csv-zen.dev
                   :dependencies [[prone "1.1.1"]]}})
