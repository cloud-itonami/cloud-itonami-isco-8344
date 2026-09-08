(ns liftingtruckops.governor
  "LiftingTruckOpsGovernor -- the independent safety/traceability
  layer named in this repository's README/business-model.md, gating
  every warehouse-crew scheduling/logistics proposal an advisor may
  make for a site's registered operator/equipment roster. The
  governor never dispatches hardware itself, never operates a lifting
  truck, and never finalizes a lift-operation or load-movement
  decision. Modeled on cloud-itonami-isco-3313's
  accountingsupport.governor. Task twist: this occupation carries a
  real physical-safety stake (load drop, tip-over, pedestrian
  collision), so the closed op-allowlist structurally excludes any op
  that could finalize a lift-operation/load-movement decision, AND a
  content-based scope-exclusion HARD block independently catches a
  compromised advisor that tries to smuggle a lift-operation/
  load-movement finalization (or an override of an operator's on-site
  safety judgment) into the free-text rationale of an otherwise
  allowlisted op.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. site provenance        -- the warehouse/site must be
                                 registered.
    2. no-actuation            -- proposal :effect must be :propose
                                 (the governor never dispatches
                                 hardware, never operates the lifting
                                 truck, and never itself finalizes a
                                 lift-operation/load-movement
                                 decision; it only gates what the
                                 advisor may coordinate).
    3. closed op-allowlist     -- :op must be one of the four
                                 registered coordination ops
                                 (:log-service-record,
                                 :schedule-crew-operation,
                                 :flag-safety-concern,
                                 :coordinate-maintenance-order); no op
                                 resembling operating the lift truck or
                                 finalizing a load movement exists in
                                 this allowlist at all -- it is
                                 structurally absent, not merely
                                 gated.
    4. equipment basis         -- a service-record or
                                 maintenance-order proposal must cite a
                                 REGISTERED lifting truck belonging to
                                 this site.
    5. operator basis          -- a crew-operation scheduling proposal
                                 must cite a REGISTERED operator
                                 belonging to this site.
    6. scope exclusion         -- defense in depth: any proposal whose
                                 rationale/detail text names a
                                 finalization/execution action for a
                                 lift-operation, a load movement, or an
                                 override of an operator's on-site
                                 safety judgment is a hard, PERMANENT
                                 block -- phrased as the finalization
                                 ACTION (e.g. 'initiate the lift
                                 operation', 'move the load'), never a
                                 bare noun ('lift', 'load'), to avoid
                                 false-tripping on the mock advisor's
                                 own default rationale text (which only
                                 ever names the coordination op being
                                 proposed). See governor-test for a
                                 dedicated regression asserting the
                                 default mock advisor's proposals for
                                 all four ops never self-trip this
                                 rule, plus a true-positive test that a
                                 hand-crafted finalization rationale
                                 still hard-blocks.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls -- these are :high/
  :safety-critical regardless of confidence):
    7. :op :flag-safety-concern (an equipment-defect/load-hazard/
                                 operator-fatigue concern always
                                 requires human review; never
                                 auto-commit-eligible).
    8. maintenance-order cost above the equipment's registered
       :max-maintenance-cost (routine procurement above a site's
       registered ceiling, not a lift-operation-authority risk, so it
       escalates rather than hard-blocking).
    9. low confidence (< `confidence-floor`)."
  (:require [kotoba.lang.text :as str]
            [liftingtruckops.store :as store]))

(def confidence-floor 0.6)

(def ^:private allowed-ops
  #{:log-service-record :schedule-crew-operation :flag-safety-concern
    :coordinate-maintenance-order})

(def ^:private always-escalate-ops #{:flag-safety-concern})

;; Phrased as finalization/execution ACTIONS, never bare nouns -- see
;; the namespace docstring's rule 6 and the dedicated self-tripping
;; regression test in governor-test.
(def ^:private scope-excluded-phrases
  ["initiate the lift operation"
   "initiate the load movement"
   "move the load"
   "finalize the lift operation"
   "finalize the load movement"
   "authorize the load movement"
   "authorize load movement"
   "execute the load movement"
   "execute the lift operation"
   "commit to the load movement"
   "direct the lift operation"
   "operate the lifting truck"
   "operate the forklift"
   "drive the forklift"
   "drive the lifting truck"
   "confirm the load movement"
   "override the operator's safety judgment"
   "override the operator safety judgment"
   "override the operator's on-site safety judgment"])

(defn- scope-excluded-text? [text]
  (when (string? text)
    (let [lower (str/lower text)]
      (some #(str/includes? lower %) scope-excluded-phrases))))

(defn- scope-excluded? [proposal]
  (or (scope-excluded-text? (:rationale proposal))
      (scope-excluded-text? (:detail proposal))))

(defn- hard-violations [{:keys [proposal]} site-record eq op-record]
  (let [{:keys [op equipment-id operator-id]} proposal
        needs-equipment? (contains? #{:log-service-record :coordinate-maintenance-order} op)
        needs-operator? (= :schedule-crew-operation op)]
    (cond-> []
      (nil? site-record)
      (conj {:rule :no-site :detail "未登録 site"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor はリフト操作/荷役の確定を直接実行しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :unknown-op :detail (str "closed allowlist 外の op: " (pr-str op) "（リフト操作・荷役確定に相当する op はこの allowlist に存在しない）")})

      (and needs-equipment? equipment-id (nil? eq))
      (conj {:rule :unknown-equipment :detail "未登録 equipment への提案は不可"})

      (and needs-equipment? (not equipment-id))
      (conj {:rule :unknown-equipment :detail "equipment-id が指定されていない"})

      (and needs-equipment? eq site-record (not= (:site-id eq) (:site-id site-record)))
      (conj {:rule :equipment-wrong-site :detail "equipment が別 site のもの"})

      (and needs-operator? operator-id (nil? op-record))
      (conj {:rule :unknown-operator :detail "未登録 operator への提案は不可"})

      (and needs-operator? (not operator-id))
      (conj {:rule :unknown-operator :detail "operator-id が指定されていない"})

      (and needs-operator? op-record site-record (not= (:site-id op-record) (:site-id site-record)))
      (conj {:rule :operator-wrong-site :detail "operator が別 site のもの"})

      (scope-excluded? proposal)
      (conj {:rule :scope-exclusion
             :detail "リフト操作/荷役決定の確定、またはオペレーターの現場安全判断の上書きに相当する提案は恒久的に禁止（このアクターは調整/ロジスティクスのみを行う）"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `liftingtruckops.store/Store`. Pure -- never
  mutates the store, never operates the lifting truck, never
  finalizes a lift-operation/load-movement decision."
  [request context proposal store]
  (let [site-record (some->> (:site-id request) (store/site store))
        eq (some->> (:equipment-id proposal) (store/equipment store))
        op-record (some->> (:operator-id proposal) (store/operator store))
        hard (hard-violations {:request request :proposal proposal}
                               site-record eq op-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        cost-over-ceiling? (and (= :coordinate-maintenance-order (:op proposal))
                                 eq
                                 (number? (:cost proposal))
                                 (number? (:max-maintenance-cost eq))
                                 (> (:cost proposal) (:max-maintenance-cost eq)))
        always-risky? (or (contains? always-escalate-ops (:op proposal))
                           cost-over-ceiling?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
