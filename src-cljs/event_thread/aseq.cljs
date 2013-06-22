(ns event-thread.aseq
  (:use [event-thread.test :only [test]]
        [jayq.util :only [log]])
  (:require [jayq.core :as jq]))

; (defprotocol Aseq
;   (afirst [coll])
;   (arest  [coll])
;   (acons  [coll value]))
; 
; (defrecord ACell [first rest]
;   Aseq
;   (afirst [coll callback]
;     (callback (:first coll)))
;   (arest  [coll callback]
;     [coll callback])
;   (acons  [coll value]
;     (ACell. value coll)))
; 
; (defrecord EmptyACell []
;   Aseq
;   (afirst [coll callback]
;     (callback nil))
;   (arest [coll callback]
;     [coll callback])
;   (acons [coll value]
;     (ACell. coll value)))
; 
; (defn aseq [seed-values]
;   (reduce (fn [c v] (acons c v)) (EmptyACell.) seed-values))
; 
; (afirst (ACell. 1 2)
;         (fn [v] (test 1 v)))
; 
; (test (acons (acons (acons (EmptyACell.) 1) 2) 3) (aseq [1 2 3]))

; Create an empty aseq
; Have someone request first
; Deliver first
; Test that the requester gets the value

; So... what if it's just a future wrapped around a cons cell?
;
; Means you can:
;
; # Close the seq without producing any values
; 1. Create an aseq (future wrapping cons)
; 2. Have someone deref the future
; 3. You close the seq
; 4. The deref resolves
; 5. They call first and receive nil
;
; But can't comply with the seq interface because you can't call first on a
; future without passsing in a callback.
; 
; So instead we need a cons cell with a future for first and an automatically
; generated list with another future.
;
; The problem here is that if you map over it then it'll just keep going
; forever.
;
; So we have first and rest both return futures... but we don't need first to
; because by the time we've dereffed rest the thing that cause that to succeed
; was the fact that we've got a value. So we're back to future wrapped around
; cell again. And there's no way that it'll support seq because everything
; needs rest to return a collection.
;
; So... you could just have some onValue function that gets called every time
; we have a value. But Then you've got no way of referring to the rest of the
; values that you haven't consumed yet... But if we generate the 'rest of the
; values' list before we know whether it's the end of the list or not then
; we'll infinitely generate without any blocking. So we need a callback that
; will give us the rest of the list. But passing the callback as a param to
; rest is going to be a bit of a pain when we're going to monads. But when you
; start the list it might have no first value... so you could have the
; constructor return a future for a list.... you can wait for the list... call
; first and you get it straight away, no future... but if you call rest then
; you get another future wrapped around the rest of the list. This should solve
; the timing problems. But we could just implement first and rest for the
; future... they'll let you know when the future resolves and you could call
; first to get your value directly... but if you're going to do that any way
; then basically you just end up with first and rest both returning futures. So
; they'll have the same interface as ISeq but the resturn value of rest won't
; match up. So we won't be able to use the default map or anything but... maybe
; that's the best we can have.

(defn acell [first rest]
  {:first (or first jq/$deferred)
   :rest  (or rest  jq/$deferred)})

(defn deferred [value]
  (jq/resolve (jq/$deferred) value))

(def empty-acell
  {:first (jq/$deferred)
   :rest  (jq/$deferred)})

(defn afirst [aseq]
  (:first aseq))

(defn arest [aseq]
  (:rest aseq))

(defn acons [value coll]
  (acell value (deferred coll)))

(defn aseq [values]
  (reduce (fn [coll v] (acons v coll))
          empty-acell
          values))

(defn async-map [f coll]
  (let [new-first (jq/$deferred)
        new-rest  (jq/$deferred)]
    (jq/done (afirst coll)
             (fn [head] (jq/resolve new-first (f head))))
    (jq/done (arest coll)
             (fn [tail] (jq/resolve new-rest (async-map f tail))))
    (acell new-first new-rest)))

; (->> [1 2 3]
;      (map deferred)
;      aseq
;      (async-map (partial + 1))
;      (async-map log))

(let [first-event     (jq/$deferred)
      second-event    (jq/$deferred)
      events          (aseq [first-event second-event])
      raw-log         (async-map log events)
      squared-events  (async-map #(* % %) events)
      squared-log     (async-map log squared-events)]
  (jq/resolve first-event  3)
  (jq/resolve first-event  30)
  (jq/resolve second-event 5))

;(async-map log (async-map (fn [v] (+ 1 v)) (aseq (map deferred [1 2 3]))))

