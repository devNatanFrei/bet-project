(ns bet-project.core
  (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route]
   [clj-http.client :as client]
   [cheshire.core :as json]))

(defn fetch-tournaments
  []
  (let [response (client/get "https://betano.p.rapidapi.com/tournaments"
                             {:headers {"x-rapidapi-key" "7e74fa9991msh6adec433f460cb7p1f3802jsn94929cbbe680"
                                        "x-rapidapi-host" "betano.p.rapidapi.com"}
                              :query-params {"sport" "soccer"}})]
    (json/parse-string (:body response) true)))

(defn fetch-events
  []
  (let [response (client/get "https://betano.p.rapidapi.com/events"
                             {:headers {:x-rapidapi-key "7e74fa9991msh6adec433f460cb7p1f3802jsn94929cbbe680"
                                        :x-rapidapi-host "betano.p.rapidapi.com"}
                              :query-params {:tournamentId "38"}})]
    (json/parse-string (:body response) true)))

(defn parse-tournament
  [tourn]
  {:categoryName (:categoryName tourn)
   :name         (:name tourn)
   :sportName    (:sportName tourn)
   :tournamentId (:tournamentId tourn)})

(defn parse-event
  [event]
  {:eventId        (:eventId event)
   :date           (:date event)
   :time           (:time event)
   :participant1   (:participant1 event)
   :participant2   (:participant2 event)
   :bookmakerCount (:bookmakerCount event)})

(defn pegar-torneios
  [request]
  (let [tournaments (fetch-tournaments)]
    {:status 200
     :body (mapv parse-tournament (vals tournaments))}))

(defn pegar-eventos
  [request]
  (let [events (fetch-events)]
    {:status 200
     :body (mapv parse-event (vals (:events events)))}))

(def routes (route/expand-routes
             #{["/jogos" :get pegar-torneios :route-name :todos-os-jogos]
               ["/eventos" :get pegar-eventos :route-name :todos-os-eventos]}))

(def service-map {::http/routes routes
                  ::http/port   9999
                  ::http/type   :jetty
                  ::http/join?  false})

(defn -main [& args]
  (http/start (http/create-server service-map)))
