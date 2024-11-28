(ns bet-project.service.euro
  (:require
   [bet-project.service.Financeiro :refer [depositar-handler obter-saldo-handler]]
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route]
   [cheshire.core :as json]
   [clj-http.client :as client])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]))

(defn today-date []
  (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd")]
    (.format (LocalDate/now) formatter)))


(defn get-schedules-euro [request]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/17/schedule"
                             {:headers {:x-rapidapi-key "3e36075547msh24537dc0606651ap103e05jsna0572db9e77c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:limit "100"}})
        data (:body response)]
    {:status 200
     :body data}))

(defn obter-mercados-euro [request]
  (let [date (today-date)
        response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/4/openers/2024-11-28" {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
                                                                                                                      :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                                                                                                            :query-params {:offset "300"
                                                                                                                           :include "scores&include=all_periods"}})
        dados (:body response)]
    {:status 200 :body dados}))

(defn obter-eventos-euro [request]
  (let [date (today-date)
        response (client/get (str "https://therundown-therundown-v1.p.rapidapi.com/sports/4/events/" date) {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
                                                                                                                      :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                                                                                                            :query-params {:include "scores"
                                                                                                                           :affiliate_ids "1,2,3"
                                                                                                                           :offset "0"}})
        dados (:body response)]
    {:status 200 :body dados}))