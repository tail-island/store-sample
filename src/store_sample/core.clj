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

;; Add message resource.
(dog-mission/conj-resource-bundle-namespace "store-sample.message")

(def database-schema
  {:products {:columns      (array-map :code     {:type :string}
                                       :name     {:type :string}
                                       :price    {:type :decimal})
              :validations  {:presence [[:code]
                                        [:name]
                                        [:price]]}
              :placeholders {:code     "123-4567"}
              }})

(def database-spec
  {:subprotocol "postgresql"
   :subname     "store-sample"
   :user        "store-sample"
   :password    "P@ssw0rd"})

(sweet-crossplane/initialize database-schema database-spec "store sample" sweet-crossplane/default-layout)

(defroutes app-routes
  (GET       "/" [] (sweet-crossplane/default-layout "sample store" [:div.jumbotron
                                                                     [:h1 "Welcome to the store sample!"]
                                                                     [:p  "This is a sweet-crossplane sample site."]]))
  (resources "/")
  (sweet-crossplane/process-request))

(def app-handler
  (-> app-routes
      (sweet-crossplane/wrap-ring-request)
      (sweet-crossplane/wrap-time-zone)
      (sweet-crossplane/wrap-locale {:locales #{(Locale. "en") (Locale. "en" "US") (Locale. "ja")}})
      (wrap-base-url)
      (wrap-cookies)
      (wrap-keyword-params)
      (wrap-params)))