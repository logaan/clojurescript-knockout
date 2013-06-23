(ns event-thread.dseq
  (:use [event-thread.test :only [test]]
        [jayq.util :only [log]])
  (:require [event-thread.dcell :as dc]
            [jayq.core :as jq]))

(defn dseq [& values]
  (reduce (fn [coll v] (dc/cons v coll)) (dc/dcell) values))

(jq/done (dc/first (dc/rest (dseq 1 2 3))) (fn [f]
  (test 2 f)))
    
