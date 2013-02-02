(ns eveline.web
  (:require [ring.adapter.jetty :as adapter]
            (compojure
             [core :as ccore]
             [route :as croute]
             [handler :as handler])
            (eveline [views :as views]
                     [data :as data]
                     [migrations :as ddl])
            [clj-time.core :as time]))

;; Should be read from env variable DATABASE_URL
(def db-spec "postgres://eveline:eveline@localhost/eveline")

(ccore/defroutes routes*
  (croute/resources "/")
  (ccore/GET "/" []
             (views/layout (data/conf-param db-spec "blog-title")
                           (data/conf-param db-spec "tag-line")
                           (data/posts db-spec)
                           (data/post-months db-spec)))
  (ccore/GET "/archive/:year/:month" [year month]
             (let [title (str (data/conf-param db-spec "blog-title")
                              ": " year "-" month " archive")]
               (views/layout title
                             (data/conf-param db-spec "tag-line")
                             (apply data/month-posts
                                    (cons db-spec (map read-string [year month])))
                             (data/post-months db-spec))))
  (ccore/GET "/posts/:id" [id]
             (views/layout (data/conf-param db-spec "blog-title")
                           (data/conf-param db-spec "tag-line")
                           [(data/post db-spec (Integer/parseInt id))]
                           (data/post-months db-spec)))
  (croute/not-found "There is nothing like that here, sorry."))

(def routes
  (handler/site routes*))

(defn start
  ([] (start 8080))
  ([port] (do (ddl/migrate db-spec ddl/migrations)
              (adapter/run-jetty #'routes {:port port
                                           :join? false}))))