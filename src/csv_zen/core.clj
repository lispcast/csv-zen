(ns csv-zen.core
  (:require [ring.adapter.jetty :as jetty]
            [hugsql.core :as hugsql]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [ring.middleware.params :as params]
            [ring.middleware.multipart-params :as multipart]
            [clojure.core.match :refer [match]]
            [clojure.string :as str]
            [clojure.pprint :as pprint]))

(def scheme "https")
(def host "localhost:8080")

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

(defn segments-from-path [uri]
  (if (= "/" uri)
    []
    (-> uri
      (.substring 1)
      (str/split #"/"))))


(defn create-endpoint-request []
  (let [endpoint-id (create-endpoint db)]
    {:status 201
     :body (str "{\"endpoint\": {\"id\": \""
             endpoint-id
             "\"}}")
     :headers {"Location" (str scheme "://" host "/endpoint/" endpoint-id)}}))

(defn upload-request [req endpoint-id]
  (let [endpoint-id (java.util.UUID/fromString endpoint-id)
        multipart (get-in req [:multipart-params "file" :tempfile])
        upload-id (do-insert-from-file db endpoint-id multipart)]
    {:status 201
     :body (str "{\"upload\": {\"id\": \""
             upload-id
             "\"}}")
     :headers {"Location" (str scheme "://" host "/endpoint/" endpoint-id "/upload/" upload-id)}}))

(defn routes [req]
  (match [(:request-method req)
          (segments-from-path (:uri req))]

    [:get []]
    {:headers {}
     :status 200
     :body "Hello, World!"}

    [:post ["endpoints"]]
    (create-endpoint-request)

    [:post ["endpoint" ?endpoint-id]]
    (upload-request req ?endpoint-id)

    [_ _] {:status 404
           :headers {}
           :body (with-out-str (pprint/pprint req))}))

(def app
  (-> routes
    params/wrap-params
    multipart/wrap-multipart-params))

(defn -main []
  (init-db)
  (jetty/run-jetty app
    {:port 8080}))
