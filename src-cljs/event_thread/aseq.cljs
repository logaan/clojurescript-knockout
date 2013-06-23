(ns event-thread.aseq
  (:use [event-thread.test :only [test]]
        [jayq.util :only [log]])
  (:require [jayq.core :as jq]))

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
;
; I guess the structure of a future wrapped around a cell would actually be
; simpler than the map with two keys. It would mean that the value returned
; from rest is actually a 'collection'.

(defn acell
  ([] (acell (jq/$deferred) (jq/$deferred)))
  ([first rest] {:first first :rest rest :empty? false}))

(defn empty-acell []
  {:empty? true})

(def empty? :empty?)

(defn deferred [value]
  (jq/resolve (jq/$deferred) value))

(defn afirst [aseq]
  (:first aseq))

(defn arest [aseq]
  (:rest aseq))

(defn acons [value coll]
  (acell value (deferred coll)))

(defn aseq [values]
  (reduce (fn [coll v] (acons v coll))
          (empty-acell)
          values))

(defn map [f coll]
  (let [new-first (jq/$deferred)
        new-rest  (jq/$deferred)]
    (jq/done (afirst coll)
             (fn [head]
               (jq/done (f head) (fn [result]
                                   (jq/resolve new-first result)))))
    (jq/done (arest coll)
             (fn [tail]
               (if (empty? tail)
                        (jq/resolve new-rest (empty-acell))
                        (jq/resolve new-rest (map f tail)))))
    (acell new-first new-rest)))

(defn mapd [f coll]
  (map (comp deferred f) coll))

; (let [first-event     (jq/$deferred)
;       second-event    (jq/$deferred)
;       events          (aseq [first-event second-event])
;       raw-log         (mapd log events)
;       squared-events  (mapd #(* % %) events)
;       squared-log     (mapd log squared-events)
;       plussed-events  (mapd #(+ 10 %) squared-events)
;       plussed-log     (mapd log plussed-events)]
;   (jq/resolve first-event  3)
;   (jq/resolve second-event 5))

; Should output:
;   3
;   9
;   19
;   5
;   25
;   35

(defn producer []
  (atom (acell)))

(defn produce [producer value]
  (let [new-cell (acell)
        old-cell (deref producer)
        old-first (afirst old-cell)
        old-rest  (arest  old-cell)]
    (jq/resolve old-first value)
    (jq/resolve old-rest  new-cell)
    (reset! producer new-cell)))

; (let [writer        (producer)
;       reader        (deref writer)
;       logged-events (mapd log reader)]
;   (produce writer 1)
;   (produce writer 2))

; Should produce:
; 1
; 2

; (let [writer          (producer)
;       events          (deref writer)
;       raw-log         (mapd log  events)
;       squared-events  (mapd #(* % %)  events)
;       squared-log     (mapd log  squared-events)
;       plussed-events  (mapd (partial + 10) squared-events)
;       plussed-log     (mapd log  plussed-events)]
;   (produce writer 3)
;   (produce writer 5))

; Should produce:
; 3
; 9
; 19
; 5
; 25
; 35

(defn merge [as1 as2]
  (let [writer        (producer)
        output-seq    (deref writer)
        add-to-writer (partial produce writer)]
    (mapd add-to-writer as1)
    (mapd add-to-writer as2)
    output-seq))

; (let [writer1    (producer)
;       input1     (deref writer1)
;       writer2    (producer)
;       input2     (deref writer2)
;       merged     (merge input1 input2)
;       multiplied (mapd (partial * 2) merged)
;       logged     (mapd log multiplied)]
;   (produce writer1 1)
;   (produce writer2 2)
;   (produce writer1 1)
;   (produce writer2 2))

; Should produce:
; 2
; 4
; 2
; 4

; It might be worth just having producer return a pair of writer and reader.
; The only reasonable usage seems to be to grab a writer and deref immediately.


; This logic is largely duplicated from map. Map should probably just use this
; but ignore the seed value.
(defn reductions [f seed coll]
  (let [new-first (jq/$deferred)
        new-rest  (jq/$deferred)]
  ; call f with the seed value, and call reductions on the tail with the return as the seed.
  (jq/done (afirst coll)
           (fn [head]
             (jq/done (f seed head)
                      (fn [result]
                        (jq/resolve new-first result)
                        (jq/done (arest coll)
                                 (fn [tail]
                                   (if (empty? tail)
                                    (jq/resolve new-rest (empty-acell))
                                    (jq/resolve new-rest (reductions f result tail)))))))))
    (acell new-first new-rest)))

; (let [writer      (producer)
;       reader      (deref writer)
;       running-sum (reductions (comp deferred +) 0 reader)
;       logged      (mapd log running-sum)]
;   (produce writer 1)
;   (produce writer 2)
;   (produce writer 3)
;   (produce writer 4))

; Should produce:
; 1
; 3
; 6
; 10

(defn reduce
  ([f seed coll]
   (let [output (jq/$deferred)]
     (reduce output f seed coll)
     output))
  ([output f seed coll]
    (jq/done (afirst coll) (fn [head]
      (jq/done (f seed head) (fn [result]
        (jq/done (arest coll) (fn [tail]
          (if (empty? tail)
            (jq/resolve output result)
            (reduce output f result tail))))))))))

; (->> [1 2 3 4]
;      (cljs.core/map deferred)
;      (aseq)
;      (reduce (comp deferred +) 0)
;      ((fn [reduce-deferred] (jq/done reduce-deferred log))))
;
; Should produce:
; 10

