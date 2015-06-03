(ns store-sample.core
  (:require (clj-time         [core             :as    time])
            (compojure        [core             :refer :all]
                              [route            :refer :all])
            (hiccup           [middleware       :refer :all])
            (ring.middleware  [cookies          :refer :all]
                              [keyword-params   :refer :all]
                              [params           :refer :all])
            (dog-mission      [core             :as    dog-mission])
            (sweet-crossplane [core             :as    sweet-crossplane])
            )
  (:import  (java.util        Locale)))

;; Before starting this application, please create PostgreSQL user and database by bellow commands.
;; ----
;; $ sudo -u postgres psql
;; postgres=# CREATE USER "store-sample" WITH PASSWORD 'P@ssw0rd';
;; postgres=# CREATE DATABASE "store-sample" WITH OWNER "store-sample";
;; postgres=# \q
;; ----
;;
;; And do testing.
;; ---
;; $ lein test
;; ---

;; Add message resource.
(dog-mission/conj-resource-bundle-namespace "store-sample.message")

(def database-schema
  (array-map :categories {:columns                   (array-map :name                {:type      :string}
                                                                :deprecated?         {:type      :boolean})
                          :many-to-one-relationships (array-map :superior-category   {:table-key :categories})
                          :one-to-many-relationships (array-map :inferior-categories {:table-key :categories, :many-to-one-relationship-key :superior-category}
                                                                :products            {:table-key :products,   :many-to-one-relationship-key :category})
                          
                          :validations  {:presense [[:name]]}}
             
             :products   {:columns                   (array-map :code                {:type      :string,     :constraint "UNIQUE"}
                                                                :name                {:type      :string}
                                                                :price               {:type      :decimal     :scale 2}
                                                                :release-date        {:type      :date}
                                                                :note                {:type      :text})
                          :many-to-one-relationships (array-map :category            {:table-key :categories})
                          :one-to-many-relationships (array-map)
                          
                          :representative         :code
                          :placeholders           {:code       "123-4567"}
                          :search-condition       {:properties [:code :category :name :price :release-date]}
                          :list                   {:properties [:code :name :price :category :release-date]
                                                   :sort-by    [:name (comp (partial * -1) compare)]}
                          :input                  {:properties [:category :code :name :price :release-date :note]}
                          :validations            {:presence   [[:code] [:name] [:price] [:release-date] [:category]]
                                                   :format     [[:code :code #"\A\d{3}-\d{4}\z" "please input %1$s like 123-4567."]]}
                          :before-validate-fn     (fn [database entity-class-key entity-key]
                                                    (update-in database [entity-class-key entity-key :release-date] #(or % (-> (time/now) (time/to-time-zone dog-mission/*joda-time-zone*) (.withMillisOfDay 0) (time/to-time-zone time/utc)))))
                          :sql-exception-catch-fn (fn [sql-exception]
                                                    (if (= (.getSQLState sql-exception) "23505")
                                                      {:code ["Code must be unique."]}))
                          }
             ))

(def database-spec
  {:subprotocol "postgresql"
   :subname     "store-sample"
   :user        "store-sample"
   :password    "P@ssw0rd"})

(sweet-crossplane/initialize database-schema database-spec "store sample" sweet-crossplane/default-layout-fn)

(defroutes app-routes
  (GET       "/" [] (sweet-crossplane/layout "sample store" [:div.jumbotron
                                                             [:h1 "Welcome to the store sample!"]
                                                             [:p  "This is a sweet-crossplane sample site."]]))
  (resources "/")
  (sweet-crossplane/process-request))

(def app-handler
  (-> app-routes
      (sweet-crossplane/wrap-ring-request)
      (sweet-crossplane/wrap-entity-params)
      (sweet-crossplane/wrap-locale {:locales #{(Locale. "en") (Locale. "en" "US") (Locale. "ja")}})
      (sweet-crossplane/wrap-time-zone)
      (sweet-crossplane/wrap-http-header-cache-control)
      (wrap-base-url)
      (wrap-cookies)
      (wrap-keyword-params)
      (wrap-params)))
