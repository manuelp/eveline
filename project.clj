(defproject eveline "0.1.0-SNAPSHOT"
  :description "A simple and personal blog engine."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-marginalia "0.7.1"]
            [lein-ring "0.8.2"]]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [ring "1.1.6"]
                 [compojure "1.1.3"]
                 [enlive "1.0.1"]
                 [clj-time "0.4.4"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [postgresql/postgresql "9.1-901-1.jdbc4"]
                 [markdown-clj "0.9.19"]
                 [com.cemerick/friend "0.1.3"]]
  :ring {:handler eveline.web/routes}
  :main eveline.web)
