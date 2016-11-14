(ns csv-zen.event-source
  (:require [hugsql.core :as hugsql]
            [clojure.edn :as edn]))

(def db (System/getenv "DATABASE_URL"))
(def db "jdbc:postgresql://localhost/csvzen")

(hugsql/def-db-fns "csv_zen/event_source/database.sql")

(defn init-db []
  (install-uuid-module db)
  (create-entity-table db)
  (create-event-table  db))

(defn to-string [o]
  {:pre [(or (keyword? o)
           (string? o)
           (symbol? o))]}
  (let [s (str o)]
    (if (= \: (first s))
      (.substring s 1)
      s)))

(defonce entity-types (atom {}))

(defn register-entity-type [entity-type f init]
  (swap! entity-types assoc entity-type {:f f
                                         :init init})
  entity-type)

(defn event [entity-id event-name event-data version]
  {:entity-id entity-id
   :event-name event-name
   :event-data event-data
   :version version})

(defn create-entity [entity-type]
  (:id (create-entity* db {:entity-type (to-string entity-type)})))

(defn dispatch! [event]
  (let [ret (dispatch-event* db (-> event
                                  (update :event-name to-string)
                                  (update :event-data pr-str)))]
    (if ret
      (:id ret)
      (throw (ex-info "Wrong version #" {:event event})))))

(defn current-state [f init events]
  (reduce f init events))

(defn parse-event [{:keys [entity_id id created_at event_name event_data]}]
  {:event-id id
   :entity-id entity_id
   :created-at created_at
   :event-name (keyword event_name)
   :event-data (edn/read-string event_data)})

(defn fetch-events [entity-id]
  (map parse-event (fetch-events* db {:entity-id entity-id})))

(defn entity-type [entity-id]
  (-> (fetch-entity-type* db {:entity-id entity-id})
    first
    :entity_type
    keyword))

(defn lookup-entity-type-info [entity-type]
  (get @entity-types entity-type))

(defn get-current-state [entity-id]
  (let [entity-type (entity-type entity-id)
        {:keys [f init]} (lookup-entity-type-info entity-type)]
    (current-state f init (fetch-events entity-id))))
