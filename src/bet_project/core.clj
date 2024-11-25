(ns bet-project.core
  (:require
    [bet-project.service.Financeiro :refer [depositar-handler obter-saldo-handler]]
    [io.pedestal.http :as http]
    [io.pedestal.http.route :as route]
    [cheshire.core :as json]
    [clj-http.client :as client])
  (:import (java.time LocalDate)
           (java.time.format DateTimeFormatter)))

(def saldo-conta (atom (bet-project.db.Database/obter-saldo)))
(def apostas (atom []))

(defn today-date []
  (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd")]
    (.format (LocalDate/now) formatter)))

(defn resultado-correto-nba [event-id palpite]
  (let [date (today-date) response (client/get ("https://therundown-therundown-v1.p.rapidapi.com/sports/4/events/" date)
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
            resultado-real (cond
                             (> score-home score-away) "Vitória do time da casa"
                             (< score-home score-away) "Vitória do time visitante"
                             :else "Empate")
            acertou? (= palpite resultado-real)]
       {:status 200
 :body {:score_home score-home
        :score_away score-away
        :resultado_real resultado-real
        :palpite palpite
        :acertou acertou?}}
)
      {:status 404
       :body "Evento não encontrado"})))

(defn open-odds [_]
  (try
    (let [date (today-date)
          response (client/get (str "https://therundown-therundown-v1.p.rapidapi.com/sports/4/openers/" date)
                               {:headers {:x-rapidapi-key "3918fd0ba5msh35b6601a595c453p1fb7dajsn11855839b9d9"
                                          :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                                :query-params {:offset "180"
                                               :include "scores&include=all_periods"}})]
      (json/parse-string (:body response) true))
    (catch Exception e
      (println (str "Erro ao buscar odds abertas: " (.getMessage e)))
      {:status 404
       :body "Erro ao buscar odds abertas"})))


(defn get-open-odds [request]
  (let [id (:id (:json-body request))
        odds (open-odds id)]
    {:status 200
     :body odds}))

(defn resultado-correto-nba-handler [request]
  (let [params (json/parse-string (slurp (:body request)) true)
        event-id (:event-id params)
        palpite (:palpite params)]
    (if (and event-id palpite)
      (resultado-correto-nba event-id palpite)
      {:status 400
       :body "Parâmetros 'event-id' e 'palpite' são obrigatórios."})))



(defn calcular-over-under [score-away score-home linha]
  (let [total-pontos (+ score-away score-home)]
    (cond
      (> total-pontos linha) "Over"
      (< total-pontos linha) "Under"
      :else "Exatamente na linha (Push)")))


(defn prever-over-under [event-id linha]
  (let [date (today-date) response (client/get (str "https://therundown-therundown-v1.p.rapidapi.com/sports/4/events/" date)
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
            resultado (calcular-over-under score-away score-home linha)]
        {:status 200
 :body {:score_away score-away
        :score_home score-home
        :linha linha
        :resultado resultado}}
)
      {:status 404
       :body "Evento não encontrado"})))


(defn over-under-handler [request]
  (let [params (json/parse-string (slurp (:body request)) true)
        event-id (:event-id params)
        linha (:linha params)]
    (if (and event-id linha)
      (prever-over-under event-id linha)
      {:status 400
       :body "Parâmetros 'event-id' e 'linha' são obrigatórios."})))


(defn registrar-aposta-handler [request]
  (let [aposta (json/parse-string (slurp (:body request)) true)
       event-id (:event-id aposta)
        valor-aposta (:quantidade aposta)
        tipo-aposta (:tipo aposta)
        palpite (:palpite aposta)
        linha (:linha aposta)]
    (if (and (number? valor-aposta) (<= valor-aposta @saldo-conta))
      (do
        (swap! saldo-conta - valor-aposta)
        (swap! apostas conj {:quantidade valor-aposta :tipo tipo-aposta})

        (cond
          (= tipo-aposta "resultado-correto")
          (if (and event-id palpite)
            (let [response (resultado-correto-nba event-id palpite)]
              {:status 200 :body (json/generate-string response)})
            {:status 400 :body "Parâmetros 'event-id' e 'palpite' são obrigatórios."})

          (= tipo-aposta "over-under")
          (if (and event-id linha)
            (let [response (prever-over-under event-id linha)]
              {:status 200 :body  response})
            {:status 400 :body "Parâmetros 'event-id' e 'linha' são obrigatórios."})

          :else
          {:status 400 :body "Tipo de aposta inválido."}))

      {:status 400
       :body "Saldo insuficiente ou valor inválido para a aposta."})))



(defn obter-aposta-handler [request]
  {:status 200
   :body (json/generate-string @apostas)})


(defn obter-eventos-nba [request]
  (let [date (today-date)
        response (client/get (str "https://therundown-therundown-v1.p.rapidapi.com/sports/4/events/" date)
                             {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:include "scores"
                                             :affiliate_ids "1,2,3"
                                             :offset "0"}})
        dados (:body response)]
    {:status 200 :body dados}))

(defn obter-mercados-nba [request]
  (let [date (today-date)
        response (client/get (str "https://therundown-therundown-v1.p.rapidapi.com/sports/4/openers/" date)
                             {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
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

(defn get-moneyline[request]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/lines/23f1b36907145528a3c54627323c5c30/moneyline" {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
                                                                                                  :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                                                                                        :query-params {:include "all_periods"}})
        data (:body response)]
    {:status 200
      :body data}))


(def rotas
  (route/expand-routes
   #{["/depositar" :post depositar-handler :route-name :depositar]
     ["/saldo" :get obter-saldo-handler :route-name :saldo]
     ["/moneyline" :get get-moneyline :route-name :moneyline]
     ["/testeOdds" :get get-open-odds :route-name :teste]
     ["/apostar" :post registrar-aposta-handler :route-name :registrar-aposta]
     ["/qtdApostada" :get obter-aposta-handler :route-name :obter-apostas]
     ["/eventos-nba" :get obter-eventos-nba :route-name :eventos-nba]
     ["/mercados-nba" :get obter-mercados-nba :route-name :mercados-nba]
     ["/schedules-nba" :get get-schedules-nba :route-name :get-nba-schedules]
     ["/schedules-euro" :get get-schedules-euro :route-name :get-euro-schedules]
     ["/overUnderNba" :post over-under-handler :route-name :over-under]
    ["/resultadoCorretoNba" :post resultado-correto-nba-handler :route-name :resultado-correto]
   }))

(def mapa-servico
  {::http/routes rotas
   ::http/port   9999
   ::http/type   :jetty
   ::http/join?  false})

(defn -main []
  (http/start (http/create-server mapa-servico))
  (println "Servidor rodando na porta 9999"))
