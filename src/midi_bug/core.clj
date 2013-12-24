(ns midi-bug.core
  (:use
   [overtone.live]
   [nano-kontrol2.config :only [mixer-init-state basic-mixer-init-state]])
  (:require
   [nano-kontrol2.core :as nk2]
   [nano-kontrol2.buttons :as btn]
   [midi-bug.mixers :as mixers]))

(defn nk-bank
  "Returns the nk bank number for the specified bank key"
  [bank-k]
  (case bank-k
    :master btn/record
    :synths btn/rewind))

(def cfg
  {:synths {:s0 mixer-init-state}
   :master {:s7 mixer-init-state :m7 mixer-init-state :r7 mixer-init-state}})

(def banks {:master btn/record :synths btn/rewind})

(nk2/start! banks cfg)

(defonce default-mixer-g (group :tail (foundation-safe-post-default-group)))

(defonce synth-bus (audio-bus 2))
(defonce mixer-s0 (mixers/add-nk-mixer (nk-bank :synths) :s0 default-mixer-g synth-bus))
(defonce basic-synth-mix (mixers/basic-mixer [:after default-mixer-g] :in-bus synth-bus))

(defsynth high-space-organ [out-bus 0 amp 1 size 200 r 8 noise 10 trig 0 t0 8 t1 16 t2 24 d0 1 d1 1/2 d2 1/4 d3 1/8]
  (let [notes (map #(midicps (duty:kr % (mod trig 16) (dseq [10 20 30] INF))) [d0 d1 d2 d3])
        tones (map (fn [note tone] (blip (* note tone)
        (mul-add:kr (lf-noise1:kr noise) 3 4))) notes [t0 t1 t2])]
        (out out-bus (* amp (g-verb (sum tones) size r)))))

(def so (high-space-organ :amp 0.5 :noise 220 :t0 2 :t1 4 :t2 8 :out-bus (mixers/nkmx :s0)))
