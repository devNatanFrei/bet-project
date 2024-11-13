(ns bet-project.core
  (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route]
   [cheshire.core :as json]
   [clj-http.client :as client]))

(def saldo-conta (atom 0))
(def apostas (atom []))

(defn depositar-handler
  [request]
  (let [quantidade (:quantidade (json/parse-string (slurp (:body request)) true))]
    (if (number? quantidade)
      (do
        (swap! saldo-conta + quantidade)
        {:status 200
         :body "Depósito realizado com sucesso!"})
      {:status 400
       :body "Valor inválido para depósito."})))

(defn obter-saldo-handler [request]
  {:status 200
   :body (json/generate-string {:saldo @saldo-conta})})

(defn registrar-aposta-handler [request]
  (let [aposta (json/parse-string (slurp (:body request)) true)
        valor-aposta (:quantidade aposta)]
    (if (and (number? valor-aposta) (<= valor-aposta @saldo-conta))
      (do
        (swap! saldo-conta - valor-aposta)
        (swap! apostas conj aposta)
        {:status 200 :body "Aposta registrada com sucesso"})
      {:status 400 :body "Saldo insuficiente ou valor inválido para a aposta."})))


(defn obter-aposta-handler [request]
  {:status 200
   :body (json/generate-string @apostas)})





(defn obter-eventos-nba [request]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/4/events/2024-11-13"
                             {:headers {:x-rapidapi-key "sua-chave-api"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:include "scores"
                                             :affiliate_ids "1,2,3"
                                             :offset "0"}})
        dados (:body response)]
    {:status 200 :body dados}))

(defn obter-mercados-nba [request]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/4/openers/2024-11-11"
                             {:headers {:x-rapidapi-key "sua-chave-api"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:offset ""
                                             :include "scores&include=all_periods"}})
        dados (:body response)]
    {:status 200 :body dados}))

(defn get-schedules-nba [request]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/4/schedule"
                             {:headers {:x-rapidapi-key "3e36075547msh24537dc0606651ap103e05jsna0572db9e77c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:limit "100"}})
        data (:body response)]
    {:status 200
     :body data}))

(defn get-schedules-euro [request]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/17/schedule"
                             {:headers {:x-rapidapi-key "3e36075547msh24537dc0606651ap103e05jsna0572db9e77c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:limit "100"}})
        data (:body response)]
    {:status 200
     :body data}))

(def rotas
  (route/expand-routes
   #{["/depositar" :post depositar-handler :route-name :depositar]
     ["/saldo" :get obter-saldo-handler :route-name :saldo]
     ["/apostar" :post registrar-aposta-handler :route-name :registrar-aposta]
     ["/qtdApostada" :get obter-aposta-handler :route-name :obter-apostas]
     ["/eventos-nba" :get obter-eventos-nba :route-name :eventos-nba]
     ["/mercados-nba" :get obter-mercados-nba :route-name :mercados-nba]
     ["/schedules-nba" :get get-schedules-nba :route-name :get-nba-schedules]
     ["/schedules-euro" :get get-schedules-euro :route-name :get-euro-schedules]}))

(def mapa-servico
  {::http/routes rotas
   ::http/port   9999
   ::http/type   :jetty
   ::http/join?  false})

(defn -main []
  (http/start (http/create-server mapa-servico))
  (println "roda poooora"))
