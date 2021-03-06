(ns eveline.views
  (:require [net.cgrand.enlive-html :as h]
            (clj-time [core :as time]
                      [format :as tformat])
            [markdown.core :as md]
            [cemerick.friend :as friend]
            [eveline.data :as data])
  (:use clojure.pprint)
  (:import org.joda.time.DateTime))

;; TODO
;; Views should not depend/use the *data* ns.
;; Its responsibility should only be to *render views with data passed explicitly*.

(defn format-date [date formatter]
  (tformat/unparse (tformat/formatters formatter) (DateTime. date)))

(defn format-content [post]
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

(h/defsnippet category-link "post.html" [:header :.tags :li :a] [category]
              [:a] (h/do->
                    (h/set-attr :href (str "/category/" category))
                    (h/content category)))

; TODO Remove duplication w/ eveline.web on this one
(def db-spec (or (System/getenv "DATABASE_URL")
                 "postgres://eveline:eveline@localhost/eveline"))

(h/defsnippet comments "comments.html" [:div] [])

(h/defsnippet post "post.html" [:article] [request post single?]
  [:h1.title] (h/content (:title post))
  [:header :p.post-info :.post-link] (h/set-attr :href (str "/posts/" (:id post)))
  [:header :p.post-info :.edit-link] (if (authorized? request #{:admin})
                          (h/set-attr :href (str"/post/edit/" (:id post))))
  [:header :p.post-info :time] (date-element post :published)
  [:header :.update] (when (modified? post)
                       (h/content (updated post)))
  [:header :.tags :li] (h/clone-for [tag (data/post-tags db-spec (:id post))]
                                    (h/content (category-link tag)))
  [:section.content] (h/content (h/html-snippet (format-content post)))
  [:section.comments] (if (= single? true)
                        (h/content (comments))))

(h/defsnippet category-link "categories.html" [:a] [category]
              [:a] (h/do->
                    (h/set-attr :href (str "/category/" category))
                    (h/content category)))

(h/defsnippet categories "categories.html" [:nav#categories] [categories]
             [:li] (h/clone-for [category categories]
                                (h/content (category-link category))))

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

(h/defsnippet rss-feed "rss-feed.html" [:section.rss-feed] [feed-url feed-name]
              [:a] (h/set-attr :href feed-url)
              [:p] (h/content "Feed: " feed-name))

(h/deftemplate layout "layout.html" [request title tag-line posts post-months feed-data]
  [:head :title] (h/content title)
  [:#title] (h/content title)
  [:#tagline] (h/content tag-line)
  [:#page_header :nav :ul] (h/content (nav-bar request nav-links))
  [:section#posts] (h/content (if (= (count posts) 1)
                                (post request (first posts) true)
                                (for [p posts]
                                  (post request p false))))
  [:section#sidebar] (h/content (rss-feed (:url feed-data) (:caption feed-data))
                      					(archive-items post-months)
                                (categories (data/categories db-spec)))
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

(defn post-with-category? [post category db-spec]
  (some (partial = category) (data/post-tags db-spec (:id post))))

(h/defsnippet tag-checkbox "publish-form.html" [:form :section#categories :ul :li :input] [category & post]
              [:input] (h/do->
                        (h/set-attr :name category)
                        (h/set-attr :value category)
                        (h/content category)
                        (if (and (not (empty? post))
                                 (post-with-category? (first post) category db-spec))
                          (h/set-attr :checked "checked")
                          (h/content category))))

(h/defsnippet publish-form "publish-form.html" [:form] []
	[:section#categories :ul :li] (h/clone-for [category (data/categories db-spec)]
                          	  		(h/content (tag-checkbox category))))

(h/defsnippet compiled-publish-form "publish-form.html" [:form] [post]
  [:form] (h/set-attr :action (str "/post/edit/" (:id post)))
  [[:input (h/attr= :name "title")]] (h/set-attr :value (:title post))
  [[:input (h/attr= :name "format")]] (h/set-attr :value (:type post))
  [[:textarea (h/attr= :name "content")]] (h/content (:content post))
  [:section#categories :ul :li] (h/clone-for [category (data/categories db-spec)]
                          	  		(h/content (tag-checkbox category post))))

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