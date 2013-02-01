(ns eveline.migrations
  (:require [clojure.java.jdbc :as jdbc])
  (:import java.util.Date
           java.sql.Timestamp))

(defmacro ensure-table [update-fn create-table-fn]
  `(try ~update-fn
        (catch org.postgresql.util.PSQLException e#
          (do ~create-table-fn
              ~update-fn))))

(defn drop-table [db-spec table]
  (jdbc/with-connection db-spec
    (jdbc/drop-table table)))

(defn insert-record [db-spec table record]
  (jdbc/with-connection db-spec
    (jdbc/insert-record table record)))

(defn fetch-results [db-spec query]
  (jdbc/with-connection db-spec
    (jdbc/with-query-results res query
      (doall res))))

;; ## Actual eveline-specific code ##

(defn add-posts-table [db-spec]
  (jdbc/with-connection db-spec
    (jdbc/create-table :posts
                       [:id "serial primary key"]
                       [:title "varchar(256)"]
                       [:content "text"]
                       [:type "varchar(128)"]
                       [:status "boolean"]
                       [:created "timestamp default now()"]
                       [:modified "timestamp"]
                       [:published "timestamp"])))

(defn add-conf-table [db-spec]
  (jdbc/with-connection db-spec
    (jdbc/create-table :configuration
                       [:parameter "varchar(256) primary key"]
                       [:value "varchar(256)"]
                       [:created "timestamp default now()"]
                       [:modified "timestamp default now()"])))

(def migrations {1 add-posts-table
                 2 add-conf-table})

(defn create-migrations-table [db-spec]
  (jdbc/with-connection db-spec
    (jdbc/create-table :migrations
                       [:id "integer primary key"]
                       [:applied "timestamp default now()"])))

(defn- migration-applied? [id db-spec]
  (fetch-results db-spec ["select * from migrations where id=?" id]))

(defn- apply-migration [db-spec migration]
  (let [id (first migration)]
    (or (ensure-table (migration-applied? id db-spec)
                      (create-migrations-table db-spec))
        (do (insert-record db-spec :migrations {:id id})
            ((second migration) db-spec)))))

(defn migrate [db-spec migrations]
  (doall (map (partial apply-migration db-spec) migrations)))