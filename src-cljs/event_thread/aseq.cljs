(ns event-thread.aseq)

(defprotocol Aseq
  (afirst [coll callback])
  (arest  [coll callback])
  (acons  [item seq]))

(def aseq identity)

(defn event-stream-cell []
  {:first ()})
