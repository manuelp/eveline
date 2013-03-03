(ns eveline.web
  (:require [ring.adapter.jetty :as adapter]
            [ring.util.response :as rresponse]
            [ring.middleware.stacktrace :as rstacktrace]
            (compojure
             [core :as ccore]
             [route :as croute]
             [handler :as handler])
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            (eveline [views :as views]
                     [data :as data]
                     [migrations :as ddl]
                     [rss :as rss])
            [clj-time.core :as time])
  (:use clojure.pprint))

(def db-spec (or (System/getenv "DATABASE_URL")
                 "postgres://eveline:eveline@localhost/eveline"))

(ccore/defroutes routes*
  (croute/resources "/")
  (ccore/GET "/login" []
             (views/login-page (data/conf-param db-spec "blog-title")
                               (data/conf-param db-spec "tag-line")))
  (ccore/GET "/" request
             (views/layout request
                           (data/conf-param db-spec "blog-title")
                           (data/conf-param db-spec "tag-line")
                           (data/posts db-spec)
                           (data/post-months db-spec)))
  (ccore/GET "/about" request
             (views/about request
                          (data/conf-param db-spec "blog-title")
                          (data/conf-param db-spec "tag-line")))
  (ccore/GET "/archive/:year/:month" [year month :as request]
             (let [title (str (data/conf-param db-spec "blog-title")
                              ": " year "-" month " archive")]
               (views/layout request 
                             title
                             (data/conf-param db-spec "tag-line")
                             (apply data/month-posts
                                    (cons db-spec (map read-string [year month])))
                             (data/post-months db-spec))))
  (ccore/GET "/category/:category" [category :as request]
             (let [title (str (data/conf-param db-spec "blog-title")
                              "- Category: " category)]
               (views/layout request title (data/conf-param db-spec "tag-line")
                             (data/posts-by-category db-spec category)
                             (data/post-months db-spec))))
  (ccore/GET "/publish" []
             (friend/authorize #{:admin}
                               (views/publish (data/conf-param db-spec "blog-title")
                                              (data/conf-param db-spec "tag-line"))))
  (ccore/POST "/publish" [title format content]
              (friend/authorize #{:admin}
                                (do
                                  (data/publish-post db-spec title format content)
                                  (rresponse/redirect-after-post "/"))))
  (ccore/GET "/posts/:id" [id :as request]
             (views/layout request
                           (data/conf-param db-spec "blog-title")
                           (data/conf-param db-spec "tag-line")
                           [(data/post db-spec (Integer/parseInt id))]
                           (data/post-months db-spec)))
  (ccore/GET "/post/edit/:id" [id]
             (friend/authorize #{:admin}
                               (views/publish (data/conf-param db-spec "blog-title")
                                              (data/conf-param db-spec "tag-line")
                                              (data/post db-spec (Integer/parseInt id)))))
  (ccore/POST "/post/edit/:id" [id title format content]
              (friend/authorize #{:admin}
                                (do
                                  (data/update-post db-spec (Integer/parseInt id)
                                                    title format content)
                                  (rresponse/redirect (str "/posts/" id)))))
                 
  (ccore/GET "/feed" []
             (rss/feed (data/conf-param db-spec "blog-title")
                       (data/conf-param db-spec "tag-line")
                       (data/conf-param db-spec "domain-name")
                       (data/posts db-spec)))
  (friend/logout (ccore/ANY "/logout" request (rresponse/redirect "/")))
  (croute/not-found "There is nothing like that here, sorry."))

(def users {"manuel" {:username "manuel"
                      :password (or (System/getenv "EVELINE_PASSWORD")
                                    "iron-man")
                      :roles #{:user :admin}}})

(defn- plain-credentials-fn [known supplied]
  (if-let [relevant (known (:username supplied))]
    (if (= (:password supplied)
           (:password relevant))
      (dissoc relevant :password))))

;; Warning: order is important! `friend/authenticate` should be the
;; first middleware, so that it can redirect to /login as specified in
;; the workflow.
(def routes
  (-> #'routes*
      (friend/authenticate {:credential-fn (partial plain-credentials-fn users)
                            :workflows [(workflows/interactive-form)]})
      (rstacktrace/wrap-stacktrace)
      (handler/site)))

(defn start
  ([] (start 8080))
  ([port] (do (ddl/migrate db-spec ddl/migrations)
              (adapter/run-jetty #'routes {:port port
                                           :join? false}))))

(defn -main [& args]
  (if-let [custom-port (System/getenv "PORT")]
    (start (Integer/parseInt custom-port))
    (start)))

;; For LT instarepl:
;(require '[eveline.web :as web])
;(require '[ring.adapter.jetty :as adapter])
;(defonce server (adapter/run-jetty #'web/routes {:port 8080 :join? false}))