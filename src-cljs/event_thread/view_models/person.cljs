(ns event-thread.view-models.person
  (:require [knockout  :as ko]
            [jayq.util :as util]))

(defn save []
  (util/log "Saved!"))

(defn create [& {:keys [name age]}]
  (js-obj
    "name" (ko/observable name)
    "age"  (ko/observable age) 
    "save" save))

