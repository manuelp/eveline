(ns eveline.views
  (:require [net.cgrand.enlive-html :as h]
            (clj-time [core :as time]
                      [format :as tformat])
            [markdown.core :as md]
            [cemerick.friend :as friend])
  (:use clojure.pprint)
  (:import org.joda.time.DateTime))

(defn format-date [date formatter]
  (tformat/unparse (tformat/formatters formatter) (DateTime. date)))

(defn- format-content [post]
  (let [content (:content post)
        type (:type post)]
    (cond (= type "text/html") content
          (= type "text/markdown") (md/md-to-html-string content)
          :default content)))

(defn authorized?
  "A user is considered authorized if is logged and has the required roles assigned.

All information about identity and roles is in the request, and friend can extract these informations."
  [request roles]
  (not (= nil (friend/authorized? roles request))))

(defn- date-element [post date-key]
  (h/do->
   (h/set-attr :datetime (format-date (date-key post) :date-time))
   (h/content (format-date (date-key post) :rfc822))))

(defn- modified? [post]
  (not (nil? (:modified post))))

(h/defsnippet updated "post.html" [:header :.update] [post]
  [:.update :time] (date-element post :modified))

(h/defsnippet post "post.html" [:article] [request post]
  [:h1.title] (h/content (:title post))
  [:header :.post-link] (h/set-attr :href (str "/posts/" (:id post)))
  [:header :.edit-link] (if (authorized? request #{:admin})
                          (h/set-attr :href (str"/post/edit/" (:id post))))
  [:header :p :time] (date-element post :published)
  [:header :.update] (when (modified? post)
                       (h/content (updated post)))
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

(def nav-links [{:text "Home"
                 :href "/"}
                {:text "About"
                 :href "/about"}])

(h/defsnippet nav-link "layout.html" [:#page_header :nav :.nav-link] [link]
  [:a] (h/do->
        (h/set-attr :href (:href link))
        (h/content (:text link))))

(h/defsnippet nav-bar "layout.html" [:#page_header :nav :ul] [request base-links]
  [:li] (let [links (if (authorized? request #{:admin})
                      (conj base-links {:text "Logout"
                                        :href "/logout"})
                      base-links)]
          (h/clone-for [link links]
                       (h/content (nav-link link)))))

(h/deftemplate layout "layout.html" [request title tag-line posts post-months]
  [:head :title] (h/content title)
  [:#title] (h/content title)
  [:#tagline] (h/content tag-line)
  [:#page_header :nav :ul] (h/content (nav-bar request nav-links))
  [:section#posts] (h/content (for [p posts]
                                (post request p)))
  [:section#sidebar] (h/content (archive-items post-months))
  [:footer :#current-year] (let [year (time/year (time/now))] 
                             (if (> year 2013)
                               (h/append (str year)))))

(h/defsnippet login-form "login-form.html" [:form] [])

(h/deftemplate login-page "admin.html" [title tag-line]
    [:head :title] (h/content title)
    [:#title] (h/content title)
    [:#tagline] (h/content tag-line)
    [:#main] (h/content (login-form))
    [:footer :#current-year] (let [year (time/year (time/now))] 
                               (if (> year 2013)
                                 (h/append (str year)))))

(h/defsnippet publish-form "publish-form.html" [:form] [])

(h/defsnippet compiled-publish-form "publish-form.html" [:form] [post]
  [:form] (h/set-attr :action (str "/post/edit/" (:id post)))
  [[:input (h/attr= :name "title")]] (h/set-attr :value (:title post))
  [[:input (h/attr= :name "format")]] (h/set-attr :value (:type post))
  [[:textarea (h/attr= :name "content")]] (h/content (:content post)))

(h/deftemplate publish "admin.html" [title tag-line & post]
    [:head :title] (h/content title)
    [:#title] (h/content title)
    [:#tagline] (h/content tag-line)
    [:#main] (h/content (if (empty? post)
                          (publish-form)
                          (compiled-publish-form (first post))))
    [:footer :#current-year] (let [year (time/year (time/now))] 
                               (if (> year 2013)
                                 (h/append (str year)))))

(defn- about-page []
  (let [about-md (slurp "resources/about.md")]
    (md/md-to-html-string about-md)))

(h/deftemplate about "layout.html" [request title tag-line]
    [:head :title] (h/content title)
    [:#title] (h/content title)
    [:#tagline] (h/content tag-line)
    [:#page_header :nav :ul] (h/content (nav-bar request nav-links))
    [:#posts] (h/html-content (about-page))
    [:footer :#current-year] (let [year (time/year (time/now))] 
                               (if (> year 2013)
                                 (h/append (str year)))))