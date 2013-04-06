(ns event-thread.main
  (:require [event-thread.view-models.person :as person]
            [knockout :as ko]))

(def logan 
  (person/create :name "Logan" :age "25"))

(ko/apply-bindings logan)

