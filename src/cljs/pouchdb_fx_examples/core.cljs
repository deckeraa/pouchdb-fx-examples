(ns pouchdb-fx-examples.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [pouchdb-fx-examples.events :as events]
   [pouchdb-fx-examples.views :as views]
   [pouchdb-fx-examples.config :as config]
   ))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch [:load-from-pouch])
  (dev-setup)
  (mount-root))
