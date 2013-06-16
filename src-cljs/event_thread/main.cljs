(ns event-thread.main
  (:use [jayq.core :only [$ on]]))

(def body
  ($ :body))

(def events (atom []))

(defn summarise [event]
  {:type   (aget event "type")
   :time   (aget event "timeStamp")
   :target (aget event "target")})

(add-watch events
  :watch-change
    (fn [key events old-val new-val]
      (js/console.log (clj->js new-val))))

(defn append-to-event-log [event]
  (swap! events conj (summarise event))
  false)

(on body "change" append-to-event-log)

(on body "submit" append-to-event-log)

(defprotocol Aseq
  (afirst [coll callback])
  (arest  [coll callback])
  (acons  [item seq]))

(def aseq identity)

; A double click is created if two clicks happen within 50ms of each other. If
; another click happens within 50ms of the last click of a double click that
; does not trigger a second double click.
(defn within-double-click-time [click previous-click]
  (if-let [previous-time (:time previous-click)]
    (> 50 (- (:time click) previous-time))
    false))

(defn identify-double-click [state click]
  (if (within-double-click-time click (state :last-click))
    (-> state
        (update-in [:double-clicks] #(conj % click))
        (assoc :last-click nil))
    (assoc state :last-click click)))

(defn test [expected actual]
  (if (not (= expected actual))
    (js/console.log expected actual)
    (js/console.log "Pass")))

(test {:double-clicks [{:time 40}]
       :last-click nil}
      (identify-double-click {:double-clicks []
                              :last-click {:time 1}}
                             {:time 40}))

(test {:double-clicks []
       :last-click {:time 40}}
      (identify-double-click {:double-clicks []
                              :last-click nil}
                             {:time 40}))

(def clicks (aseq [{:time 0} {:time 100} {:time 101} {:time 160} {:time 200}]))

(def double-clicks
  (:double-clicks (reduce identify-double-click {:double-clicks []} clicks)))

(test [{:time 101} {:time 200}] double-clicks)

