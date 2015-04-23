(defproject store-sample "0.1.0-SNAPSHOT"
  :description  "store-sample: A store sample application for sweet-crossplane."
  :url          "http://github.com/tail-island/store-sample"
  :license      {:name "Eclipse Public License"
                 :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure              "1.6.0"]
                 [compojure                        "1.3.3"]
                 [hiccup                           "1.0.5"]
                 [com.tail-island/sweet-crossplane "0.1.0"]]
  :ring         {:handler store-sample.core/app-handler}
  :profiles     {:dev {:plugins [[lein-ring "0.9.3"]]}})
