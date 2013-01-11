(ns eveline.web
  (:require [ring.adapter.jetty :as adapter]
            (compojure
             [core :as ccore]
             [handler :as handler])))

(ccore/defroutes routes*
  (ccore/GET "/" []
       "Welcome! I'm Eveline, a fine Clojure-powered blog engine :)"))

(def routes
  (handler/site routes*))

(defn start
  ([] (start 8080))
  ([port] (adapter/run-jetty #'routes {:port port
                                       :join? false})))