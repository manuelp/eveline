(ns eveline.views
  (:require [net.cgrand.enlive-html :as h]
            (clj-time [core :as time]
                      [format :as tformat])
            [markdown.core :as md])
  (:import org.joda.time.DateTime))

(defn format-date [date formatter]
  (tformat/unparse (tformat/formatters formatter) (DateTime. date)))

(defn- format-content [post]
  (let [content (:content post)
        type (:type post)]
    (cond (= type "text/html") content
          (= type "text/markdown") (md/md-to-html-string content)
          :default content)))

(h/defsnippet post "post.html" [:article] [post]
  [:h2.title] (h/content (:title post))
  [:header :p :time] (h/do->
                      (h/set-attr :datetime (format-date (:published post)
                                                         :date-time))
                      (h/content (format-date (:published post)
                                              :rfc822)))
  [:section] (h/content (h/html-snippet (format-content post))))

(h/deftemplate layout "layout.html" [title posts]
  [:section#posts] (h/content (for [p posts]
                                (post p))))
