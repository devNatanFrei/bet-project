(ns bet-project.db.Database
  (:require
   [bet-project.service.NHL :refer [calcular-resultado-nhl]]
   [bet-project.service.Nba :refer [resultado-correto-nba prever-over-under-nba]]
   [clojure.java.jdbc :as jdbc]
   [cheshire.core :as json]))

(def db-spec
  {:dbtype   "mysql"
   :dbname   "bet"
   :host     "localhost"
   :port     3306
   :user     "root"
   :password "123456"})

(println "Sucesso na conexão")

(defn create-saldo-table []
  (jdbc/execute! db-spec
                 ["CREATE TABLE IF NOT EXISTS saldo (
                     id INT AUTO_INCREMENT PRIMARY KEY,
                     valor DECIMAL(15,2) NOT NULL DEFAULT 0.0
                   )"])
  (jdbc/execute! db-spec
                 ["INSERT INTO saldo (valor) VALUES (0.0) ON DUPLICATE KEY UPDATE id=id"]))

(defn create-apostas-table []
  (jdbc/execute! db-spec
                 ["CREATE TABLE IF NOT EXISTS apostas (
                     event_id VARCHAR(255) PRIMARY KEY NOT NULL,
                     quantidade INTEGER NOT NULL,
                     tipo VARCHAR(255) NOT NULL,
                     esporte VARCHAR(255) NOT NULL,
                     palpite VARCHAR(255) NULL,
                     linha VARCHAR(255) NULL,
                     odd_home DECIMAL(15,2) NULL,
                     odd_away DECIMAL(15,2) NULL,
                     data_aposta TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                   )"]))

(create-saldo-table)
(create-apostas-table)

(defn obter-apostas []
  (let [results (jdbc/query db-spec ["SELECT * FROM apostas"])]
    (map (fn [aposta]
           {:event_id (:event_id aposta)
            :quantidade (:quantidade aposta)
            :tipo (:tipo aposta)
            :esporte (:esporte aposta)
            :palpite (:palpite aposta)
            :linha (:linha aposta)
            :odd_home (:odd_home aposta)
            :odd_away (:odd_away aposta)
            :data_aposta (:data_aposta aposta)})
         results)))

(defn calcular-moneyline [moneyline-value]
  (if (= moneyline-value 0.0001)
    0
    (let [value (int moneyline-value)]
      (cond
        (< value 0) (inc (double (Math/abs (/ 100 value))))
        (> value 0) (double (/ value 100))))))

(defn calcular-ganho [quantidade moneyline]
  (* quantidade moneyline))

(defn prever-over-under [score-away score-home linha palpite]
  (let [total-pontos (+ score-away score-home)]
    (cond
      (> total-pontos linha) (= palpite "Over")
      (< total-pontos linha) (= palpite "Under")
      :else false)))  

(defn prever-over-under [score-away score-home linha palpite]
  (let [total-pontos (+ score-away score-home)]
    (cond
      (> total-pontos linha) (= palpite "Over")
      (< total-pontos linha) (= palpite "Under")
      :else false)))  

(defn obter-aposta-cal [_]
  (try
    (let [apostas (jdbc/query db-spec ["SELECT * FROM apostas"])]
      (let [resultados
            (map (fn [aposta]
                   (let [event-id (:event_id aposta)
                         tipo (:tipo aposta)
                         esporte (:esporte aposta)
                         linha (:linha aposta)
                         palpite (:palpite aposta)
                         odd-home (:odd_home aposta)
                         odd-away (:odd_away aposta)
                         moneyline-home (calcular-moneyline odd-home)
                         moneyline-away (calcular-moneyline odd-away)
                         ganho (cond
                                
                                 (and (= tipo "resultado-correto")
                                      (= esporte "basquete"))
                                 (let [{:keys [status body]} (resultado-correto-nba event-id palpite)]
                                   (when (= status 200)
                                     (cond (and (=  true (:acertou body)) (> (:score_home body) (:score_away body)))
                                       (calcular-ganho (:quantidade aposta) moneyline-home) 
                                       (and (=  true (:acertou body)) (< (:score_home body) (:score_away body)))
                                        (calcular-ganho (:quantidade aposta) moneyline-away)
                                           
                                           ))) 

                                 (and (= tipo "resultado-correto")
                                      (= esporte "nhl"))
                                 (let [{:keys [status body]} (calcular-resultado-nhl event-id palpite)]
                                   (when (= status 200)
                                      (cond (and (=  true (:acertou body)) (> (:score_home body) (:score_away body)))
                                           (calcular-ganho (:quantidade aposta) moneyline-home)
                                           (and (=  true (:acertou body)) (< (:score_home body) (:score_away body)))
                                           (calcular-ganho (:quantidade aposta) moneyline-away))))  

                                 
                                 (= tipo "over-and-under")
                                 (let [{:keys [status body]} (if (= esporte "basquete")
                                                               (resultado-correto-nba event-id palpite)
                                                               (calcular-resultado-nhl event-id palpite))]  ; Obter scores
                                   (when (= status 200)
                                     (let [score-away (:score_away body)
                                           score-home (:score_home body)
                                           acertou? (prever-over-under score-away score-home linha palpite)]
                                       (if acertou?
                                         (if (= esporte "futebol")
                                           (calcular-ganho (:quantidade aposta) moneyline-home)
                                           (calcular-ganho (:quantidade aposta) moneyline-away))
                                         0)))) 

                                 :else 0)]  
                     {:event_id event-id
                      :tipo tipo
                      :esporte esporte
                      :palpite palpite
                      :ganho ganho
                      :odd_home odd-home
                      :odd_away odd-away}))
                 apostas)
            respostas (remove nil? resultados)]
        {:status 200
         :body (json/generate-string {:resultados respostas})}))
    (catch Exception e
      (println "Erro ao processar apostas:" (.getMessage e))
      {:status 500
       :body (json/generate-string {:erro "Erro ao processar as apostas"})})))


(defn obter-saldo []
  (let [result (jdbc/query db-spec ["SELECT valor FROM saldo LIMIT 1"])]
    (:valor (first result))))

(defn atualizar-saldo [quantidade]
  (jdbc/execute! db-spec ["UPDATE saldo SET valor = valor + ?" quantidade]))

(defn inserir-aposta [event-id quantidade esporte tipo palpite linha odd_home odd_away]
  (println "Inserindo aposta...")
  (let [query "INSERT INTO apostas (event_id, quantidade, esporte, tipo, palpite, linha, odd_home, odd_away)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)"]
    (try
      (jdbc/execute! db-spec [query event-id quantidade esporte tipo
                              (if (empty? palpite) nil palpite)
                              (if (nil? linha) nil linha)
                              odd_home odd_away])
      (println "Aposta inserida com sucesso.")
      (catch Exception e
        (println "Erro ao inserir aposta:" (.getMessage e))))))

