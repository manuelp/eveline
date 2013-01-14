(ns eveline.migrations
  (:require [clojure.java.jdbc :as jdbc])
  (:import java.util.Date
           java.sql.Timestamp))

(defmacro ensure-table [update-fn create-table-fn]
  `(try ~update-fn
        (catch org.postgresql.util.PSQLException e#
          (do ~create-table-fn
              ~update-fn))))

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

(defn drop-posts-table [db-spec]
  (jdbc/with-connection db-spec
    (jdbc/drop-table :posts)))
