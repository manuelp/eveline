(ns eveline.web
  (:require [ring.adapter.jetty :as adapter]
            (compojure
             [core :as ccore]
             [route :as croute]
             [handler :as handler])
            (eveline [views :as views]
                     [data :as data])
            [clj-time.core :as time]))

(def db-spec "postgres://eveline:eveline@localhost/eveline")

(ccore/defroutes routes*
  (croute/resources "/")
  (ccore/GET "/" []
             (views/layout "Lambda Land" (data/posts db-spec)))
  (ccore/GET "/archive/:year/:month" [year month]
             (views/layout (str "Lambda Land: " year "-" month " archive")
                           (data/posts db-spec)))
  (croute/not-found "There is nothing like that here, sorry."))

(def routes
  (handler/site routes*))

(defn start
  ([] (start 8080))
  ([port] (adapter/run-jetty #'routes {:port port
                                       :join? false})))