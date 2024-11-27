(ns bet-project.service.Nba
  (:require
    [clj-http.client :as client]
    [cheshire.core :as json]
    [bet-project.db.Database :refer [inserir-aposta atualizar-saldo obter-saldo]]
    )
  (:import (java.time LocalDate)
           (java.time.format DateTimeFormatter)))

(defn today-date []
  (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd")]
    (.format (LocalDate/now) formatter)))
(defn get-schedules-nba [request]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/4/schedule"
                             {:headers {:x-rapidapi-key "3e36075547msh24537dc0606651ap103e05jsna0572db9e77c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:limit "100"}})
        data (:body response)]
    {:status 200
     :body data}))

(defn obter-mercados-nba [request]
  (let [date (today-date)
        response (client/get (str "https://therundown-therundown-v1.p.rapidapi.com/sports/4/openers/" date)
                             {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:offset ""
                                             :include "scores&include=all_periods"}})
        dados (:body response)]
    {:status 200 :body dados}))

(defn resultado-correto-nba [event-id palpite]
  (try
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
        (let [score (:score evento)
              score-away (:score_away score)
              score-home (:score_home score)]
          (if (and score-away score-home)
            (let [resultado-real (cond
                                   (> score-home score-away) "Casa"
                                   (< score-home score-away) "Visitante"
                                   :else "Empate")
                  acertou? (= palpite resultado-real)]
              {:status 200
               :body {:score_home score-home
                      :score_away score-away
                      :resultado_real resultado-real
                      :palpite palpite
                      :acertou acertou?}})
            {:status 500
             :body "Dados de pontuação incompletos no evento."}))
        {:status 404
         :body "Evento não encontrado"}))
    (catch Exception e
      (println "Erro ao buscar dados do evento:" (.getMessage e))
      {:status 500
       :body "Erro ao buscar dados do evento."})))

(defn obter-eventos-nba [request]
  (let [date (today-date)
        response (client/get (str "https://therundown-therundown-v1.p.rapidapi.com/sports/4/events/" date)
                             {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:include "scores"
                                             :affiliate_ids "2"
                                             :offset "0"}})
        dados (:body response)]
    {:status 200 :body dados}))

(defn resultado-correto-nba-handler [request]
  (try
    (let [params (json/parse-string (slurp (:body request)) true)
          event-id (:event-id params)
          quantidade (:quantidade params)
          tipo (:tipo params)
          palpite (:palpite params)]
      (if (and event-id quantidade tipo palpite (pos? quantidade))
        (let [saldo-atual (obter-saldo)]
          (if (>= saldo-atual quantidade)
            (do
              (atualizar-saldo (- quantidade))
            
              {:status 200
               :body (json/generate-string {:message "Aposta registrada com sucesso!"
                                            :saldo   (obter-saldo)})})
            {:status 400
             :body (json/generate-string {:error "Saldo insuficiente para realizar a aposta."})}))
        {:status 400
         :body "Parâmetros 'event-id', 'quantidade', 'tipo' e 'palpite' são obrigatórios e a quantidade deve ser positiva."}))
    (catch Exception e
      (println "Erro no handler resultado-correto-nba:" (.getMessage e))
      {:status 500
       :body "Erro interno no servidor."})))