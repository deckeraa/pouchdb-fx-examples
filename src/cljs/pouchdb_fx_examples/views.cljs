(ns pouchdb-fx-examples.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [pouchdb-fx-examples.subs :as subs]
   ))

;; (defn create-note []
;;   (let [questions (re-frame/subscribe [::subs/questions])]
;;     [:div "Past questions:"
;;      [:ul (map (fn [x] ^{:key (:_id x)} [:li (str x)]) @questions)]]))

(defn create-note []
  (let [text (reagent/atom "")]
    (fn []
      [:div
       [:h3 "Create a document using post"]
       [:input {:type :text :value @text
                :on-change #(reset! text (-> % .-target .-value))}]
       [:button {:on-click #(re-frame/dispatch
                             [:pouchdb
                              {:db "example"
                               :method :post
                               :doc {:type "note" :text @text}}])}
        "Create document"]])))

(defn demo-put []
  (let [text (reagent/atom "")]
    (fn []
      (let [docs (re-frame/subscribe [::subs/docs])
            doc  (or (first (filter #(= (:_id %) "poem-1") @docs))
                     {:_id "poem-1"})]
        (swap! text (fn [v]
                      (if (and (:text doc) (= v ""))
                        (:text doc)
                        v)))
        [:div
         [:h3 "Create/update a document with a known ID using put"]
         [:textarea {:rows 5
                     :columns 80
                     :style {:width "300px"}
                     :value @text
                     :on-change #(reset! text (-> % .-target .-value))}]
         [:button {:on-click #(re-frame/dispatch
                               [:pouchdb
                                {:db "example"
                                 :method :post
                                 :doc (assoc doc :text @text)}])}
          "Create/update document"]
         [:p doc]]))))

(defn list-docs []
  (let [docs (re-frame/subscribe [::subs/docs])]
    [:div
     [:h3 "All documents in the example database"]
     (if (empty? @docs)
       [:p "Database is empty."]
       [:ul (map (fn [x] ^{:key (:_id x)} [:li (str x)]) @docs)])]))

(defn main-panel []
  [:div
   [create-note]
   [demo-put]
   [list-docs]])
