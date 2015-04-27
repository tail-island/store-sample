(ns store-sample.core-test
  (:use     (store-sample     core))
  (:require (clojure          [pprint  :refer :all]
                              [test    :refer :all])
            (clojure.java     [jdbc    :as    jdbc])
            (clojure.tools    [logging :as    log])
            (clj-time         [core    :as    time])
            (twin-spar        [core    :as    twin-spar :refer [$=]])))

(def ^:private database-data'
  (partial twin-spar/database-data database-schema))

(def ^:private database'
  (partial twin-spar/database      database-schema))

(def ^:private save!'
  (partial twin-spar/save!         database-schema))

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

                            (assoc-in [:categories (row-key 10)] {:name "Category #10", :superior-category-key nil})
                            (assoc-in [:categories (row-key 11)] {:name "Category #11", :superior-category-key (row-key 10)})
                            (assoc-in [:categories (row-key 12)] {:name "Category #12", :superior-category-key (row-key 11)})
                            (assoc-in [:categories (row-key 13)] {:name "Category #13", :superior-category-key (row-key 11)})
                            (assoc-in [:categories (row-key 14)] {:name "Category #14", :superior-category-key (row-key 10)})
                            
                            (assoc-in [:products   (row-key 20)] {:code "000-0020", :name "Product #20", :price 1020.20M, :category-key (row-key 10)})
                            (assoc-in [:products   (row-key 21)] {:code "000-0021", :name "Product #21", :price 1021.21M, :category-key (row-key 10)})
                            (assoc-in [:products   (row-key 22)] {:code "000-0022", :name "Product #22", :price 1022.22M, :category-key (row-key 10)})
                            (assoc-in [:products   (row-key 23)] {:code "000-0023", :name "Product #23", :price 1023.23M, :category-key (row-key 11)})
                            (assoc-in [:products   (row-key 24)] {:code "000-0024", :name "Product #24", :price 1024.24M, :category-key (row-key 12)})

                            (save!' transaction)))
                      (test-function)))

(deftest test-category
  (jdbc/with-db-transaction [transaction database-spec]
    (let [database (database' (database-data' transaction :categories ($= :name "Category #11")))]
      (is (= "Category #11" (get-in database [:categories (row-key 11) :name])))
      (is (= "Category #10" (get-in database [:categories (row-key 11) :superior-category :name])))
      (is (= ["Category #12" "Category #13"]
             (sort (map :name (get-in database [:categories (row-key 11) :inferior-categories])))))
      )))
