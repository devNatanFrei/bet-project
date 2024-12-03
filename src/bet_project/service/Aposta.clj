(ns bet-project.service.Aposta 
  (:require
   [bet-project.service.Financeiro :refer [saldo-conta]]
   [bet-project.db.Database :refer [inserir-aposta obter-apostas]]
   [cheshire.core :as json]))

(def apostas (atom []))

;; (defn today-date []
;;   (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd")]
;;     (.format (LocalDate/now) formatter)))

(defn salvar-apostas-no-banco []
  (dorun
   (map #(inserir-aposta (:event-id %)
                         (:quantidade %)
                         (:esporte %)
                         (:tipo %)
                         (:palpite %)
                         (:odd_home %)
                         (:odd_away %)
                         (:total_over %))
        @apostas))
  (reset! apostas []))

(defn registrar-aposta-handler [request]
  (let [aposta (json/parse-string (slurp (:body request)) true)
        event-id (:event-id aposta)
        valor-aposta (:quantidade aposta)
        esporte (:esporte aposta)
        tipo-aposta (:tipo aposta)
        palpite (get aposta :palpite nil)
        odd-home (get aposta :odd_home nil)
        odd-away (get aposta :odd_away nil)]
    (if (and (number? valor-aposta) (<= valor-aposta @saldo-conta))
      (do
        (swap! saldo-conta - valor-aposta)
        (swap! apostas conj {:event-id event-id
                             :quantidade valor-aposta
                             :esporte esporte
                             :tipo tipo-aposta
                             :palpite palpite
                             :odd_home odd-home
                             :odd_away odd-away})
        (salvar-apostas-no-banco)
        {:status 200
         :body (json/generate-string {:mensagem "Aposta registrada com sucesso."
                                      :saldo @saldo-conta})})
      {:status 400 :body "Saldo insuficiente ou valor da aposta inválido."})))

(defn obter-aposta-handler [request]
  (let [apostas (obter-apostas)]
    {:status 200
     :body (json/generate-string apostas)}))