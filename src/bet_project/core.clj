(ns bet-project.core
  (:require
    [io.pedestal.http :as http]
    [io.pedestal.http.route :as route]
    [clj-http.client :as client]
    [cheshire.core :as json]
    )
  )

(def tourn (client/get "https://betano.p.rapidapi.com/tournaments"
                      {:headers {"x-rapidapi-key" "7e74fa9991msh6adec433f460cb7p1f3802jsn94929cbbe680"
                                 "x-rapidapi-host" "betano.p.rapidapi.com"}
                       :query-params {"sport" "soccer"}}))

(def oddsType (client/get "https://betano.p.rapidapi.com/oddstypes" {:headers {:x-rapidapi-key "7e74fa9991msh6adec433f460cb7p1f3802jsn94929cbbe680"
                                                                 :x-rapidapi-host "betano.p.rapidapi.com"}
                                                       :query-params {:sport "soccer"}}))              



(def body-tourn (:body tourn))
(def body-parsed-tourn (json/parse-string body-tourn true))

  (defn pegar-torneios [request]
        {:status 200
         :body (mapv (fn [request]
                       {:categoryName (:categoryName request)
                        :name         (:name request)
                        :sportName    (:sportName request)
                        :tournamentId (:tournamentId request)})
                     (vals body-parsed-tourn))})

  (def routes (route/expand-routes
                #{["/jogos" :get pegar-torneios :route-name :todos-os-jogos]}))
  (def service-map {
                    ::http/routes routes
                    ::http/port   9999
                    ::http/type   :jetty
                    ::http/join?  false
                    })

  (http/start (http/create-server service-map))
  (println "FUNCIONA PELO AMOR DE DEUS")
