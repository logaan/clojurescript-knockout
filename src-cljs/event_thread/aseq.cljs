(ns event-thread.aseq
  (:use [event-thread.test :only [test]]
        [jayq.util :only [log]])
  (:require [jayq.core :as jq]))

; Async cell
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

; Async list
(defn acons [value coll]
  (acell value (deferred coll)))

(defn aseq [values]
  (cljs.core/reduce (fn [coll v] (acons v coll)) (empty-acell) values))

; It might be worth just having producer return a pair of writer and reader.
; The only reasonable usage seems to be to grab a writer and deref immediately.
(defn producer []
  (atom (acell)))

(defn produce [producer value]
  (let [new-cell (acell)
        old-cell  (deref producer)
        old-first (afirst old-cell)
        old-rest  (arest  old-cell)]
    (jq/resolve old-first value)
    (jq/resolve old-rest  new-cell)
    (reset! producer new-cell)))

; Higher order functions
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

(defn reductions [f seed coll]
  (let [writer    (producer)
        reader    (deref writer)
        emit-seed (fn [s v]
                    (let [result-def (f s v)]
                      (jq/done result-def #(produce writer %)
                      result-def)))]
    (produce writer seed)
    (reduce emit-seed seed coll)
    reader))

(defn map [f coll]
  (let [writer (producer)
        reader (deref writer)]
    (reduce (fn [s v] (jq/done (f v) (partial produce writer))) nil coll)
    reader))

(defn mapd [f coll]
  (map (comp deferred f) coll))

(defn merge [as1 as2]
  (let [writer        (producer)
        output-seq    (deref writer)
        add-to-writer (partial produce writer)]
    (mapd add-to-writer as1)
    (mapd add-to-writer as2)
    output-seq))

