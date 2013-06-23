(ns event-thread.dseq
  (:use [event-thread.test :only [test]])
  (:require [event-thread.dcell :as dc]
            [jayq.core :as jq]))

(defn dseq [& values]
  (reduce (fn [coll v] (dc/cons v coll)) (dc/dcell) values))

; (dc/first (dc/rest (dseq 1 2 3)))
    
