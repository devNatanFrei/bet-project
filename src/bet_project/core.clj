(ns bet-project.core
  (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route]
   [cheshire.core :as json]
   [clj-http.client :as client]))

(def account-balance (atom 0))


(defn deposit-handler
  [request]
  (let [amount (:amount (json/parse-string (slurp (:body request)) true))]
    (if (number? amount)
      (do
        (swap! account-balance + amount)
        {:status 200})
      )))


(defn balance-handler
  [request]
  (let [amount (:amount (json/parse-string (slurp (:body request)) true))]
    (if (number? amount)
      (do
        (swap! account-balance - amount)
        {:status 200}))))

(defn get-schedules-nba [request]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/4/schedule" {:headers {:x-rapidapi-key "3e36075547msh24537dc0606651ap103e05jsna0572db9e77c"
                                                                                                            :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                                                                                                  :query-params {:limit "100"}})
                         data (:body response)]
                     {:status 200
                      :body data}))

(defn get-schedules-euro [request]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/17/schedule" {:headers {:x-rapidapi-key "3e36075547msh24537dc0606651ap103e05jsna0572db9e77c"
                                                                                                            :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                                                                                                  :query-params {:limit "100"}})
        data (:body response)]
    {:status 200
     :body data}))

  (defn get-deposit [request]
  {:status 200
   :body (json/generate-string {:balance @account-balance})})


(defn get-events-nba [request]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/4/events/2022-12-20"
                             {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:include "scores"
                                             :affiliate_ids "1,2,3"
                                             :offset "0"}})
        data (:body response)]
    {:status 200
     :body data}))


(defn get-openers-nba [request]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/4/openers/2024-11-11"
                             {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:offset ""
                                             :include "scores&include=all_periods"}})
        data (:body response)]
    {:status 200
     :body data}))


(defn get-openers-euro [request]
  (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/sports/17/openers/2024-12-11"
                             {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
                                        :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
                              :query-params {:include "scores&include=all_periods"}})
        data (:body response)]
    {:status 200
     :body data}))


(def routes
  (route/expand-routes
   #{["/deposit" :post deposit-handler :route-name :deposit]
     ["/balance" :get get-deposit :route-name :get-balance]
     ["/events-nba" :get get-events-nba :route-name :get-events]
     ["/openers-nba" :get get-openers-nba :route-name :get-openers]
     ["/schedules-nba" :get get-schedules-nba :route-name :get-nba-schedules]
     ["/schedules-euro" :get get-schedules-euro :route-name :get-euro-schedules]
     ["/openers-euro" :get get-openers-euro :route-name :get-openers-euro]}))


(def service-map
  {::http/routes routes
   ::http/port   9999
   ::http/type   :jetty
   ::http/join?  false})

  (http/start (http/create-server service-map))
  (println "rodou porra")
