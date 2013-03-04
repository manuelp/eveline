(ns eveline.rss
  (:require [clj-rss.core :as rss]
            [eveline.data :as data]
            [eveline.views :as views]))

(defn- entry 
  "Generate a RSS entry from a post."
  [base-url post]
   {:title (:title post)
    :link (str base-url "/posts/" (:id post))
    :description (views/format-content post)
    :pubDate (:published post)})

(defn feed 
  "Generate a RSS feed from a seq of posts."
  [title description base-url posts]
  (let [channel-data {:title title :description description :link base-url} 
        gen-rss (partial rss/channel-xml false)
        rss-entry (partial entry base-url)]
    (apply (partial rss/channel-xml channel-data) (map rss-entry posts))))