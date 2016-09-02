(ns csv-zen.core
  (:require [ring.adapter.jetty :as jetty]))

(defn app [req]
  {:headers {}
   :status 200
   :body "Hello, World!"})

(defn -main []
  (init-db)
  (jetty/run-jetty app
    {:port 8080}))
