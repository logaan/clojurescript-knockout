; A double click is created if two clicks happen within 50ms of each other. If
; another click happens within 50ms of the last click of a double click that
; does not trigger a second double click.
(ns event-thread.double-click
  (:use [event-thread.test :only [test]]))

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

; (test {:double-clicks [{:time 40}]
;        :last-click nil}
;       (identify-double-click {:double-clicks []
;                               :last-click {:time 1}}
;                              {:time 40}))
; 
; (test {:double-clicks []
;        :last-click {:time 40}}
;       (identify-double-click {:double-clicks []
;                               :last-click nil}
;                              {:time 40}))

(def clicks [{:time 0} {:time 100} {:time 101} {:time 160} {:time 200}])

(def double-clicks
  (:double-clicks (reduce identify-double-click {:double-clicks []} clicks)))

; (test [{:time 101} {:time 200}] double-clicks)
