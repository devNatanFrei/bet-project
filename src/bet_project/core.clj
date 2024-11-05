(ns bet-project.core
  (:require [clj-http.client :as client]
            [cheshire.core :as json])
  (:gen-class))

(def tourn (client/get "https://betano.p.rapidapi.com/tournaments"
                      {:headers {"x-rapidapi-key" "7e74fa9991msh6adec433f460cb7p1f3802jsn94929cbbe680"
                                 "x-rapidapi-host" "betano.p.rapidapi.com"}
                       :query-params {"sport" "soccer"}}))

(def oddsType (client/get "https://betano.p.rapidapi.com/oddstypes" {:headers {:x-rapidapi-key "7e74fa9991msh6adec433f460cb7p1f3802jsn94929cbbe680"
                                                                 :x-rapidapi-host "betano.p.rapidapi.com"}
                                                       :query-params {:sport "soccer"}}))              

(def )

(def body-tourn (:body tourn))
(def body-parsed-tourn (json/parse-string body-tourn true))

(defn -main
  [& args]

  (mapv (fn [tournament]
          (println "categoryName:" (:categoryName tournament))
          (println "name:" (:name tournament))
          (println "sportName:" (:sportName tournament))
          (println "tournamentId:" (:tournamentId tournament))
          (println "------------------"))
        (vals body-parsed-tourn))
        
)
