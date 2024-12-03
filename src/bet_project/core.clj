(ns bet-project.core
  (:require
   [bet-project.db.Database :refer [obter-aposta-cal]]
   [bet-project.service.Aposta :refer [obter-aposta-handler
                                       registrar-aposta-handler]]
   [bet-project.service.Financeiro :refer [depositar-handler
                                           obter-saldo-handler]]
   [bet-project.service.Nba :refer [get-schedules-nba obter-eventos-nba
                                    obter-mercados-nba]]
   [bet-project.service.NHL :refer [obter-eventos-nhl obter-mercados-nhl]]
   [cheshire.core :as json]
   [clj-http.client :as client]
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route]))

;; (defn get-moneyline [request]
;;   (let [response (client/get "https://therundown-therundown-v1.p.rapidapi.com/lines/23f1b36907145528a3c54627323c5c30/moneyline"
;;                              {:headers {:x-rapidapi-key "8b7aaa01f5msh14e11a5a9881536p14b4b3jsn74e4cd56608c"
;;                                         :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
;;                               :query-params {:include "all_periods"}})
;;         data (:body response)]
;;     {:status 200
;;      :body data}))

(def saldo-conta (atom (bet-project.db.Database/obter-saldo)))
;; (def apostas (atom []))

;; ;; (defn today-date []
;; ;;   (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd")]
;; ;;     (.format (LocalDate/now) formatter)))

;; (defn salvar-apostas-no-banco []
;;   (dorun
;;    (map #(inserir-aposta (:event-id %)
;;                          (:quantidade %)
;;                          (:esporte %)
;;                          (:tipo %)
;;                          (:palpite %)
;;                          (:data_aposta %) 
;;                          (:odd_home %)
;;                          (:odd_away %))
;;         @apostas))
;;   (reset! apostas []))

;; (defn registrar-aposta-handler [request]
;;   (let [aposta (json/parse-string (slurp (:body request)) true)
;;         event-id (:event-id aposta)
;;         valor-aposta (:quantidade aposta)
;;         esporte (:esporte aposta)
;;         tipo-aposta (:tipo aposta)
;;         palpite (get aposta :palpite nil)
      
;;         odd-home (get aposta :odd_home nil)  
;;         odd-away (get aposta :odd_away nil)]  
;;     (if (and (number? valor-aposta) (<= valor-aposta @saldo-conta))
;;       (do
;;         (swap! saldo-conta - valor-aposta)
;;         (swap! apostas conj {:event-id event-id
;;                              :quantidade valor-aposta
;;                              :esporte esporte
;;                              :tipo tipo-aposta
;;                              :palpite palpite
;;                              :odd_home odd-home       
;;                              :odd_away odd-away})     
;;         (salvar-apostas-no-banco)
;;         {:status 200
;;          :body (json/generate-string {:mensagem "Aposta registrada com sucesso."
;;                                       :saldo @saldo-conta})})
;;       {:status 400 :body "Saldo insuficiente ou valor da aposta inválido."})))

;; (defn obter-aposta-handler [request]
;;   (let [apostas (obter-apostas)]
;;     {:status 200
;;      :body (json/generate-string apostas)}))

;; (defn open-odds [_]
;;   (try
;;     (let [date (today-date)
;;           response (client/get (str "https://therundown-therundown-v1.p.rapidapi.com/sports/4/openers/" date)
;;                                {:headers {:x-rapidapi-key "3918fd0ba5msh35b6601a595c453p1fb7dajsn11855839b9d9"
;;                                           :x-rapidapi-host "therundown-therundown-v1.p.rapidapi.com"}
;;                                 :query-params {:offset "180"
;;                                                :include "scores&include=all_periods"}})]
;;       (json/parse-string (:body response) true))
;;     (catch Exception e
;;       {:status 404 :body "Erro ao buscar odds abertas"})))

;; (defn get-open-odds [request]
;;   (let [id (:id (:json-body request))
;;         odds (open-odds id)]
;;     {:status 200 :body odds}))

(def rotas
  (route/expand-routes
   #{["/depositar" :post depositar-handler :route-name :depositar]
     ["/saldo" :get obter-saldo-handler :route-name :saldo]
     
     
     ["/apostar" :post registrar-aposta-handler :route-name :registrar-aposta]
     ["/liquidaposta" :get obter-aposta-cal :route-name :obter-apostas-cal]
     ["/aposta" :get obter-aposta-handler :route-name :obter-apostas]
     ["/eventos-nba" :get obter-eventos-nba :route-name :eventos-nba]
     ["/mercados-nba" :get obter-mercados-nba :route-name :mercados-nba]
     ["/mercados-nhl" :get obter-mercados-nhl :route-name :mercados-nhl]
     ["/schedules-nba" :get get-schedules-nba :route-name :get-nba-schedules]
     
     ["/events-nhl" :get obter-eventos-nhl :route-name :events-fut]
     }))

