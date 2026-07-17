(ns liftingtruckops.advisor
  "Warehouse Logistics Advisor -- the advisor named in this
  repository's README, proposing a warehouse-crew scheduling/logistics
  coordination operation (log a service record, schedule a crew
  operation, flag a safety concern, coordinate a maintenance order)
  from a site's registered operator/equipment roster and service
  history. Swappable mock/llm; the advisor ONLY proposes --
  `liftingtruckops.governor` checks site/operator/equipment
  provenance, the closed op-allowlist and the maintenance-order cost
  ceiling independently, and always escalates safety-concern flags and
  over-ceiling maintenance orders. Modeled on
  cloud-itonami-isco-3313's accountingsupport.advisor.

  A proposal: {:op :log-service-record|:schedule-crew-operation|
               :flag-safety-concern|:coordinate-maintenance-order
               :effect :propose :site-id str :equipment-id str?
               :operator-id str? :cost number? :record-type kw?
               :concern-type kw? :stake kw :confidence n
               :rationale str}

  IMPORTANT (self-tripping guard, see liftingtruckops.governor): the
  default rationale text below deliberately never uses a
  finalization/execution phrase like 'initiate the lift operation' or
  'move the load' -- it only ever names the *coordination* op being
  proposed, never a lift-operation/load-movement action. This is
  covered by a dedicated regression test in governor-test asserting
  the default mock advisor's proposals for all four ops never
  self-trip the governor's scope-exclusion block.")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake site-id equipment-id operator-id
                              cost record-type concern-type] :as request}]
  {:op op
   :effect :propose
   :site-id site-id
   :equipment-id equipment-id
   :operator-id operator-id
   :cost cost
   :record-type record-type
   :concern-type concern-type
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for site " (:site-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a warehouse logistics advisor for lifting-truck (forklift)
   operations. Given a request, propose an :op (one of
   :log-service-record, :schedule-crew-operation, :flag-safety-concern,
   :coordinate-maintenance-order), the :site-id and, when relevant, the
   :equipment-id/:operator-id/:cost, an honest :confidence and a
   :stake. You coordinate SCHEDULING and LOGISTICS ONLY -- you never
   propose to operate the lifting truck yourself, never propose to
   directly finalize a lift-operation or load-movement decision, and
   never propose to override an operator's on-site safety judgment;
   the governor hard-blocks any such proposal regardless of
   confidence. Never propose a maintenance order beyond the
   equipment's registered cost ceiling without expecting human
   sign-off, and never omit a safety concern -- flagging one always
   requires human review regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
