(ns csv-zen.core
  (:require [ring.adapter.jetty :as jetty]
            [hugsql.core :as hugsql]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [ring.middleware.params :as params]
            [ring.middleware.multipart-params :as multipart]
            [ring.middleware.resource :as resource]
            [ring.middleware.content-type :as content-type]
            [ring.middleware.not-modified :as not-modified]
            [ring.handler.dump :as dump]
            [clojure.core.match :refer [match]]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [hiccup.core :refer [html h]]
            [hiccup.page :as page]
            [bidi.bidi :as bidi]
            [bidi.vhosts :refer [vhosts-model]]
            [yada.yada :as yada]
            [yada.resources.classpath-resource :refer [new-classpath-resource]]))

(def scheme "http")
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

(defn create-endpoint-request [ctx]
  (let [endpoint-id (create-endpoint db)
        response (:response ctx)]
    (-> response
      (assoc :status 201)
      (assoc :body {:endpoint {:id endpoint-id}})
      (assoc-in [:headers "Location"]
        (yada/url-for ctx :endpoint {:route-params {:id endpoint-id}})))))

(defn upload-request [ctx]
  (let [req (:request ctx)
        response (:response ctx)
        endpoint-id (get-in req [:route-params :id])
        endpoint-id (java.util.UUID/fromString endpoint-id)
        multipart (get-in ctx [:parameters :form :file])
        upload-id (do-upload db endpoint-id (java.io.StringReader. multipart))]
    (-> response
      (assoc :status 201)
      (assoc :body {:upload {:id upload-id
                             :endpoint {:id endpoint-id}}})
      (assoc-in [:headers "Location"]
        (yada/url-for ctx :endpoint-upload {:route-params {:id endpoint-id :upload-id upload-id}})))))

(defn dashboard-page []
  (page/html5
             {:lang :en}
             [:head
              [:meta {:charset "utf-8"}]
              [:meta {:http-equiv "X-UA-Compatible"
                      :content "IE=edge"}]
              [:meta {:name "viewport"
                      :content "width=device-width, initial-scale=1"}]
              ;; the above 3 meta tags must come first
              [:link {:rel "stylesheet"
                      :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
                      :integrity "sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u"
                      :crossorigin "anonymous"}]
              [:link {:rel "stylesheet"
                      :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css"
                      :integrity "sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp"
                      :crossorigin "anonymous"}]


              [:title "Dashboard"]]
             [:body
              [:div.container
               [:h1 "Dashboard"]

               [:form
                [:button.btn.btn-default "Click me!"]]
               ]
              [:script {:src "https://code.jquery.com/jquery-3.1.0.min.js"}]

              [:script {:src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"
                        :integrity "sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa"
                        :crossorigin "anonymous"}]

              ]))

(def routes
  (vhosts-model
    [[(str scheme "://" host)]
     ["" [["/dump" dump/handle-dump]
          ["/dashboard" (yada/resource {:id :dashboard
                                        :produces {:media-type "text/html"
                                                   :language "en"}
                                        :response (fn [ctx]
                                                    (dashboard-page))})]
          ["/endpoints" (yada/resource
                          {:id :endpoints
                           :produces {:media-type "application/json"}
                           :methods {:post
                                     {:response
                                      (fn [ctx]
                                        (create-endpoint-request ctx))}}})]
          [["/endpoint/" :id]
           {"" (yada/resource
                 {:id :endpoint
                  :produces {:media-type "application/json"}
                  :methods {:post
                            {:consumes "multipart/form-data"
                             :parameters {:form {:file String}}
                             :response upload-request}}})
            ["/upload/" :upload-id] (yada/resource
                                      {:id :endpoint-upload
                                       :response nil})}]
          ["" (new-classpath-resource "homepage"
                {:index-files ["index.html"]})]]]]))

(defonce server (atom nil))

(defn -main []
  (init-db)
  (let [listener (yada/listener routes
                   {:port 8080})]
    (reset! server listener)))

(defn reset []
  (when (fn? (:close @server))
    ((:close @server)))
  (-main))

(reset)
