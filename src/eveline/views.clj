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
  [:header :.post-link] (h/set-attr :href (str "/posts/" (:id post)))
  [:header :p :time] (h/do->
                      (h/set-attr :datetime (format-date (:published post)
                                                         :date-time))
                      (h/content (format-date (:published post)
                                              :rfc822)))
  [:section] (h/content (h/html-snippet (format-content post))))

(h/defsnippet archive-link "archives.html" [:a] [post-month]
  [:a] (let [month (:month post-month)
             year (:year post-month)]
         (h/do->
          (h/set-attr :href (str "/archive/" year "/" month))
          (h/content (str year "-" month)))))

(h/defsnippet archive-items "archives.html" [:nav#archives] [post-months]
  [:li]  (h/clone-for [post-month post-months]
                      (h/content (archive-link post-month))))

(h/deftemplate layout "layout.html" [title tag-line posts post-months]
  [:head :title] (h/content title)
  [:#title] (h/content title)
  [:#tagline] (h/content tag-line)
  [:section#posts] (h/content (for [p posts]
                                (post p)))
  [:section#sidebar] (h/content (archive-items post-months))
  [:footer :#current-year] (let [year (time/year (time/now))] 
                             (if (> year 2013)
                               (h/append (str year)))))
