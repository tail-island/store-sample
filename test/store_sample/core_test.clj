(ns store-sample.core-test
  (:use     (store-sample     core))
  (:require (clojure          [pprint  :refer :all]
                              [test    :refer :all])
            (clojure.java     [jdbc    :as    jdbc])
            (clojure.tools    [logging :as    log])
            (clj-time         [core    :as    time])
            (twin-spar        [core    :as    twin-spar])))

;; Before testing, please create user and database by bellow commands.
;; ----
;; CREATE USER "store-sample" WITH PASSWORD 'P@ssw0rd';
;; CREATE DATABASE "store-sample" WITH OWNER "sweet-crossplane";
;; ----

(def ^:private database'
  (partial twin-spar/database database-schema))

(def ^:private save!'
  (partial twin-spar/save!    database-schema))

(def ^:private row-keys
  (repeatedly twin-spar/new-key))

(defn- row-key
  [index]
  (nth row-keys index))

(defn- prepare-tables
  []
  (try
    (twin-spar/drop-tables database-schema database-spec)
    (catch Exception ex
      ;; (log/error ex)
      ))
  (twin-spar/create-tables database-schema database-spec))

(use-fixtures :each (fn [test-function]
                      (prepare-tables)
                      (jdbc/with-db-transaction [transaction database-spec]
                        (-> (database')
                            (assoc-in [:products (row-key 10)] {:code "000-0010", :name "Product #10", :price 1010.00M})
                            (assoc-in [:products (row-key 11)] {:code "000-0011", :name "Product #11", :price 1011.00M})
                            (assoc-in [:products (row-key 12)] {:code "000-0012", :name "Product #12", :price 1012.00M})
                            (save!' transaction)))
                      (test-function)))

(deftest a-test
  (is (= 1 2)))
