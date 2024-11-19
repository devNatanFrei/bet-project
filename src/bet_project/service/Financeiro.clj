(ns bet-project.service.Financeiro
  (:require [bet-project.db.Database :refer [atualizar-saldo obter-saldo]]
            [cheshire.core :as json])
  )

(defn depositar-handler [request]
  (let [params (json/parse-string (slurp (:body request)) true)
        quantidade (:quantidade params)]
    (if (number? quantidade)
      (do
        (atualizar-saldo quantidade)
        {:status 200
         :body   (json/generate-string {:message "Depósito realizado com sucesso!"
                                        :saldo   (obter-saldo)})})
      {:status 400
       :body   (json/generate-string {:error "Valor inválido para depósito."})})))

(defn obter-saldo-handler [_]
  {:status 200
   :body   (json/generate-string {:saldo (obter-saldo)})})