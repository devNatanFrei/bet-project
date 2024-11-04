(ns bet-project.core
  (:require [clj-http.client :as client]
            [cheshire.core :as json])
  (:gen-class))

(def resp (client/get "https://betano.p.rapidapi.com/tournaments"
                      {:headers {"x-rapidapi-key" "7e74fa9991msh6adec433f460cb7p1f3802jsn94929cbbe680"
                                 "x-rapidapi-host" "betano.p.rapidapi.com"}
                       :query-params {"sport" "soccer"}}))

(def body (:body resp))
(def body-parsed (json/parse-string body true))

(defn -main
  [& args]

  (mapv (fn [tournament]
          (println "categoryName:" (:categoryName tournament))
          (println "name:" (:name tournament))
          (println "sportName:" (:sportName tournament))
          (println "tournamentId:" (:tournamentId tournament))
          (println "------------------"))
        (vals body-parsed)))
