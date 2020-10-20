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

(defn record-audio [e]
  (println "TODO record audio")
  )

(defn put-attachment-demo []
  (let [foo "bar"
       ;stream (.createMediaStreamDestination (js/AudioContext.))
        user-media (js/navigator.mediaDevices.getUserMedia #js {:audio true})
                                        ;       recorder (js/MediaRecorder. stream)
        recorder-atom (atom nil)
        chunks-atom (atom #js [])
        blob-atom (reagent/atom nil)
        ]
                                        ;(set! (.onsuccess user-media))
    (.then user-media (fn [stream]
                        (println "Got the stream: " stream)
                        (let [recorder (js/MediaRecorder. stream)]
                          (println "Got the recorder: " recorder)
                          (set! (.-ondataavailable recorder)
                                (fn [e]
                                  ;; (swap! chunks-atom
                                  ;;        (fn [chunks]
                                  ;;          (println "(type chunks)" (type chunks))
                                  ;;          (println "chunks before: " chunks)
                                  ;;          (println "what we're doing: " (.push chunks (.-data e)))
                                  ;;          (.push chunks (.-data e))))
                                  ;; (println "chunks after: " @chunks-atom)
                                  (reset! blob-atom (.-data e))
                                  (println "Got data!" (.-data e))))
                          (set! (.-onstop recorder)
                                (fn [e]
                                  (let [chunks @chunks-atom
                                        blob (js/Blob. (array chunks) #js{"type" "audio/ogg; codecs=opus"})]
                           ;         (reset! blob-atom blob)
                                    )))
                          (reset! recorder-atom recorder))))
    (fn []
      [:div
                                        ;       [:p (str stream)]
       [:p (str "Blob atom: ") blob-atom]
       (when-let [blob @blob-atom]
         (println "blob for audio element: " blob)
         [:audio {:src (js/window.URL.createObjectURL blob)
                  :controls true}])
       [:button {:on-click (fn [e]
                             (when-let [recorder @recorder-atom]
                               (.start recorder)
                               (println "started recording")))}
        "Record audio"]
       [:button {:on-click (fn [e]
                             (when-let [recorder @recorder-atom]
                               (.stop recorder)
                               (println "stopped recording")))}
        "Stop recording"
        ]])))

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
   [put-attachment-demo]
   [list-docs]])
