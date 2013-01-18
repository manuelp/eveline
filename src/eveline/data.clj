(ns eveline.data
  (:require [clojure.java.jdbc :as jdbc])
  (:import java.util.Date
           java.sql.Timestamp))

;; ## Utils ##
(defn- fetch-results [db-spec query]
  (jdbc/with-connection db-spec
    (jdbc/with-query-results res query
      (doall res))))

(defn- insert-record [db-spec table record]
  (jdbc/with-connection db-spec
    (jdbc/insert-record table record)))

(defmacro run-transaction [db-spec & forms]
  `(jdbc/with-connection ~db-spec
     (jdbc/transaction ~@forms)))

(defn posts [db-spec]
  (fetch-results db-spec
                 ["SELECT * FROM posts WHERE published IS NOT NULL ORDER BY published desc"]))

(defn month-posts [db-spec year month]
  (fetch-results db-spec
                 [(str "SELECT * FROM posts WHERE extract(year from published)=?"
                       " AND extract(month from published)=?") year month]))
