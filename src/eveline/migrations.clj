(ns eveline.migrations
  (:require [clojure.java.jdbc :as jdbc])
  (:import java.util.Date
           java.sql.Timestamp))

;; ## Utils ##
(defn fetch-results [db-spec query]
  (jdbc/with-connection db-spec
    (jdbc/with-query-results res query
      (doall res))))

(defn insert-record [db-spec table record]
  (jdbc/with-connection db-spec
    (jdbc/insert-record table record)))

(defmacro run-transaction [db-spec & forms]
  `(jdbc/with-connection ~db-spec
     (jdbc/transaction ~@forms)))

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
                       [:title "varchar(255)"]
                       [:content "text"]
                       [:status "boolean"]
                       [:created "timestamp default now()"]
                       [:modified "timestamp"]
                       [:published "timestamp"])))

(defn drop-posts-table [db-spec]
  (jdbc/with-connection db-spec
    (jdbc/drop-table :posts)))
