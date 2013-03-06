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

(defn post-tags [db-spec id]
  (if-let [tags (m/fetch-results db-spec
                                 ["select t.id,t.label,l.post from tags t left join post_tags l on l.tag=t.id where l.post=?" id])]
    (vec (map :label tags))
    []))

(defn posts-id-by-category [db-spec category]
  (map :post (m/fetch-results db-spec
             	      ["select l.post from post_tags l left join tags t on l.tag=t.id where t.label=?" category])))

(defn posts-by-category [db-spec category]
  (map #(post db-spec %) (posts-id-by-category db-spec category)))

(defn publish-post [db-spec title format content]
  (m/insert-record db-spec :posts {:title title
                                   :type format
                                   :content content
                                   :published (Timestamp. (.getTime (Date.)))}))

(defn categories [db-spec]
  (map :label (m/fetch-results db-spec
              	     ["select label from tags"])))

(defn category-id [db-spec label]
  ((comp :id first) (m/fetch-results db-spec
                   		["select id from tags where label=?" label])))

(defn create-category [db-spec category]
  (jdbc/with-connection db-spec
    (jdbc/insert-record :tags {:label category})))

; TODO performance: (categories db-spec)
(defn update-categories [db-spec coll]
  (letfn [(known? [category]
                  (some (partial = category) (categories db-spec)))
          (update-category [category] (if-not (known? category)
    																	  (create-category db-spec category)))]
    (doall (map update-category coll))))

(defn make-links [db-spec post-id categories]
  (map #(hash-map :post post-id :tag (category-id db-spec %)) categories))

(defn define-categories [db-spec post-id categories]
  (m/run-transaction db-spec 
    (jdbc/delete-rows :post_tags ["post=?" post-id])
    (apply (partial jdbc/insert-records :post_tags) (make-links db-spec post-id categories))))

(defn update-post [db-spec id title format content categories]
  (do
  	(jdbc/with-connection db-spec
    	(jdbc/update-values :posts ["id=?" id]
       {:title title
        :type format
        :content content
        :modified (Timestamp. (.getTime (Date.)))}))
    (update-categories db-spec categories)
    (define-categories db-spec id categories)))

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