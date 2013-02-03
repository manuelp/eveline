(ns eveline.web
  (:require [ring.adapter.jetty :as adapter]
            [ring.util.response :as rresponse]
            (compojure
             [core :as ccore]
             [route :as croute]
             [handler :as handler])
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])
            (eveline [views :as views]
                     [data :as data]
                     [migrations :as ddl])
            [clj-time.core :as time]))

(def db-spec (or (System/getenv "DATABASE_URL")
                 "postgres://eveline:eveline@localhost/eveline"))

(ccore/defroutes routes*
  (croute/resources "/")
  (ccore/GET "/login" []
             (views/login-page (data/conf-param db-spec "blog-title")
                               (data/conf-param db-spec "tag-line")))
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
  (ccore/GET "/publish" []
             (friend/authorize #{::admin}
                               (views/publish (data/conf-param db-spec "blog-title")
                                              (data/conf-param db-spec "tag-line"))))
  (ccore/POST "/publish" [title format content]
              (friend/authorize #{::admin}
                                (do
                                  (data/publish-post db-spec title format content)
                                  (rresponse/redirect-after-post "/"))))
  (ccore/GET "/posts/:id" [id]
             (views/layout (data/conf-param db-spec "blog-title")
                           (data/conf-param db-spec "tag-line")
                           [(data/post db-spec (Integer/parseInt id))]
                           (data/post-months db-spec)))
  (croute/not-found "There is nothing like that here, sorry."))

(def users {"manuel" {:username "manuel"
                      :password (or (System/getenv "EVELINE_PASSWORD")
                                    "iron-man")
                      :roles #{::user ::admin}}})

(defn- plain-credentials-fn [known supplied]
  (if-let [relevant (known (:username supplied))]
    (if (= (:password supplied)
           (:password relevant))
      (dissoc relevant :password))))

(def routes
  (handler/site (friend/authenticate routes*
                                     {:credential-fn (partial plain-credentials-fn users)
                                      :workflows [(workflows/interactive-form)]})))

(defn start
  ([] (start 8080))
  ([port] (do (ddl/migrate db-spec ddl/migrations)
              (adapter/run-jetty #'routes {:port port
                                           :join? false}))))