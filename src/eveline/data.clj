(ns eveline.data
  (:require [clojure.java.jdbc :as jdbc]
            [eveline.migrations :as m])
  (:import java.util.Date
           java.sql.Timestamp))

(defn posts [db-spec]
  (m/fetch-results db-spec
                   ["SELECT * FROM posts WHERE published IS NOT NULL ORDER BY published desc"]))

(defn post-months [db-spec]
  (m/fetch-results db-spec
                   [(str "SELECT cast(extract(year from published) as int) AS year,"
                         " cast(extract(month from published) as int) as month FROM posts"
                         " WHERE published IS NOT NULL "
                         " group by year,month order by year,month")]))

(defn month-posts [db-spec year month]
  (m/fetch-results db-spec
                   [(str "SELECT * FROM posts WHERE extract(year from published)=?"
                         " AND extract(month from published)=?") year month]))

(defn conf-param [db-spec param]
  (:value (first (m/fetch-results db-spec
                                  ["SELECT value FROM configuration WHERE parameter=?"
                                   param]))))