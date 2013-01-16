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

(defn- build-archive [posts]
  (letfn [(month-index [post]
            (let [d (DateTime. (:published post))]
              (vector (time/year d) (time/month d))))]
    (sort (group-by month-index posts))))

(h/defsnippet archive-link "archives.html" [:a] [month]
  [:a] (h/do->
        (h/set-attr :href (str (first month) "/" (second month)))
        (h/content (str (first month) "-" (second month)))))

(h/defsnippet archive-items "archives.html" [:nav#archives] [archive]
  [:li]  (h/clone-for [month (map first archive)]
                      (h/content (archive-link month))))

(h/deftemplate layout "layout.html" [title posts]
  [:section#posts] (h/content (for [p posts]
                                (post p)))
  [:section#sidebar] (h/content (archive-items (build-archive posts))))
