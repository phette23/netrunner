(in-ns 'game.core)

(def cards-assets
  {"Adonis Campaign"
   {:effect (effect (add-prop card :counter 12))
    :derezzed-events {:runner-turn-ends corp-rez-toast}
    :events {:corp-turn-begins {:msg "gain 3 [Credits]" :counter-cost 3
                                :effect (req (gain state :corp :credit 3)
                                             (when (zero? (:counter card)) (trash state :corp card)))}}}

   "Aggressive Secretary"
   {:advanceable :always
    :access {:optional
             {:req (req installed) :prompt "Pay 2 [Credits] to use Aggressive Secretary ability?"
              :yes-ability {:cost [:credit 2]
                            :effect (req (let [agg card]
                                          (resolve-ability
                                           state side (assoc (assoc-in trash-program [:choices :max] (req (:advance-counter agg)))
                                                        :effect (effect (trash-cards targets))) agg nil)))}}}}

   "Alix T4LB07"
   {:events {:corp-install {:effect (effect (add-prop card :counter 1))}}
    :abilities [{:cost [:click 1] :label "Gain 2 [Credits] for each counter on Alix T4LB07"
                 :msg (msg "gain " (* 2 (get card :counter 0)) " [Credits]")
                 :effect (effect (gain :credit (* 2 (get card :counter 0))) (trash card))}]}

   "Allele Repression"
   {:advanceable :always
    :abilities [{:label "Swap 1 cards in HQ and Archives for each advancement token"
                 :effect (effect (trash card))
                 :msg (msg "Swap " (:advance-counter card) " cards in HQ and Archives")}]}

   "Blacklist"
   {:effect (effect (lock-zone (:cid card) :runner :discard))
    :leave-play (effect (release-zone (:cid card) :runner :discard))}

   "Brain-Taping Warehouse"
   {:events {:pre-rez
             {:req (req (and (ice? target) (has-subtype? target "Bioroid")))
              :effect (effect (rez-cost-bonus (- (:click runner))))}}}

   "Broadcast Square"
   {:abilities [{:label "Trace 3 - Avoid taking a bad publicity"
                 :trace {:base 3 :msg "avoid taking a bad publicity"
                         :effect (effect (lose :bad-publicity 1))}}]}

   "Capital Investors"
   {:abilities [{:cost [:click 1] :effect (effect (gain :credit 2)) :msg "gain 2 [Credits]"}]}

   "Cerebral Overwriter"
   {:advanceable :always
    :access {:optional {:req (req installed)
                        :prompt "Pay 3 [Credits] to use Cerebral Overwriter ability?"
                        :yes-ability {:cost [:credit 3] :msg (msg "do " (:advance-counter card) " brain damage")
                                      :effect (effect (damage :brain (:advance-counter card) {:card card}))}}}}

   "Chairman Hiro"
   {:effect (effect (lose :runner :hand-size-modification 2))
    :leave-play (effect (gain :runner :hand-size-modification 2))
    :trash-effect {:req (req (:access @state)) :effect (effect (as-agenda :runner card 2))}}

   "City Surveillance"
   {:events {:runner-turn-begins
             {:prompt "Pay 1 [Credits] or take 1 tag" :choices ["Pay 1 [Credits]" "Take 1 tag"]
              :player :runner :msg "make the Runner pay 1 [Credits] or take 1 tag"
              :effect (req (if-not (and (= target "Pay 1 [Credits]") (pay state side card :credit 1))
                             (do (tag-runner state side 1) (system-msg state side "takes 1 tag"))
                             (system-msg state side "pays 1 [Credits]")))}}}

   "Constellation Protocol"
   {:derezzed-events {:runner-turn-ends corp-rez-toast}
    :events {:corp-turn-begins
             {:optional
              {:prompt "Move one advancement token between ICE?"
               :yes-ability {:choices {:req #(and (ice? %) (:advance-counter %))}
                             :priority true
                             :effect (req (let [fr target]
                                            (resolve-ability
                                              state side
                                              {:priority true
                                               :prompt "Move to where?"
                                               :choices {:req #(and (ice? %)
                                                                    (not= (:cid fr) (:cid %))
                                                                    (can-be-advanced? %))}
                                               :effect (effect (add-prop :corp target :advance-counter 1)
                                                               (add-prop :corp fr :advance-counter -1)
                                                               (system-msg (str "uses Constellation Protocol to move an advancement token from "
                                                                                (card-str state fr) " to " (card-str state target))))} card nil)
                                            card nil))}}}}}

   "Contract Killer"
   {:advanceable :always
    :abilities [{:label "Trash a connection" :cost [:click 1] :req (req (>= (:advance-counter card) 2))
                 :choices {:req #(has-subtype? % "Connection")}
                 :msg (msg "to trash " (:title target)) :effect (effect (trash card) (trash target))}
                {:cost [:click 1] :req (req (>= (:advance-counter card) 2))
                 :msg "do 2 meat damage" :effect (effect (trash card) (damage :meat 2 {:card card}))}]}

   "Corporate Town"
   {:additional-cost [:forfeit]
    :derezzed-events {:runner-turn-ends corp-rez-toast}
    :events {:corp-turn-begins
             {:prompt "Choose a resource to trash with Corporate Town"
              :choices {:req #(is-type? % "Resource")}
              :msg (msg "trash " (:title target))
              :effect (effect (trash target {:unpreventable true}))}}}

   "Cybernetics Court"
   {:effect (effect (gain :hand-size-modification 4))
    :leave-play (effect (lose :hand-size-modification 4))}

   "Daily Business Show"
   {:events {:corp-draw
             {:msg "draw additional cards"
              :once :per-turn
              :once-key :daily-business-show
              :req (req (first-event state side :corp-draw))
              :effect (req
                        (let [dbs (->> (:corp @state) :servers seq flatten (mapcat :content)
                                       (filter #(and (:rezzed %) (= (:title %) "Daily Business Show")))  count)
                              newcards (take dbs (:deck corp))
                              drawn (conj newcards (last (:hand corp)))]
                          (doseq [c newcards] (move state side c :hand))
                          (resolve-ability
                            state side
                            {:prompt (str "Choose " dbs " card" (if (> dbs 1) "s" "") " to add to the bottom of R&D")
                             :choices {:max dbs
                                       :req #(and (in-hand? %)
                                                  (some (fn [c] (= (:cid c) (:cid %))) drawn))}
                             :msg (msg "add " dbs " card" (if (> dbs 1) "s" "") " to bottom of R&D")
                             :effect (req (doseq [c targets] (move state side c :deck)))} card targets)))}}}

   "Dedicated Response Team"
   {:events {:successful-run-ends {:req (req tagged) :msg "do 2 meat damage"
                                   :effect (effect (damage :meat 2 {:card card}))}}}

   "Dedicated Server"
   {:recurring 2}

   "Director Haas"
   {:effect (effect (gain :click 1 :click-per-turn 1)) :leave-play (effect (lose :click-per-turn 1))
    :trash-effect {:req (req (:access @state)) :effect (effect (as-agenda :runner card 2))}}

   "Docklands Crackdown"
   {:abilities [{:cost [:click 2] :msg "add 1 power counter" :effect (effect (add-prop card :counter 1))}]
    :events {:pre-install {:req (req (and (not (zero? (:counter card)))
                                          (not (get-in @state [:per-turn (:cid card)]))))
                           :effect (effect (install-cost-bonus [:credit (:counter card)]))}
             :runner-install {:req (req (and (not (zero? (:counter card)))
                                             (not (get-in @state [:per-turn (:cid card)]))))
                              :msg (msg "increase the install cost of " (:title target) " by " (:counter card) " [Credits]")
                              :effect (req (swap! state assoc-in [:per-turn (:cid card)] true))}}}

   "Early Premiere"
   {:derezzed-events {:runner-turn-ends corp-rez-toast}
    :abilities [{:cost [:credit 1] :label "Place 1 advancement token on a card that can be advanced in a server"
                 :choices {:req #(and (can-be-advanced? %)
                                      (installed? %)
                                      (= (last (:zone %)) :content))} ; should be *in* a server
                 :effect (effect (add-prop target :advance-counter 1 {:placed true})) :once :per-turn
                 :msg (msg "place 1 advancement token on " (card-str state target))}]}

   "Edge of World"
   {:access {:req (req installed)
             :effect (effect (show-wait-prompt :runner "Corp to use Edge of World")
                             (resolve-ability
                               {:optional
                                {:prompt "Pay 3 [Credits] to use Edge of World ability?"
                                 :yes-ability {:cost [:credit 3]
                                               :msg (msg "do " (count (get-in corp [:servers (last (:server run)) :ices]))
                                                         " brain damage")
                                               :effect (effect (clear-wait-prompt :runner)
                                                               (damage :brain
                                                                       (count (get-in corp [:servers (last (:server (:run @state))) :ices]))
                                                                       {:card card}))}
                                 :no-ability {:effect (effect (clear-wait-prompt :runner))}}}
                               card nil))}}

   "Elizabeth Mills"
   {:effect (effect (lose :bad-publicity 1)) :msg "remove 1 bad publicity"
    :abilities [{:cost [:click 1] :label "Trash a location"
                 :msg (msg "trash " (:title target) " and take 1 bad publicity")
                 :choices {:req #(has-subtype? % "Location")}
                 :effect (effect (trash card) (trash target) (gain :bad-publicity 1))}]}

   "Elizas Toybox"
   {:abilities [{:cost [:click 3] :choices {:req #(not (:rezzed %))}
                 :label "Rez a card at no cost" :msg (msg "rez " (:title target) " at no cost")
                 :effect (effect (rez target {:ignore-cost :all-costs}))}]}

   "Encryption Protocol"
   {:events {:pre-trash {:req (req (installed? target))
                         :effect (effect (trash-cost-bonus 1))}}}

   "Eve Campaign"
   {:effect (effect (add-prop card :counter 16))
    :derezzed-events {:runner-turn-ends corp-rez-toast}
    :events {:corp-turn-begins {:msg "gain 2 [Credits]" :counter-cost 2
                                :effect (req (gain state :corp :credit 2)
                                             (when (zero? (:counter card)) (trash state :corp card)))}}}

   "Executive Boot Camp"
   {:derezzed-events {:runner-turn-ends corp-rez-toast}
    :abilities [{:choices {:req (complement rezzed?)}
                 :label "Rez a card, lowering the cost by 1 [Credits]"
                 :msg (msg "rez " (:title target))
                 :effect (effect (rez-cost-bonus -1) (rez target))}
                {:prompt "Choose an asset to add to HQ"
                 :msg (msg "add " (:title target) " to HQ")
                 :activatemsg "searches R&D for an asset"
                 :choices (req (cancellable (filter #(is-type? % "Asset")
                                                    (:deck corp))
                                            :sorted))
                 :cost [:credit 1]
                 :label "Search R&D for an asset"
                 :effect (effect (trash card) (move target :hand) (shuffle! :deck))}]}

   "Exposé"
   {:advanceable :always
    :abilities [{:label "Remove 1 bad publicity for each advancement token on Exposé"
                 :msg (msg "remove " (:advance-counter card) " bad publicity")
                 :effect (effect (trash card) (lose :bad-publicity (:advance-counter card)))}]}

   "Franchise City"
   {:events {:access {:req (req (is-type? target "Agenda"))
                      :msg "add it to their score area and gain 1 agenda point"
                      :effect (effect (as-agenda :corp card 1))}}}

   "Ghost Branch"
   {:advanceable :always
    :access {:req (req installed)
             :effect (effect (show-wait-prompt :runner "Corp to use Ghost Branch")
                             (resolve-ability
                               {:optional {:prompt "Use Ghost Branch ability?"
                                           :yes-ability {:msg (msg "give the Runner " (:advance-counter card) " tag"
                                                                   (when (> (:advance-counter card) 1) "s"))
                                                         :effect (effect (clear-wait-prompt :runner)
                                                                         (tag-runner :runner (:advance-counter card)))}
                                           :no-ability {:effect (effect (clear-wait-prompt :runner))}}}
                               card nil))}}

   "GRNDL Refinery"
   {:advanceable :always
    :abilities [{:label "Gain 4 [Credits] for each advancement token on GRNDL Refinery"
                 :cost [:click 1] :msg (msg "gain " (* 4 (get card :advance-counter 0)) " [Credits]")
                 :effect (effect (trash card) (gain :credit (* 4 (get card :advance-counter 0))))}]}

   "Haas Arcology AI"
   {:advanceable :while-unrezzed
    :abilities [{:label "Gain [Click]" :once :per-turn :msg "gain [Click]"
                 :cost [:click 1] :advance-counter-cost 1 :effect (effect (gain :click 2))}]}

   "Hostile Infrastructure"
   {:events {:runner-trash {:req (req (= (:side target) "Corp"))
                            :msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}}
    :abilities [{:msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}]}

   "Isabel McGuire"
   {:abilities [{:cost [:click 1] :label "Add an installed card to HQ"
                 :choices {:req installed?}
                 :msg (msg "move " (card-str state target) " to HQ")
                 :effect (effect (move target :hand))}]}

   "IT Department"
   {:abilities [{:counter-cost 1 :label "Add strength to a rezzed ICE"
                 :choices {:req #(and (ice? %) (:rezzed %))}
                 :msg (msg "add strength to a rezzed ICE")
                 :effect (req (update! state side (update-in card [:it-targets (keyword (str (:cid target)))]
                                                             (fnil inc 0)))
                              (update-ice-strength state side target))}
                {:cost [:click 1] :msg "add 1 counter" :effect (effect (add-prop card :counter 1))}]
    :events (let [it {:req (req (:it-targets card))
                      :effect (req (update! state side (dissoc card :it-targets))
                                   (update-all-ice state side))}]
              {:pre-ice-strength {:req (req (get-in card [:it-targets (keyword (str (:cid target)))]))
                                  :effect (effect (ice-strength-bonus
                                                    (* (get-in card [:it-targets (keyword (str (:cid target)))])
                                                       (inc (:counter card))) target))}
               :runner-turn-ends it :corp-turn-ends it})}

   "Jackson Howard"
   {:abilities [{:cost [:click 1] :effect (effect (draw 2)) :msg "draw 2 cards"}
                {:label "Shuffle up to 3 cards from Archives into R&D"
                 :effect (effect
                          (move card :rfg)
                          (resolve-ability
                           {:show-discard true
                            :choices {:max 3 :req #(and (:side % "Corp") (= (:zone %) [:discard]))}
                            :msg (msg "shuffle "
                                      (let [seen (filter :seen targets)]
                                        (str (join ", " (map :title seen))
                                             (let [n (count (filter #(not (:seen %)) targets))]
                                               (when (pos? n)
                                                 (str (when-not (empty? seen) " and ") n " card"
                                                      (when (> n 1) "s"))))))
                                      " into R&D")
                            :effect (req (doseq [c targets] (move state side c :deck))
                                         (shuffle! state side :deck))}
                           card nil))}]}

   "Kala Ghoda Real TV"
   {:events
    {:corp-turn-begins
     {:optional
      {:req (req (not= (:title (:card (first (get-in @state [side :prompt])))) (:title card)))
       :prompt "Use Kala Ghoda Real TV to look at the top card of the Runner's Stack?"
       :yes-ability {:msg "look at the top card of the Runner's Stack"
                     :effect (effect (prompt! card (str "The top card of the Runner's Stack is "
                                                        (:title (first (:deck runner)))) ["OK"] {}))}}}}
    :abilities [{:label "[Trash]: Trash the top card of the Runner's Stack"
                 :msg "trash the top card of the Runner's Stack"
                 :effect (effect (mill :runner)
                                 (trash card {:cause :ability-cost}))}]}

   "Launch Campaign"
   {:effect (effect (add-prop card :counter 6))
    :derezzed-events {:runner-turn-ends corp-rez-toast}
    :events {:corp-turn-begins {:msg "gain 2 [Credits]" :counter-cost 2
                                :effect (req (gain state :corp :credit 2)
                                             (when (zero? (:counter card)) (trash state :corp card)))}}}

   "Levy University"
   {:abilities [{:prompt "Choose an ICE"
                 :msg (msg "adds " (:title target) " to HQ")
                 :choices (req (cancellable (filter ice? (:deck corp)) :sorted))
                 :label "Search R&D for a piece of ICE"
                 :cost [:click 1 :credit 1]
                 :effect (effect (move target :hand) (shuffle! :deck))}]}

   "Lily Lockwell"
   {:effect (effect (draw 3))
    :msg (msg "draw 3 cards")
    :abilities [{:label "Remove a tag to search R&D for an operation"
                 :prompt "Choose an operation to put on top of R&D"
                 :cost [:click 1]
                 :choices (req (let [ops (filter #(is-type? % "Operation") (:deck corp))]
                                 (if (empty? ops) ["No Operation in R&D"] ops)))
                 :req (req (pos? (get-in @state [:runner :tag])))
                 :effect (req (if (not= target "No Operation found")
                                (let [c (move state :corp target :play-area)]
                                  (shuffle! state :corp :deck)
                                  (move state :corp c :deck {:front true})
                                  (system-msg state side (str "uses Lily Lockwell to put " (:title c) " on top of R&D")))
                                (do (shuffle! state :corp :deck)
                                    (system-msg state side (str "uses Lily Lockwell, but did not find an Operation in R&D"))))
                                (lose state :runner :tag 1))}]}

   "Mark Yale"
   {:events {:agenda-counter-spent {:effect (effect (gain :credit 1))
                                    :msg "gain 1 [Credits]"}}
    :abilities [{:label "Trash to gain 2 [Credits]"
                 :msg "gain 2 [Credits]"
                 :effect (effect (gain :credit 2) (trash card))}
                {:label "Spend an agenda counter to gain 2 [Credits]"
                 :effect (req (resolve-ability
                                state side
                                {:prompt "Select an agenda with a counter"
                                 :choices {:req #(and (is-type? % "Agenda")
                                                      (:counter %))}
                                 :effect (req (add-prop state side target :counter -1)
                                              (gain state :corp :credit 2)
                                              (trigger-event state side :agenda-counter-spent card))
                                 :msg (msg "spend an agenda token on " (:title target) " and gain 2 [Credits]")}
                                card nil))}]}

   "Marked Accounts"
   {:abilities [{:cost [:click 1] :msg "store 3 [Credits]"
                 :effect (effect (add-prop card :counter 3))}]
    :events {:corp-turn-begins {:msg "gain 1 [Credits]" :counter-cost 1
                                :effect (effect (gain :credit 1))}}}

   "Melange Mining Corp."
   {:abilities [{:cost [:click 3] :effect (effect (gain :credit 7)) :msg "gain 7 [Credits]"}]}

   "Mental Health Clinic"
   {:effect (effect (gain :runner :hand-size-modification 1))
    :leave-play (effect (lose :runner :hand-size-modification 1))
    :derezzed-events {:runner-turn-ends corp-rez-toast}
    :events {:corp-turn-begins {:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}}}

   "Mumba Temple"
   {:recurring 2}

   "Net Police"
   {:recurring (effect (set-prop card :rec-counter (:link runner)))
    :effect (effect (set-prop card :rec-counter (:link runner)))}

   "News Team"
   {:access {:msg (msg "give the Runner 2 tags or -1 agenda point")
             :effect (effect (resolve-ability
                               {:player :runner
                                :prompt "Take 2 tags or take News Team as -1 agenda point?"
                                :choices ["Take 2 tags" "Add News Team to score area"]
                                :effect (req (if (= target "Add News Team to score area")
                                               (do (or (move state :runner (assoc card :agendapoints -1) :scored)
                                                       (move state :runner (assoc card :agendapoints -1 :zone [:discard]) :scored))
                                                   (gain-agenda-point state :runner -1)
                                                   (system-msg state side
                                                    (str "adds News Team to their score area as -1 agenda point")))
                                               (do (tag-runner state :runner 2)
                                                   (system-msg state side (str "takes 2 tags from News Team")))))}
                              card targets))}}

   "PAD Campaign"
   {:derezzed-events {:runner-turn-ends corp-rez-toast}
    :events {:corp-turn-begins {:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}}}

   "Plan B"
   {:advanceable :always
    :access {:optional
             {:prompt "Score an Agenda from HQ?"
              :req (req installed)
              :yes-ability {:effect (req (let [c card]
                                           (resolve-ability
                                             state side
                                             {:prompt "Choose an Agenda in HQ to score"
                                              :choices {:req #(and (is-type? % "Agenda")
                                                                   (<= (:advancementcost %) (:advance-counter c))
                                                                   (in-hand? %))}
                                              :msg (msg "score " (:title target))
                                              :effect (effect (score (assoc target :advance-counter
                                                                                   (:advancementcost target))))} c nil)))}}}}

   "Primary Transmission Dish"
   {:recurring 3}

   "Private Contracts"
   {:effect (effect (add-prop card :counter 14))
    :abilities [{:cost [:click 1] :counter-cost 2 :msg "gain 2 [Credits]"
                 :effect (req (gain state :corp :credit 2)
                              (when (= (:counter card) 0) (trash state :corp card)))}]}

   "Project Junebug"
   {:advanceable :always
    :access {:optional {:prompt "Pay 1 [Credits] to use Project Junebug ability?"
                        :req (req (and installed (> (:credit corp) 0)))
                        :yes-ability {:cost [:credit 1] :msg (msg "do " (* 2 (get card :advance-counter 0)) " net damage")
                                      :effect (effect (damage :net (* 2 (get card :advance-counter 0)) {:card card}))}}}}

   "Psychic Field"
   (let [ab {:psi {:req (req installed)
                   :not-equal {:msg (msg "do " (count (:hand runner)) " net damage")
                               :effect (effect (damage :net (count (:hand runner)) {:card card}))}}}]
     {:expose ab :access ab})

   "Public Support"
   {:effect (effect (add-prop card :counter 3))
    :derezzed-events {:runner-turn-ends corp-rez-toast}
    :events {:corp-turn-begins
             {:effect (req (add-prop state side card :counter -1)
                           (when (<= (:counter card) 1)
                             (system-msg state :corp "adds Public Support to his scored area and gains 1 agenda point")
                             (as-agenda state :corp (dissoc card :counter) 1)))} }}

   "Reality Threedee"
   {:effect (effect (gain :bad-publicity 1) (system-msg "takes 1 bad publicity"))
    :derezzed-events {:runner-turn-ends corp-rez-toast}
    :events {:corp-turn-begins
             {:effect (req (gain state side :credit (if tagged 2 1)))
              :msg (msg (if tagged "gain 2 [Credits]" "gain 1 [Credits]"))}}}

   "Reversed Accounts"
   {:advanceable :always
    :abilities [{:cost [:click 1]
                 :label "Force the Runner to lose 4 [Credits] per advancement"
                 :msg (msg "force the Runner to lose " (min (* 4 (get card :advance-counter 0)) (:credit runner)) " [Credits]")
                 :effect (effect (lose :runner :credit (* 4 (get card :advance-counter 0))) (trash card))}]}

   "Rex Campaign"
   {:effect (effect (add-prop card :counter 3))
    :derezzed-events {:runner-turn-ends corp-rez-toast}
    :events {:corp-turn-begins
             {:effect (req (add-prop state side card :counter -1)
                           (when (<= (:counter card) 1)
                             (trash state side card)
                             (resolve-ability state side
                                              {:prompt "Remove 1 bad publicity or gain 5 [Credits]?"
                                               :choices ["Remove 1 bad publicity" "Gain 5 [Credits]"]
                                               :msg (msg (if (= target "Remove 1 bad publicity")
                                                           "remove 1 bad publicity" "gain 5 [Credits]"))
                                               :effect (req (if (= target "Remove 1 bad publicity")
                                                              (lose state side :bad-publicity 1)
                                                              (gain state side :credit 5)))}
                                              card targets)))}}}

   "Ronald Five"
   {:events {:runner-trash {:req (req (and (= (:side target) "Corp") (> (:click runner) 0)))
                            :msg "force the runner to lose 1 [Click]" :effect (effect (lose :runner :click 1))}}}

   "Ronin"
   {:advanceable :always
    :abilities [{:cost [:click 1] :req (req (>= (:advance-counter card) 4))
                 :msg "do 3 net damage" :effect (effect (trash card) (damage :net 3 {:card card}))}]}

   "Sealed Vault"
   {:abilities [{:label "Store any number of [Credits] on Sealed Vault" :cost [:credit 1]
                 :prompt "How many [Credits]?" :choices :credit :msg (msg "store " target " [Credits]")
                 :effect (effect (add-prop card :counter target))}
                {:label "Move any number of [Credits] to your credit pool"
                 :cost [:click 1] :prompt "How many [Credits]?"
                 :choices :counter :msg (msg "gain " target " [Credits]")
                 :effect (effect (gain :credit target))}
                {:label "[Trash]: Move any number of [Credits] to your credit pool"
                 :prompt "How many [Credits]?" :choices :counter
                 :msg (msg "trash it and gain " target " [Credits]")
                 :effect (effect (gain :credit target) (trash card))}]}

   "Security Subcontract"
   {:abilities [{:choices {:req #(and (ice? %) (rezzed? %))} :cost [:click 1]
                 :msg (msg "trash " (:title target) " to gain 4 [Credits]")
                 :label "Trash a rezzed ICE to gain 4 [Credits]"
                 :effect (effect (trash target) (gain :credit 4))}]}

   "Server Diagnostics"
   {:derezzed-events {:runner-turn-ends corp-rez-toast}
    :events {:corp-turn-begins {:effect (effect (gain :credit 2))
                                :msg "gain 2 [Credits]"}
             :corp-install {:req (req (ice? target))
                            :effect (effect (trash card)
                                            (system-msg "trashes Server Diagnostics"))}}}

   "Shannon Claire"
   {:abilities [{:cost [:click 1]
                 :msg "draw 1 card from the bottom of R&D"
                 :effect (effect (move (last (:deck corp)) :hand))}
                {:label "[Trash]: Search R&D for an agenda"
                 :prompt "Choose an agenda to add to the bottom of R&D"
                 :msg (msg "reveal " (:title target) " from R&D and add it to the bottom of R&D")
                 :choices (req (cancellable (filter #(is-type? % "Agenda") (:deck corp)) :sorted))
                 :effect (effect (shuffle! :deck) (move target :deck)
                                 (trash card {:cause :ability-cost}))}
                {:label "[Trash]: Search Archives for an agenda"
                 :prompt "Choose an agenda to add to the bottom of R&D"
                 :msg (msg "reveal " (:title target) " from Archives and add it to the bottom of R&D")
                 :choices (req (cancellable (filter #(is-type? % "Agenda") (:discard corp)) :sorted))
                 :effect (effect (move target :deck) (trash card {:cause :ability-cost}))}]}

   "Shattered Remains"
   {:advanceable :always
    :access {:optional
             {:req (req installed) :prompt "Pay 1 [Credits] to use Shattered Remains ability?"
              :yes-ability {:cost [:credit 1]
                            :effect (req (let [shat card]
                                          (resolve-ability
                                           state side (assoc (assoc-in trash-hardware [:choices :max] (req (:advance-counter shat)))
                                                        :effect (effect (trash-cards targets))) shat nil)))}}}}

   "Shi.Kyū"
   {:access
    {:optional {:req (req (not= (first (:zone card)) :deck))
                :prompt "Pay [Credits] to use Shi.Kyū?"
                :yes-ability {:prompt "How many [Credits] for Shi.Kyū?" :choices :credit
                              :msg (msg "attempt to do " target " net damage")
                              :effect (effect (resolve-ability
                               {:player :runner
                                :prompt (str "Take " target " net damage or take Shi.Kyū as -1 agenda point?")
                                :choices [(str "Take " target " net damage") "Add Shi.Kyū to score area"]
                                :effect (let [dmg target]
                                          (req (if (= target "Add Shi.Kyū to score area")
                                                 (do (or (move state :runner (assoc card :agendapoints -1) :scored) ; if the runner did not trash the card on access, then this will work
                                                         (move state :runner (assoc card :agendapoints -1 :zone [:discard]) :scored)) ;if the runner did trash it, then this will work
                                                   (gain-agenda-point state :runner -1)
                                                   (system-msg state side
                                                    (str "adds Shi.Kyū to their score area as -1 agenda point")))
                                                 (do (damage state :corp :net dmg {:card card})
                                                   (system-msg state :corp
                                                    (str "uses Shi.Kyū to do " dmg " net damage"))))))}
                              card targets))}}}}

   "Shock!"
   {:access {:msg "do 1 net damage" :effect (effect (damage :net 1 {:card card}))}}

   "Snare!"
   {:access {:req (req (not= (first (:zone card)) :discard))
             :effect (effect (show-wait-prompt :runner "Corp to use Snare!")
                             (resolve-ability
                               {:optional {:prompt "Pay 4 [Credits] to use Snare! ability?"
                                           :yes-ability {:cost [:credit 4]
                                                         :msg "do 3 net damage and give the Runner 1 tag"
                                                         :effect (effect (clear-wait-prompt :runner)
                                                                         (damage :net 3 {:card card})
                                                                         (tag-runner :runner 1))}
                                           :no-ability {:effect (effect (clear-wait-prompt :runner))}}} card nil))}}

   "Space Camp"
   {:access {:msg (msg "place 1 advancement token on " (card-str state target))
             :choices {:req can-be-advanced?}
             :effect (effect (add-prop target :advance-counter 1 {:placed true}))}}

   "Sundew"
   {:events {:runner-spent-click {:req (req (= (:click runner) (:click-per-turn runner)))
                                  :once :per-turn
                                  :msg (req (when (not this-server) "gain 2 [Credits]"))
                                  :effect (req (when (not this-server)
                                                 (gain state :corp :credit 2)))}}}

   "Team Sponsorship"
   {:events {:agenda-scored {:effect (req (toast state :corp (str "Click Team Sponsorship "
                                                                  "to install a card from "
                                                                  "Archives or HQ.") "info")
                                          (update! state side (assoc card :ts-active true)))}}
    :abilities [{:label "Install a card from Archives or HQ"
                 :req (req (:ts-active card))
                 :prompt "Choose a card from Archives or HQ to install"
                 :show-discard true
                 :choices {:req #(and (not (is-type? % "Operation"))
                                      (#{[:hand] [:discard]} (:zone %)))}
                 :msg (msg (corp-install-msg target))
                 :effect (effect (corp-install target nil {:no-install-cost true})
                                 (update! (dissoc (get-card state card) :ts-active)))}]}

   "Tech Startup"
   {:derezzed-events {:runner-turn-ends corp-rez-toast}
    :abilities [{:label "Install an asset from R&D"
                 :prompt "Choose an asset to install"
                 :msg (msg "install " (:title target))
                 :choices (req (filter #(is-type? % "Asset") (:deck corp)))
                 :effect (effect (trash card) (corp-install target nil) (shuffle! :deck))}]}

   "Tenma Line"
   {:abilities [{:label "Swap 2 pieces of installed ICE"
                 :cost [:click 1]
                 :prompt "Select two pieces of ICE to swap positions"
                 :choices {:req #(and (installed? %) (ice? %)) :max 2}
                 :effect (req (if (= (count targets) 2)
                                (let [fndx (ice-index state (first targets))
                                      sndx (ice-index state (second targets))
                                      fnew (assoc (first targets) :zone (:zone (second targets)))
                                      snew (assoc (second targets) :zone (:zone (first targets)))]
                                  (swap! state update-in (cons :corp (:zone (first targets)))
                                         #(assoc % fndx snew))
                                  (swap! state update-in (cons :corp (:zone (second targets)))
                                         #(assoc % sndx fnew))
                                  (doseq [newcard [fnew snew]]
                                    (doseq [h (:hosted newcard)]
                                      (let [newh (-> h (assoc-in [:zone] '(:onhost))
                                                     (assoc-in [:host :zone] (:zone newcard)))]
                                        (update! state side newh)
                                        (unregister-events state side h)
                                        (register-events state side (:events (card-def newh)) newh))))
                                  (update-ice-strength state side fnew)
                                  (update-ice-strength state side snew))))
                 :msg "swap the positions of two ICE"}]}

   "Test Ground"
   {:advanceable :always
    :abilities [{:label "Derez 1 card for each advancement token"
                 :msg (msg "derez " (:advance-counter card)) :effect (effect (trash card))}]}

   "The Board"
   {:effect (effect (lose :runner :agenda-point
                          (count (filter #(> (get-agenda-points state :runner %) 0) (:scored runner)))))
    :leave-play (effect (gain :runner :agenda-point
                              (count (filter #(> (get-agenda-points state :runner %) 0) (:scored runner)))))
    :trash-effect {:req (req (:access @state)) :effect (effect (as-agenda :runner card 2))}
    :events {:agenda-stolen {:req (req (> (get-agenda-points state :runner target) 0))
                             :effect (effect (lose :runner :agenda-point 1))}}}

   "The News Now Hour"
   {:events {:runner-turn-begins {:effect (req (prevent-current state side))}}
    :effect (req (prevent-current state side))
    :leave-play (req (swap! state assoc-in [:runner :register :cannot-play-current] false))}

   "The Root"
   {:recurring 3}

   "Thomas Haas"
   {:advanceable :always
    :abilities [{:label "Gain credits" :msg (msg "gain " (* 2 (get card :advance-counter 0)) " [Credits]")
                 :effect (effect (gain :credit (* 2 (get card :advance-counter 0))) (trash card))}]}

   "Toshiyuki Sakai"
   {:advanceable :always}

   "Turtlebacks"
   {:events {:server-created {:msg "gain 1 [Credits]" :effect (effect (gain :credit 1))}}}

   "Victoria Jenkins"
   {:effect (effect (lose :runner :click-per-turn 1)) :leave-play (effect (gain :runner :click-per-turn 1))
    :trash-effect {:req (req (:access @state)) :effect (effect (as-agenda :runner card 2))}}

   "Worlds Plaza"
   {:abilities [{:label "Install an asset on Worlds Plaza"
                 :req (req (< (count (:hosted card)) 3))
                 :cost [:click 1]
                 :prompt "Choose an asset to install on Worlds Plaza"
                 :choices {:req #(and (is-type? % "Asset")
                                      (in-hand? %)
                                      (= (:side %) "Corp"))}
                 :msg (msg "host " (:title target))
                 :effect (req (trigger-event state side :corp-install target)
                              (host state side card target)
                              (rez-cost-bonus state side -2) (rez state side (last (:hosted (get-card state card))))
                              (when (:rezzed (last (:hosted (get-card state card))))
                                (update! state side (dissoc (get-card state (last (:hosted card))) :facedown))))}]}})
