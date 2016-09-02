(ns csv-zen.core
  (:require [ring.adapter.jetty :as jetty]
            [hugsql.core :as hugsql]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(def db (System/getenv "DATABASE_URL"))
(def db "jdbc:postgresql://localhost/csvzen")

(hugsql/def-db-fns "csv_zen/database.sql")

(defn init-db []
  (install-uuid-module db)
  (create-endpoint-table db)
  (create-upload-table db)
  (create-row-table db)
  (create-cell-table db))

(defn reset-db! []
  (init-db)
  (delete-all-cells db)
  (delete-all-rows db)
  (delete-all-uploads db)
  (delete-all-endpoints db))

(defn create-endpoint [db]
  (:id (create-endpoint* db)))

(defn create-upload [db endpoint-id]
  (:id (create-upload* db {:endpoint-id endpoint-id})))

(defn create-row [db upload-id]
  (:id (create-row* db {:upload-id upload-id})))

(defn create-cell [db row-id key value]
  (:id (create-cell* db {:row-id row-id
                         :key key
                         :value value})))

(defn read-and-insert [db upload-id rdr]
  (let [[header & rows] (csv/read-csv rdr)]
    (doseq [row rows]
      (let [row-id (create-row db upload-id)]
        (doseq [[key value] (map vector header row)]
          (create-cell db row-id key value))))))

(defn do-upload [db endpoint-id rdr]
  (let [upload-id (create-upload db endpoint-id)]
    (read-and-insert db upload-id rdr)
    upload-id))

(defn do-insert-from-file [db endpoint-id filename]
  (with-open [rdr (io/reader (io/file filename))]
    (do-upload db endpoint-id rdr)))

(defn keys-for-upload [db upload-id]
  (->> (keys-for-upload* db {:upload-id upload-id})
    (map :key)
    set))

(defn rows-for-upload [db upload-id]
  (let [rows (rows-for-upload* db {:upload-id upload-id})
        groups (group-by :row_id rows)]
    (for [[row-id cells] groups]
      (reduce (fn [row {:keys [key value]}]
                (assoc row key value))
        {:row-id row-id}
        cells))))

(defn app [req]
  {:headers {}
   :status 200
   :body "Hello, World!"})

(defn -main []
  (init-db)
  (jetty/run-jetty app
    {:port 8080}))
