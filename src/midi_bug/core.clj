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
    :lp64   btn/play
    ;;      btn/stop    ; stop
    :riffs  btn/fast-forward ; fast-forward
    :synths btn/rewind)) ; rewind

(def cfg
  {:synths {:s0 mixer-init-state :s1 mixer-init-state :s2 mixer-init-state :m0 mixer-init-state :m1 mixer-init-state :r0 mixer-init-state :r7 basic-mixer-init-state}
   :riffs  {:s0 mixer-init-state :s1 mixer-init-state :m0 mixer-init-state :m1 mixer-init-state :r7 basic-mixer-init-state}
   :master {:s7 mixer-init-state :m7 mixer-init-state :r7 mixer-init-state}})

(def banks
  {:master btn/record
   :lp64   btn/play
   :riffs  btn/fast-forward
   :synths btn/rewind})

(nk2/start! banks cfg)

(defonce default-mixer-g (group :tail (foundation-safe-post-default-group)))

(defonce synth-bus (audio-bus 2))
(defonce riffs-bus (audio-bus 2))

(defonce mixer-s0 (mixers/add-nk-mixer (nk-bank :synths) :s0 default-mixer-g synth-bus))
(defonce mixer-s1 (mixers/add-nk-mixer (nk-bank :synths) :s1 default-mixer-g synth-bus))
(defonce mixer-m0 (mixers/add-nk-mixer (nk-bank :synths) :m0 default-mixer-g synth-bus))
(defonce mixer-m1 (mixers/add-nk-mixer (nk-bank :synths) :m1 default-mixer-g synth-bus))
(defonce mixer-s2 (mixers/add-nk-mixer (nk-bank :synths) :s2 default-mixer-g synth-bus))
(defonce mixer-r0 (mixers/add-nk-mixer (nk-bank :synths) :r0 default-mixer-g synth-bus))

(defonce basic-synth-mix (mixers/basic-mixer [:after default-mixer-g] :in-bus synth-bus))
(defonce basic-riffs-mix (mixers/basic-mixer [:after default-mixer-g] :in-bus riffs-bus))

(defonce mixer-riff-s0 (mixers/add-nk-mixer (nk-bank :riffs) :s0 default-mixer-g riffs-bus))
(defonce mixer-riff-s1 (mixers/add-nk-mixer (nk-bank :riffs) :s1 default-mixer-g riffs-bus))
(defonce mixer-riff-m0 (mixers/add-nk-mixer (nk-bank :riffs) :m0 default-mixer-g riffs-bus))
(defonce mixer-riff-m1 (mixers/add-nk-mixer (nk-bank :riffs) :m1 default-mixer-g riffs-bus))
(defonce mixer-riff-s2 (mixers/add-nk-mixer (nk-bank :riffs) :s2 default-mixer-g riffs-bus))
(defonce mixer-riff-r0 (mixers/add-nk-mixer (nk-bank :riffs) :r0 default-mixer-g riffs-bus))

(on-latest-event [:v-nanoKON2 (nk-bank :synths) :r7 :control-change :slider7]
                 (fn [{:keys [val]}]
                   (ctl basic-synth-mix :amp val))
                 ::synths-master-amp)

(on-latest-event [:v-nanoKON2 (nk-bank :riffs) :r7 :control-change :slider7]
                 (fn [{:keys [val]}]
                   (ctl basic-riffs-mix :amp val))
                 ::riffs-master-amp)


(defsynth high-space-organ [out-bus 0 amp 1 size 200 r 8 noise 10 trig 0 t0 8 t1 16 t2 24 d0 1 d1 1/2 d2 1/4 d3 1/8]
  (let [notes (map #(midicps (duty:kr % (mod trig 16) (dseq [10 20 30] INF))) [d0 d1 d2 d3])
        tones (map (fn [note tone] (blip (* note tone)
        (mul-add:kr (lf-noise1:kr noise) 3 4))) notes [t0 t1 t2])]
        (out out-bus (* amp (g-verb (sum tones) size r)))))

(def so (high-space-organ :amp 0.5 :noise 220 :t0 2 :t1 4 :t2 8 :out-bus (mixers/nkmx :s0)))
