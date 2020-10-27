(ns pouchdb-fx-examples.events
  (:require
   [re-frame.core :as re-frame]
   [com.stronganchortech.pouchdb-fx :as pouchdb-fx]
   [pouchdb-fx-examples.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   ))

(defonce setup-watcher
  (do
    (println "setting up the watcher")
    (re-frame/dispatch
     [:pouchdb
      {:method :attach-change-watcher!
       :db "example"
       :options {:live true}
       :handler (fn [v]
                  (println "Calling :load-from-pouch from the change watcher.")
                  (re-frame/dispatch [:load-from-pouch]))}])
    ;; You can also call this directly from the library:
    ;; (pouchdb-fx/attach-change-watcher!
    ;;  "example"
    ;;  {:live true}
    ;;  (fn [v]
    ;;    (println "Calling :load-from-pouch from the change watcher.")
    ;;    (re-frame/dispatch [:load-from-pouch])
    ;;    ))
    ))

(re-frame/reg-event-fx
 :load-from-pouch
 (fn [{:keys [db pouchdb-docs]} [_ add-sync?]]
   (println "Handling :load-from-pouch")
   {:pouchdb
    {:db "example"
     :method :all-docs
     :options {:include_docs true}
     :success
     (fn [v]
       (re-frame/dispatch [:pouchdb-alldocs-success (js->clj v :keywordize-keys true)]) ;; TODO the keywordize keys might not be necessary anymore
       )}}))

(re-frame/reg-event-fx
 :pouchdb-alldocs-success
 (fn [{:keys [db]} [_  all-docs]]
   (let [docs (mapv :doc (:rows all-docs))]
     {:db (assoc db :docs docs)})))

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))
