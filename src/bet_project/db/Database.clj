(ns bet-project.db.Database
  (:require
   [bet-project.service.Futebol :refer [obter-aposta-futebol-handler]]
   [bet-project.service.Nba :refer [obter-aposta-nba-handler]]
   [clojure.java.jdbc :as jdbc])
  )

(def db-spec
  {:dbtype   "mysql"
   :dbname   "bet"
   :host     "localhost"
   :port     3306
   :user     "root"
   :password "123456"})
(println "Sucesso na conexao")

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
            :data_aposta (:data_aposta aposta)})
         results)))


(defn obter-apostas-cal []
  (println "Processando apostas...")
  (let [results (jdbc/query db-spec ["SELECT * FROM apostas"])]
    (dorun (map (fn [aposta]
                  (let [event-id (:event_id aposta)
                        tipo     (:tipo aposta)
                        esporte  (:esporte aposta)
                        linha    (:linha aposta)
                        palpite  (:palpite aposta)]
                    (cond
                      (= esporte "futebol")
                      (obter-aposta-futebol-handler event-id tipo linha palpite)

                      (= esporte "basquete")
                      (obter-aposta-nba-handler event-id tipo linha palpite)

                      :else
                      (println "Esporte inválido encontrado:" esporte))))
                results))))

(defn obter-saldo []
  (let [result (jdbc/query db-spec ["SELECT valor FROM saldo LIMIT 1"])]
    (:valor (first result))))

(defn atualizar-saldo [quantidade]
  (jdbc/execute! db-spec ["UPDATE saldo SET valor = valor + ?" quantidade]))

(defn inserir-aposta [event-id quantidade esporte tipo palpite linha]
  (println "Inserindo aposta...")
  (let [query "INSERT INTO apostas (event_id, quantidade, esporte, tipo, palpite, linha)
                VALUES (?, ?, ?, ?, ?, ?)"]
    (try

      (jdbc/execute! db-spec [query event-id quantidade esporte tipo
                              (if (empty? palpite) nil palpite)
                              (if (nil? linha) nil linha)])
      (println "Aposta inserida com sucesso.")
      (catch Exception e
        (println "Erro ao inserir aposta:" (.getMessage e))))))



