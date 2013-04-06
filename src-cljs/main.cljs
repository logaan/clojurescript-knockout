(ns event-thread.main
  (:require [event-thread.knockout :as ko])
  (:use [jayq.util :only [log]]))

(def logan 
  (js-obj "name" (ko/observable "Logan")
          "age"  (ko/observable 25) 
          "save"  #(log "Saved!")))

(ko/apply-bindings logan)

