(ns csv-zen.csv.model
  (:require [hugsql.core :as hugsql]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(def db (System/getenv "DATABASE_URL"))
(def db "jdbc:postgresql://localhost/csvzen")

(hugsql/def-db-fns "csv_zen/csv/database.sql")

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

(defn create-endpoint []
  (:id (create-endpoint* db)))

(defn create-upload [endpoint-id]
  (:id (create-upload* db {:endpoint-id endpoint-id})))

(defn create-row [upload-id]
  (:id (create-row* db {:upload-id upload-id})))

(defn create-cell [row-id key value]
  (:id (create-cell* db {:row-id row-id
                         :key key
                         :value value})))

(defn read-and-insert [upload-id rdr]
  (let [[header & rows] (csv/read-csv rdr)]
    (doseq [row rows]
      (let [row-id (create-row upload-id)]
        (doseq [[key value] (map vector header row)]
          (create-cell row-id key value))))))

(defn do-upload [endpoint-id rdr]
  (let [upload-id (create-upload endpoint-id)]
    (read-and-insert upload-id rdr)
    upload-id))

(defn do-insert-from-file [endpoint-id filename]
  (with-open [rdr (io/reader (io/file filename))]
    (do-upload endpoint-id rdr)))

(defn keys-for-upload [upload-id]
  (->> (keys-for-upload* db {:upload-id upload-id})
    (map :key)
    set))

(defn rows-for-upload [upload-id]
  (let [rows (rows-for-upload* db {:upload-id upload-id})
        groups (group-by :row_id rows)]
    (for [[row-id cells] groups]
      (reduce (fn [row {:keys [key value]}]
                (assoc row key value))
        {:row-id row-id}
        cells))))
