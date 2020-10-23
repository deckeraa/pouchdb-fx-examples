(ns pouchdb-fx-examples.views
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [pouchdb-fx-examples.subs :as subs]
   [com.stronganchortech.pouchdb-fx :as pouchdb-fx]
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
                               :doc {:type "note" :text @text}
                               }])}
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
  (let [user-media (js/navigator.mediaDevices.getUserMedia #js {:audio true})
        recorder-atom (atom nil)
        chunks-atom (atom [])
        blob-atom (reagent/atom nil)
        ]
    (.then user-media (fn [stream]
                        (println "Got the stream: " stream)
                        (let [recorder (js/MediaRecorder. stream)]
                          (println "Got the recorder: " recorder)
                          (set! (.-ondataavailable recorder)
                                (fn [e]
                                  (swap! chunks-atom conj (.-data e)) ;; add in the new data as it comes in.
                                  (println "Got data!" (.-data e))))
                          (set! (.-onstop recorder)
                                (fn [e]
                                  (let [chunks @chunks-atom
                                        ;; join all the chunks together into one big blob
                                        blob (js/Blob. chunks #js{"type" "audio/ogg; codecs=opus"})]
                                    (reset! blob-atom blob))))
                          (reset! recorder-atom recorder))))
    (fn []
      (let [docs (re-frame/subscribe [::subs/docs])
            doc  (or (first (filter #(= (:_id %) "doc-with-attachment") @docs))
                     {:_id "doc-with-attachment"})]
        [:div
         [:p (str "chunks-atom: ") chunks-atom]
         [:button {:on-click (fn [e]
                               (when-let [recorder @recorder-atom]
                                 (reset! chunks-atom []) ;; clear out previous chunks
                                 (.start recorder)
                                 (println "started recording")))}
          "Record audio"]
         [:button {:on-click (fn [e]
                               (when-let [recorder @recorder-atom]
                                 (.stop recorder)
                                 (println "stopped recording")))}
          "Stop recording"]
         (when-let [blob @blob-atom]
           (println "blob for audio element: " blob)
           [:div
            [:audio {:src (js/window.URL.createObjectURL blob)
                     :controls true}]
            [:button {:on-click
                      #(re-frame/dispatch
                        [:pouchdb {:db "example"
                                   :method :put-attachment
                                   :doc doc
                                   :attachment-id "sound.ogg"
                                   :attachment blob
                                   :attachment-type "audio/ogg"
                                   :success (fn [e] (println ":put-attachment success" e))
                                   :failure (fn [e] (println ":put-attachment failure" e))}])}
             "Save recording as an attachment"]])
         ]))))

(defn list-docs []
  (let [docs (re-frame/subscribe [::subs/docs])]
    [:div
     [:h3 "All documents in the example database"]
     (if (empty? @docs)
       [:p "Database is empty."]
       [:ul (map (fn [doc]
                   ^{:key (:_id doc)}
                   [:li
                    (str doc)
                    [:button {:on-click (fn [e]
                                          (re-frame/dispatch
                                           [:pouchdb
                                            {:db "example"
                                             :method :remove
                                             :doc doc
                                             :failure #(fn [e] (println "Failed to delete: " e))}]))}
                     "Delete"]])
                 @docs)])]))

(defn attachment-player []
  (let [blob-atom (reagent/atom nil)
        read-failed (reagent/atom nil)]
    (fn []
      (let [docs (re-frame/subscribe [::subs/docs])
            doc  (or (first (filter #(= (:_id %) "doc-with-attachment") @docs))
                     {:_id "doc-with-attachment"})]
        [:div
         [:h3 "This will play the audio attached to the doc with id doc-with-attachment"]
         (when-let [blob @blob-atom]
           [:audio {:src (js/window.URL.createObjectURL blob)
                    :controls true}])
         [:button {:on-click
                   (fn []
                     (reset! read-failed nil)
                     (re-frame/dispatch
                      [:pouchdb {:db "example"
                                 :method :get-attachment
                                 :doc doc
                                 :attachment-id "sound.ogg"
                                 :success (fn [attachment]
                                            (println "Got x: " attachment)
                                            (reset! blob-atom attachment))
                                 :failure #(reset! read-failed %)}]))} "Get attachment"]
         (when-let [fail-msg @read-failed]
           [:p {} "Couldn't read the attachment " (str fail-msg)])
         [:button {:on-click #(re-frame/dispatch
                               [:pouchdb {:db "example"
                                          :method :remove-attachment
                                          :doc doc
                                          :attachment-id "sound.ogg"}])}
          "Remove attachment."]
         ]))))

(defn sync-login []
  (let [username (reagent/atom "")
        password (reagent/atom "")]
    (fn []
      [:div {}
       [:h3 {} "Configure syncing"]
       [:div
        [:label {:class "ma1"} "Username: "]
        [:input {:type :text :value @username
                 :on-change #(reset! username (-> % .-target .-value))}]]
       [:div
        [:label {:class "ma1"} "Password: "]
        [:input {:type :password :value @password
                 :on-change #(reset! password (-> % .-target .-value))}]]
       [:button {:on-click
                 (fn [e]
                   (re-frame/dispatch
                    [:pouchdb
                     {:db "example"
                      :method :sync!
                      :target (str "http://" @username ":" @password "@localhost:5984/example")
                      :options {:live true}
                      :handlers
                      {:change #(re-frame/dispatch [:load-from-pouch])
                       :complete #(re-frame/dispatch [:load-from-pouch])}
                      }]
                    ))
                 ;;;; You can call the library directly if you would like
                 ;; (fn [e]
                 ;;   (pouchdb-fx/sync!
                 ;;    "example" (str "http://" @username ":" @password "@localhost:5984/example")
                 ;;    {:live true}
                 ;;    {:change (fn [v]
                 ;;               (println "change: " v)
                 ;;               (re-frame/dispatch [:load-from-pouch]))
                 ;;     :complete (fn [v]
                 ;;                 (println "complete: " v)
                 ;;                 (re-frame/dispatch [:load-from-pouch]))
                 ;;     :error  #(println "error: " %)}))
                 }
        "Sync"]
       [:button {:on-click #(re-frame/dispatch [:pouchdb {:method :cancel-sync!
                                                          :db "example"}])}
        "Cancel Sync"]
       [:button {:on-click (fn [] (println (keys (pouchdb-fx/db-obj "example"))))}
        "db-obj"]])))

(defn test-replication []
  (let [outbound? (reagent/atom true)
        username (reagent/atom "")
        password (reagent/atom "")]
    (fn []
      [:div
       [:h3 "Test one-shot replication"]
       [:div
        [:label {:class "ma1"} "Username: "]
        [:input {:type :text :value @username
                 :on-change #(reset! username (-> % .-target .-value))}]]
       [:div
        [:label {:class "ma1"} "Password: "]
        [:input {:type :password :value @password
                 :on-change #(reset! password (-> % .-target .-value))}]]
       [:p "Replication: " (if @outbound? "outbound" "inbound")]
       [:label "Remote url: "]
       ;; [:input {:type :text :value @remote :on-change #(reset! remote (-> % .-target .-value))}]
       [:button {:on-click (fn [e]
                             (swap! outbound? (fn [x] (if x false true))))}
        "Toggle replication direction."]
       [:button {:on-click #(re-frame/dispatch [:pouchdb
                                                {:db "example"
                                                 :method :replicate
                                                 :target (str "http://" @username ":" @password "@localhost:5984/example")
                                                 :outbound? @outbound?
                                                 :handlers {:change (fn [x] (println "change: " x))}}])}
        "Call replicate()"]])))

(defn document-getter []
  (let [returned-doc (reagent/atom nil)
        id (reagent/atom nil)]
    (fn []
      [:div
       [:h3 "Document Getter"]
       [:label "ID: "]
       [:input {:type :text :value @id :on-change #(reset! id (-> % .-target .-value))}]
       [:button {:on-click (fn []
                             (re-frame/dispatch
                              [:pouchdb
                               {:db "example"
                                :method :get
                                :doc-id @id
                                :options {:conflicts true
                                          :revs true}
                                :success #(reset! returned-doc %)
                                }]))}
        "Get the document"]
       [:p "Returned doc: " @returned-doc]])))

(defn test-various-db-ops []
  [:div
   [:h3 "Testing various DB ops"]
   [:button {:on-click (fn [] (re-frame/dispatch
                               [:pouchdb
                                {:db "example"
                                 :method :close
                                 :success #(println "Closed the database")}]))} "close"]
   [:button {:on-click (fn [] (re-frame/dispatch
                               [:pouchdb
                                {:db "example"
                                 :method :info
                                 :success #(println "DB info: " %)}]))} "info"]
   [:button {:on-click (fn [] (re-frame/dispatch
                               [:pouchdb
                                {:db "example"
                                 :method :compact
                                 :success #(println "Compaction succeeded: " %)
                                 :failure #(println "Compaction failed: " %)}]))} "compact"]])

(defn test-doc-revs-diff []
  (let [doc-id (reagent/atom "")
        rev-1  (reagent/atom "")
        rev-2  (reagent/atom "")
        result (reagent/atom nil)]
    (fn []
      [:div
       [:h3 "Testing revsDiff"]
       [:ul {:style {:list-style :none}}
        [:li
         [:label "doc-id"]
         [:input {:type :text :style {:width "30em"} :value @doc-id :on-change #(reset! doc-id (-> % .-target .-value))}]]
        [:li
         [:label "rev-1"]
         [:input {:type :text :style {:width "30em"} :value @rev-1 :on-change #(reset! rev-1 (-> % .-target .-value))}]]
        [:li
         [:label "rev-2"]
         [:input {:type :text :style {:width "30em"} :value @rev-2 :on-change #(reset! rev-2 (-> % .-target .-value))}]]]
       [:button {:on-click (fn [] (re-frame/dispatch
                                   [:pouchdb
                                    {:db "example"
                                     :method :revsDiff
                                     :diff {@doc-id [@rev-1 @rev-2]}
                                     :success #(reset! result %)}]))}
        "Run revsDiff"]
       [:p (str "Results: " @result)]])))

(defn test-bulk-get []
  (let [result (reagent/atom nil)]
    (fn []
      [:div
       [:h3 "Testing bulkGet"]
       [:button {:on-click (fn [] (re-frame/dispatch
                                   [:pouchdb
                                    {:db "example"
                                     :method :bulkGet
                                     :options {:docs [{:id "poem-1"} {:id "doc-with-attachment"}]
                                               :binary true}
                                     :success #(reset! result %)
                                     }]))}
        "bulkGet"]
       [:p {} (str "Result: " @result)]])))

(defn main-panel []
  [:div
   [create-note]
   [demo-put]
   [put-attachment-demo]
   [attachment-player]
   [sync-login]
   [test-replication]
   [document-getter]
   [test-various-db-ops]
   [test-doc-revs-diff]
   [test-bulk-get]
   [list-docs]])
