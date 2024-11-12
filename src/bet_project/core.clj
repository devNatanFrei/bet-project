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
     ["/eventsnba" :get get-events-nba :route-name :get-events]
     ["/openersnba" :get get-openers-nba :route-name :get-openers]
     ["/openerseuro" :get get-openers-euro :route-name :get-openers-euro]}))


(def service-map
  {::http/routes routes
   ::http/port   9999
   ::http/type   :jetty
   ::http/join?  false})

(defn -main [& args]
  (http/start (http/create-server service-map))
  (println "roda casseta"))
