(ns store-sample.core-test
  (:use     (store-sample     core))
  (:require (clojure          [pprint  :refer :all]
                              [test    :refer :all])
            (clojure.java     [jdbc    :as    jdbc])
            (clojure.tools    [logging :as    log])
            (clj-time         [core    :as    time]
                              [coerce  :as    time.coerce])
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

                            (assoc-in [:categories (row-key 10)] {:name "Category #10", :deprecated? false, :superior-category-key nil})
                            (assoc-in [:categories (row-key 11)] {:name "Category #11", :deprecated? false, :superior-category-key (row-key 10)})
                            (assoc-in [:categories (row-key 12)] {:name "Category #12", :deprecated? false, :superior-category-key (row-key 11)})
                            (assoc-in [:categories (row-key 13)] {:name "Category #13", :deprecated? false, :superior-category-key (row-key 11)})
                            (assoc-in [:categories (row-key 14)] {:name "Category #14", :deprecated? true,  :superior-category-key (row-key 10)})
                            
                            (assoc-in [:products   (row-key 20)] {:code "000-0020", :name "Product #20", :price 1020.20M, :release-date (time.coerce/from-date #inst "2015-01-20T00:00:00+09:00"), :node "Note #20", :category-key (row-key 10)})
                            (assoc-in [:products   (row-key 21)] {:code "000-0021", :name "Product #21", :price 1021.21M, :release-date (time.coerce/from-date #inst "2015-01-21T00:00:00+09:00"), :node "Note #21", :category-key (row-key 10)})
                            (assoc-in [:products   (row-key 22)] {:code "000-0022", :name "Product #22", :price 1022.22M, :release-date (time.coerce/from-date #inst "2015-01-22T00:00:00+09:00"), :node "Note #22", :category-key (row-key 10)})
                            (assoc-in [:products   (row-key 23)] {:code "000-0023", :name "Product #23", :price 1023.23M, :release-date (time.coerce/from-date #inst "2015-01-23T00:00:00+09:00"), :node "Note #23", :category-key (row-key 11)})
                            (assoc-in [:products   (row-key 24)] {:code "000-0024", :name "Product #24", :price 1024.24M, :release-date (time.coerce/from-date #inst "2015-01-24T00:00:00+09:00"), :node "Note #24", :category-key (row-key 12)})

                            (#(reduce (fn [database number]
                                        (assoc-in database [:products (row-key number)] {:code (format "999-%d" number), :name (format "Product %d" number), :price number, :release-date (time.coerce/from-date #inst "2015-02-01T00:00:00+09:00"), :note (format "Note %d" number), :category-key (row-key 14)}))
                                      %
                                      (range 1000 2000)))

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
