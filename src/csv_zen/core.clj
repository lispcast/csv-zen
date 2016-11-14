(ns csv-zen.core
  (:require [yada.yada :as yada]

            [csv-zen.csv.model :as csv]
            [csv-zen.event-source :as es]
            [csv-zen.routes :as routes]))

(defonce server (atom nil))

(defn -main []
  (csv/init-db)
  (es/init-db)
  (let [listener (yada/listener routes/routes
                   {:port 8080})]
    (reset! server listener)))

(defn reset []
  (when (fn? (:close @server))
    ((:close @server)))
  (-main))

(reset)
