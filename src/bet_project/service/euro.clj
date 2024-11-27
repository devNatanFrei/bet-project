(ns bet-project.service.euro
  (:require
   [bet-project.service.Financeiro :refer [depositar-handler obter-saldo-handler]] 
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route]
   [cheshire.core :as json]
   [clj-http.client :as client])
  (:import (java.time LocalDate)
           (java.time.format DateTimeFormatter)))


