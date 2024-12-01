(ns bet-project.core

  (:require
   [bet-project.db.Database :refer [inserir-aposta  obter-apostas obter-apostas-cal]]
   [bet-project.service.Financeiro :refer [depositar-handler
                                           obter-saldo-handler]]
   [bet-project.service.Futebol :refer [get-schedules-futebol]]
   [bet-project.service.Nba :refer [get-schedules-nba obter-eventos-nba
                                    obter-mercados-nba
                                    ]]
   [cheshire.core :as json]
   [clj-http.client :as client]
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route])
  (:import
   (java.time LocalDate)
   (java.time.format DateTimeFormatter)))

(def saldo-conta (atom (bet-project.db.Database/obter-saldo)))
(def apostas (atom []))

;; (defn calcular-odds-e-ganhos [event-id]
;;   (let [aposta (obter-apostas event-id)]
;;     (if aposta
;;       (let [quantidade (:quantidade aposta)
;;             esporte (:esporte aposta)
;;             tipo (:tipo aposta)
;;            ]
;;         {:status 200
;;          :body {:event-id event-id
;;                 :quantidade quantidade
;;                 :esporte esporte
;;                 :tipo tipo
;;                 }})
;;       {:status 404
;;        :body "Aposta não encontrada"})))


;; (defn calcular-odds-handler [request]
;;   (let [event-id (get-in request [:query-params "event-id"])]
;;     (if event-id 
;;       (calcular-odds-e-ganhos event-id)
;;       {:status 400
;;        :body "Parâmetro 'event-id' é obrigatório."})))
  

(defn salvar-apostas-no-banco []
  (println "fdhfjdsbhvhigdfvgufdugvtdtc.")

  (dorun (map #(inserir-aposta (:event-id %)
                               (:quantidade %)
                               (:esporte %)
                               (:tipo %)
                               (:palpite %)
                               (:linha %))
              @apostas))
  (reset! apostas [])) 


(defn registrar-aposta-handler [request]
  (let [aposta (json/parse-string (slurp (:body request)) true)
        event-id (:event-id aposta)
        valor-aposta (:quantidade aposta)
        esporte (:esporte aposta)
        tipo-aposta (:tipo aposta)
        palpite (get aposta :palpite nil)
        linha (get aposta :linha nil)]
    (if (and (number? valor-aposta) (<= valor-aposta @saldo-conta))
      (do
        (swap! saldo-conta - valor-aposta)
        (swap! apostas conj {:event-id event-id
                             :quantidade valor-aposta
                             :esporte esporte
                             :tipo tipo-aposta
                             :palpite palpite
                             :linha linha})
        (println "padedgfuadfydggydgyfydgfygdy")
        (salvar-apostas-no-banco)
        {:status 200
         :body (json/generate-string {:mensagem "Aposta registrada com sucesso."
                                      :saldo @saldo-conta})})
     
      {:status 400 :body "Saldo insuficiente ou valor da aposta inválido."})))


 (defn obter-aposta-handler [request]
   (let [apostas (obter-apostas)]
     {:status 200
      :body (json/generate-string apostas)})) 


 (defn obter-aposta-cal [request]
   (let [apostas (obter-apostas)]
     {:status 200
      :body( apostas)})) 

(defn today-date []
  (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd")]
    (.format (LocalDate/now) formatter)))
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



;; (defn over-under-handler [request]
;;   (let [params (json/parse-string (slurp (:body request)) true)
;;         event-id (:event-id params)
;;         linha (:linha params)]
;;     (if (and event-id linha)
;;       (prever-over-under event-id linha)
;;       {:status 400
;;        :body "Parâmetros 'event-id' e 'linha' são obrigatórios."})))


;; (defn registrar-aposta-handler [request]
;;    (let [aposta (json/parse-string (slurp (:body request)) true)
;;          event-id (:event-id aposta)
;;          valor-aposta (:quantidade aposta)
;;          esporte (:esporte aposta)
;;          tipo-aposta (:tipo aposta)
;;          palpite (:palpite aposta)
;;          linha (:linha aposta)]
;;      (if (and (number? valor-aposta) (<= valor-aposta @saldo-conta))
;;        (do
;;          (swap! saldo-conta - valor-aposta)
;;          (swap! apostas conj {:quantidade valor-aposta :tipo tipo-aposta})
;;          (cond
;;            (= esporte "basquete")
;;            (cond
;;              (= tipo-aposta "resultado-correto")
;;              (if (and event-id palpite)
;;                (let [response (resultado-correto-nba event-id palpite)]
;;                  {:status 200 :body (json/generate-string response)})
;;                {:status 400 :body "Parâmetros 'event-id' e 'palpite' são obrigatórios."})

;;              (= tipo-aposta "over-under")
;;              (if (and event-id linha)
;;                (let [response (prever-over-under event-id linha)]
;;                  {:status 200 :body response})
;;                {:status 400 :body "Parâmetros 'event-id' e 'linha' são obrigatórios."})

;;              :else
;;              {:status 400 :body "Tipo de aposta inválido."})

;;            (= esporte "futebol")
;;            (cond
;;              (= tipo-aposta "resultado-correto")
;;              (if (and event-id palpite)
;;                (let [response (resultado-correto-nba event-id palpite)]
;;                  {:status 200 :body (json/generate-string response)})
;;                {:status 400 :body "Parâmetros 'event-id' e 'palpite' são obrigatórios."})

;;              (= tipo-aposta "over-under")
;;              (if (and event-id linha)
;;                (let [response (prever-over-under event-id linha)]
;;                  {:status 200 :body response})
;;                {:status 400 :body "Parâmetros 'event-id' e 'linha' são obrigatórios."})

;;              :else
;;              {:status 400 :body "Tipo de aposta inválido."}))

;;          :else
;;        {:status 400 :body "Esporte inválido."}))
;;      {:status 400 :body "Saldo insuficiente ou valor da aposta inválido."}))



;; (defn obter-aposta-handler [request]
;;   {:status 200
;;    :body (json/generate-string @apostas)})

(obter-apostas-cal)

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
     ["/aposta" :get obter-aposta-handler :route-name :obter-apostas]
      ["/liquidaposta" :get obter-aposta-cal :route-name :obter-apostas-cal]
     ["/eventos-nba" :get obter-eventos-nba :route-name :eventos-nba]
     ["/mercados-nba" :get obter-mercados-nba :route-name :mercados-nba]
     ["/schedules-nba" :get get-schedules-nba :route-name :get-nba-schedules]
     ["/schedules-euro" :get get-schedules-futebol :route-name :get-euro-schedules]
   }))

(def mapa-servico
  {::http/routes rotas
   ::http/port   8080
   ::http/type   :jetty
   ::http/join?  false})

(defn -main []
  (http/start (http/create-server mapa-servico))
  (println "Servidor rodando na porta 9999"))
