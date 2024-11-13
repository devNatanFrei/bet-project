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


(defn calcular-resultado-handicap [score-away score-home handicap]
  (let [adjusted-score-home (+ score-home handicap)]
    (cond
      (> adjusted-score-home score-away) "Vitória do time da casa"
      (< adjusted-score-home score-away) "Vitória do time visitante"
      :else "Empate com o handicap")))

(defn handicap-asiatico [event-id handicap]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/4/events/2024-11-13"
                             {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:include "scores"
                                             :affiliate_ids "1,2,3"
                                             :offset "0"}})
        dados (json/parse-string (:body response) true)
        eventos (:events dados)
        evento (some #(when (= (:event_id %) event-id) %) eventos)]
    (if evento
      (let [score-away (:score_away (:score evento))
            score-home (:score_home (:score evento))
            resultado (calcular-resultado-handicap score-away score-home handicap)]
        {:status 200
         :body (json/generate-string {:score_away score-away
                                      :score_home score-home
                                      :resultado resultado})})
      {:status 404
       :body "Evento não encontrado"})))

(defn handicap-asiatico-handler [request]
  (let [params (json/parse-string (slurp (:body request)) true)
        event-id (:event-id params)
        handicap (:handicap params)]
    (if (and event-id (number? handicap))
      (handicap-asiatico event-id handicap)
      {:status 400
       :body "Parâmetros inválidos para cálculo do handicap asiático."})))


(defn resultado-correto [event-id]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/4/events/2024-11-13"
                             {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:include "scores"
                                             :affiliate_ids "1,2,3"
                                             :offset "0"}})
        dados (json/parse-string (:body response) true)
        eventos (:events dados)
        evento (some #(when (= (:event_id %) event-id) %) eventos)]
    (if evento
      (let [score-away (:score_away (:score evento))
            score-home (:score_home (:score evento))
            resultado (cond
                        (> score-home score-away) "Vitória do time da casa"
                        (< score-home score-away) "Vitória do time visitante"
                        :else "Empate")]
        {:status 200
         :body (json/generate-string {:score_home score-home
                                      :score_away score-away
                                      :resultado resultado})})
      {:status 404
       :body "Evento não encontrado"})))


(defn resultado-correto-handler [request]
  (let [params (json/parse-string (slurp (:body request)) true)
        event-id (:event-id params)]
    (if event-id
      (resultado-correto event-id)
      {:status 400
       :body "Parâmetro 'event-id' é obrigatório para obter o resultado correto."})))


(defn obter-eventos-nba [request]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/4/events/2024-11-13" {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
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
     ["/schedules-euro" :get get-schedules-euro :route-name :get-euro-schedules]
     ["/handicapAsia" :post handicap-asiatico-handler :route-name :handicap-asiatico]
     ["/resultadoCorreto" :post resultado-correto-handler :route-name :resultado-correto]}))

(def mapa-servico
  {::http/routes rotas
   ::http/port   8080
   ::http/type   :jetty
   ::http/join?  false})

(defn -main []
  
  (http/start (http/create-server mapa-servico))
)
