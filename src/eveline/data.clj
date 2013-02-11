(ns eveline.data
  (:require [clojure.java.jdbc :as jdbc]
            [eveline.migrations :as m]
            (clj-time [core :as dt]
                      [coerce :as coerce]))
  (:import java.util.Date
           java.sql.Timestamp))

(defn posts [db-spec]
  (m/fetch-results db-spec
                   ["SELECT * FROM posts WHERE published IS NOT NULL ORDER BY published desc"]))

(defn post [db-spec id]
  (first (m/fetch-results db-spec
                          ["SELECT * FROM posts WHERE id=?" id])))

(defn publish-post [db-spec title format content]
  (m/insert-record db-spec :posts {:title title
                                   :type format
                                   :content content
                                   :published (Timestamp. (.getTime (Date.)))}))

(defn update-post [db-spec id title format content]
  (jdbc/with-connection db-spec
    (jdbc/update-values :posts ["id=?" id]
                        {:title title
                         :type format
                         :content content
                         :modified (Timestamp. (.getTime (Date.)))})))

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