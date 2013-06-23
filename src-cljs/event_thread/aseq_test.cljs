(ns event-thread.aseq-test
  (use event-thread.test :only [test]))

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

; (->> [1 2 3 4]
;      (cljs.core/map deferred)
;      (aseq)
;      (reduce (comp deferred +) 0)
;      ((fn [reduce-deferred] (jq/done reduce-deferred log))))
;
; Should produce:
; 10

; (let [writer      (producer)
;       reader      (deref writer)
;       running-sum (reductions (comp deferred +) 0 reader)
;       logged      (mapd log running-sum)]
;   (produce writer 1)
;   (produce writer 2)
;   (produce writer 3)
;   (produce writer 4))

; Should produce:
; 0
; 1
; 3
; 6
; 10

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
