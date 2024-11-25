(ns bet-project.db.Database
  (:require
    [clojure.java.jdbc :as jdbc])
  )

(def db-spec
  {:dbtype   "mysql"
   :dbname   "bet"
   :host     "localhost"
   :port     3306
   :user     "user"
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
                     quantidade INTEGER NOT NULL ,
                     tipo VARCHAR(255) NOT NULL,
                     palpite VARCHAR(255) NOT NULL
                   )"])
  )
(create-saldo-table)
(create-apostas-table)

(defn obter-saldo []
  (let [result (jdbc/query db-spec ["SELECT valor FROM saldo LIMIT 1"])]
    (:valor (first result))))

(defn atualizar-saldo [quantidade]
  (jdbc/execute! db-spec ["UPDATE saldo SET valor = valor + ?" quantidade]))

(defn inserir-aposta [event-id quantidade tipo palpite]
  (let [query "INSERT INTO apostas (event_id, quantidade, tipo, palpite) VALUES (?, ?, ?, ?)"]
    (jdbc/execute! db-spec [query event-id quantidade tipo palpite])))