(def mapa-servico
  {::http/routes rotas
   ::http/port   9999
   ::http/type   :jetty
   ::http/join?  false})

(declare menu-principal)

(defn mostrar-menu []
  (println "\n====== Sistema de Apostas ======")
  (println "1. Gerenciar Conta")
  (println "2. Fazer Aposta")
  (println "3. Consultar Resultados")
  (println "4. Sair")
  (println "================================")
  (print "Escolha uma opcao: "))


(defn gerenciar-conta []
  (println "\n====== Gerenciar Conta ======")
  (println "1. Consultar Saldo")
  (println "2. Depositar")
  (println "3. Voltar")
  (print "Escolha uma opcao: ")
  (let [opcao (read-line)]
    (cond
      (= opcao "1")
      (let [response (client/get "http://localhost:9999/saldo")]
        (println "Seu saldo atual e: RS" (:saldo (json/parse-string (:body response) true))))

      (= opcao "2")
      (do
        (print "Digite o valor a depositar: ")
        (let [quantidade (Double/parseDouble (read-line))
              response (client/post "http://localhost:9999/depositar"
                                    {:body (json/generate-string {:quantidade quantidade})
                                     :headers {"Content-Type" "application/json"}})]
          (println "Depósito realizado com sucesso!")
          (println "Saldo atualizado:" (:saldo (json/parse-string (:body response) true)))))


      (= opcao "3") (println "Voltando ao menu principal...")
      :else (println "Opção invalida!")))
  (menu-principal))

(defn fazer-aposta []
  (println "\n====== Fazer Aposta ======")
  (print "Digite o ID do evento: ")
  (let [event-id (read-line)]
    (print "Digite o valor da aposta: ")
    (let [quantidade (Double/parseDouble (read-line))
          esporte (do (print "Esporte (futebol/basquete): ") (read-line))
          tipo (do (print "Tipo de aposta (resultado-correto/over-and-under): ") (read-line))
          palpite (if (= tipo "resultado-correto")
                    (do (print "Digite o palpite (Casa/Visitante/Empate): ") (read-line))
                    nil)
          linha (if (= tipo "over-and-under")
                  (do (print "Digite a linha do over/under: ") (Double/parseDouble (read-line)))
                  nil)
          odd-home (if (= tipo "over-and-under")
                     (do (print "Digite a odd home: ") (Double/parseDouble (read-line)))
                     nil)
          odd-away (if (= tipo "over-and-under")
                     (do (print "Digite a odd away: ") (Double/parseDouble (read-line)))
                     nil)
          response (client/post "http://localhost:9999/apostar"
                                {:body (json/generate-string
                                        {:event-id event-id
                                         :quantidade quantidade
                                         :esporte esporte
                                         :tipo tipo
                                         :palpite palpite
                              
                                         :odd_home odd-home      
                                         :odd_away odd-away})   
                                 :headers {"Content-Type" "application/json"}})]
      (println "Resultado:" (:mensagem (json/parse-string (:body response) true)))))
  (menu-principal))

(defn consultar-resultados []
  (println "\n====== Consultar Resultados ======")
  (let [response (client/get "http://localhost:9999/aposta")
        apostas (json/parse-string (:body response) true)]
    (dorun
     (map #(println (str "Evento ID: " (:event_id %)
                         ", Quantidade: " (:quantidade %)
                         ", Tipo: " (:tipo %)
                         ", Esporte: " (:esporte %)
                         ", Palpite: " (:palpite %) 
                         ", Data da Aposta: " (:data_aposta %)))
          apostas)))
  (menu-principal))

(defn menu-principal []
  (mostrar-menu)
  (let [opcao (read-line)]
    (cond
      (= opcao "1") (gerenciar-conta)
      (= opcao "2") (fazer-aposta)
      (= opcao "3") (consultar-resultados)
      (= opcao "4") (println "Saindo do sistema...")
      :else (do (println "Opcao invalida!") (menu-principal)))))

(defn -main []
  (http/start (http/create-server mapa-servico))
  (menu-principal))
