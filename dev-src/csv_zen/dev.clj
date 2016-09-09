(ns csv-zen.dev
  (:require [ring.adapter.jetty :as jetty]
            [csv-zen.core :as core]
            [ring.middleware.reload :as reload]
            [prone.middleware :as prone]))

(defn -main []
  (core/init-db)
  (jetty/run-jetty (reload/wrap-reload
                     (prone/wrap-exceptions
                       #'core/app))
    {:port 8080}))
