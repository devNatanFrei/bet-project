(ns bet-project.core
  (:require
    [io.pedestal.http :as http]
    [io.pedestal.http.route :as route]
    [clj-http.client :as client]
    [cheshire.core :as json]))

(defn fetch-soccer-tournaments
  []
  (let [response (client/get "https://betano.p.rapidapi.com/tournaments" {:headers {:x-rapidapi-key "507110868dmsh80767a1dbb87630p1ce51fjsn9eaed59d8bcf"
                                                                                    :x-rapidapi-host "betano.p.rapidapi.com"}
                                                                          :query-params {:sport "soccer"}})]
    (json/parse-string (:body response) true)))


(defn fetch-events
  []
  (let [response(client/get "https://betano.p.rapidapi.com/events" {:headers {:x-rapidapi-key "507110868dmsh80767a1dbb87630p1ce51fjsn9eaed59d8bcf"
                                                                              :x-rapidapi-host "betano.p.rapidapi.com"}
                                                                    :query-params {:tournamentId "38"}})]
    (json/parse-string (:body response) true)))

(defn fetch-oddtypes
  []
  (let [response (client/get "https://betano.p.rapidapi.com/oddstypes" {:headers {:x-rapidapi-key "507110868dmsh80767a1dbb87630p1ce51fjsn9eaed59d8bcf"
                                                                                  :x-rapidapi-host "betano.p.rapidapi.com"}
                                                                        :query-params {:sport "soccer"}})]
    (json/parse-string (:body response) true)))

(defn parse-tournament
  [tourn]
  {:categoryName (:categoryName tourn)
   :name         (:name tourn)
   :sportName    (:sportName tourn)
   :tournamentId (:tournamentId tourn)})

(defn parse-event
  [event]
  {:bookmakerCount (:bookmakerCount event)
   :date           (:date event)
   :eventId        (:eventId event)
   :eventStatus    (:eventStatus event)
   :participant1   (:participant1 event)
   :participant1Id (:participant1Id event)
   :participant2   (:participant2 event)
   :participant2Id (:participant2Id event)
   :startTime      (:startTime event)
   :time           (:time event)})

(defn parse-oddtype
  [odd]
  {:handicap        (:handicap odd)
   :marketId        (:marketId odd)
   :marketName      (:marketName odd)
   :marketNameShort (:marketNameShort odd)
   :oddsType        (:oddsType odd)
   :outcomeId       (:outcomeId odd)
   :outcomeName     (:outcomeName odd)
   :outcomes        (:outcomes odd)
   :sport           (:sport odd)})

(defn pegar-torneios-futebol
  [request]
  (let [tournaments (fetch-soccer-tournaments)]
    {:status 200
     :body (mapv parse-tournament (vals tournaments))}))

(defn pegar-eventos
  [request]
  (let [events (fetch-events)]
    {:status 200
     :body (mapv parse-event (vals (:events events)))}))

(defn pegar-oddtypes
  [request]
  (let [oddtypes (fetch-oddtypes)]
    {:status 200
     :body (mapv parse-oddtype (vals oddtypes))}))

(def routes (route/expand-routes
              #{["/futebol" :get pegar-torneios-futebol :route-name :jogos-futebol] 
                ["/eventos" :get pegar-eventos :route-name :todos-os-eventos]
                ["/oddtypes" :get pegar-oddtypes :route-name :todos-os-oddtypes]}))

(def service-map {::http/routes routes
                  ::http/port   9999
                  ::http/type   :jetty
                  ::http/join?  false})

(defn -main
  [& args] 
  (http/start (http/create-server service-map))
  (println "Rodou pourra")
  )


