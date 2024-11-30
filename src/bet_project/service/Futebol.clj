(ns bet-project.service.Futebol
  (:require
   [bet-project.db.Database :refer [obter-apostas]]
   [cheshire.core :as json]
   [clj-http.client :as client])
  (:import
   (java.time LocalDate)
   (java.time.format DateTimeFormatter)))

(defn today-date []
  (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd")]
    (.format (LocalDate/now) formatter)))

(defn get-schedules-futebol [request]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/17/schedule"
                             {:headers {:x-rapidapi-key "3e36075547msh24537dc0606651ap103e05jsna0572db9e77c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:limit "100"}})
        data (:body response)]
    {:status 200
     :body data}))

(defn obter-mercados-futebol [request]
  (let [date (today-date)
        response (client/get (str "https://therundown-therundown-v1.p.rapidapi.com/sports/17/openers/" date)
                             {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:offset ""
                                             :include "scores&include=all_periods"}})
        dados (:body response)]
    {:status 200 :body dados}))

(defn obter-eventos-futebol [request]
  (let [date (today-date)
        response (client/get (str "https://therundown-therundown-v1.p.rapidapi.com/sports/17/events/" date)
                             {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:include "scores"
                                             :affiliate_ids "1,2,3"
                                             :offset "0"}})
        dados (:body response)]
    {:status 200 :body dados}))

(defn calcular-resultado-futebol [event-id palpite]
  (try
    (let [date (today-date)
          response (client/get (str "https://therundown-therundown-v1.p.rapidapi.com/sports/17/events/" date)
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
              score-home (:score_home score)
              score-away (:score_away score)]
          (if (and score-home score-away)
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
      (println "Erro ao calcular resultado do evento:" (.getMessage e))
      {:status 500
       :body "Erro ao calcular resultado do evento."})))

(defn calcular-over-under-futebol [score-away score-home linha]
  (let [total-pontos (+ score-away score-home)]
    (cond
      (> total-pontos linha) "Over"
      (< total-pontos linha) "Under"
      :else "Exatamente na linha (Push)")))

(defn prever-over-under-futebol [event-id linha]
  (let [date (today-date)
        response (client/get (str "https://therundown-therundown-v1.p.rapidapi.com/sports/17/events/" date)
                             {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:include "scores"
                                             :affiliate_ids "1,2,3"
                                             :offset "0"}})
        dados (json/parse-string (:body response) true)
        eventos (:events dados)
        evento (some #(when (= (:event_id %) event-id) %) eventos)]
    (if evento
      (let [score-home (:score_home (:score evento))
            score-away (:score_away (:score evento))
            resultado (calcular-over-under-futebol score-home score-away linha)]
        {:status 200
         :body {:score_home score-home
                :score_away score-away
                :linha linha
                :resultado resultado}})
      {:status 404
       :body "Evento não encontrado"})))

(defn obter-aposta-futebol-handler [request]
  (let [apostas (obter-apostas)
        resultados (map (fn [aposta]
                          (let [event-id (:event_id aposta)
                                tipo (:tipo aposta)
                                linha (:linha aposta)
                                palpite (:palpite aposta)]
                            (cond
                              (= tipo "resultado-correto")
                              (calcular-resultado-futebol event-id palpite)

                              (= tipo "over-and-under")
                              (prever-over-under-futebol event-id linha)

                              :else
                              {:status 400
                               :body "Tipo de aposta inválido."})))
                        apostas)]
    {:status 200
     :body (json/generate-string resultados)}))
