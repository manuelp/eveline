(ns eveline.core
  (:require (clj-time [core :as t]))
  (:import org.joda.time.DateTime))

(defn build-archive
  "Group posts by a vector like this: [year month].
   The resulting map is ordered by key."
  [posts]
  (letfn [(month-index [post]
            (let [d (DateTime. (:published post))]
              (vector (t/year d) (t/month d))))]
    (sort (group-by month-index posts))))