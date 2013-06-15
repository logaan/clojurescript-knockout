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

