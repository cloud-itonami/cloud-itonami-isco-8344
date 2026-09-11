(ns liftingtruckops.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [liftingtruckops.store :as store]
            [liftingtruckops.advisor :as advisor]
            [liftingtruckops.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-site! st {:site-id "site-1" :name "North Yard Warehouse"})
    (store/register-operator! st {:operator-id "OP-1" :site-id "site-1" :name "Kenji Sato"})
    (store/register-equipment! st {:equipment-id "EQ-1" :site-id "site-1"
                                    :name "forklift-042"
                                    :max-maintenance-cost 3000})
    st))

(def ^:private req {:site-id "site-1"})

(defn- log-op [equipment-id]
  {:op :log-service-record :effect :propose :site-id "site-1"
   :equipment-id equipment-id :record-type :usage
   :confidence 0.9 :stake :low})

(defn- schedule-op [operator-id]
  {:op :schedule-crew-operation :effect :propose :site-id "site-1"
   :operator-id operator-id
   :confidence 0.9 :stake :low})

(defn- maintenance-op [cost]
  {:op :coordinate-maintenance-order :effect :propose :site-id "site-1"
   :equipment-id "EQ-1" :cost cost
   :confidence 0.9 :stake :low})

(defn- safety-op []
  {:op :flag-safety-concern :effect :propose :site-id "site-1"
   :equipment-id "EQ-1" :concern-type :load-hazard
   :confidence 0.9 :stake :high})

;; -- ok paths ----------------------------------------------------------

(deftest ok-log-service-record-known-equipment
  (let [st (fresh-store)
        v (governor/check req {} (log-op "EQ-1") st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest ok-schedule-crew-operation-known-operator
  (let [st (fresh-store)
        v (governor/check req {} (schedule-op "OP-1") st)]
    (is (:ok? v))))

(deftest ok-coordinate-maintenance-order-within-ceiling
  (let [st (fresh-store)
        v (governor/check req {} (maintenance-op 2000) st)]
    (is (:ok? v))))

(deftest ok-at-exact-ceiling-boundary
  (testing "the maintenance-order cost ceiling is inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (maintenance-op 3000) st)]
      (is (:ok? v)))))

;; -- HARD invariants -----------------------------------------------------

(deftest hard-on-unregistered-site
  (let [st (fresh-store)
        v (governor/check {:site-id "ghost-site"} {} (log-op "EQ-1") st)]
    (is (:hard? v))
    (is (some #(= :no-site (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op "EQ-1") :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-unknown-op
  (testing "closed op-allowlist -- no op resembling lift operation/load-movement finalization exists in it"
    (let [st (fresh-store)
          v (governor/check req {} {:op :operate-lifting-truck :effect :propose
                                     :site-id "site-1" :confidence 0.99 :stake :low
                                     :rationale "operate the lifting truck directly"} st)]
      (is (:hard? v))
      (is (some #(= :unknown-op (:rule %)) (:violations v))))))

(deftest hard-on-unknown-equipment
  (let [st (fresh-store)
        v (governor/check req {} (log-op "EQ-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-equipment (:rule %)) (:violations v)))))

(deftest hard-on-missing-equipment-id
  (let [st (fresh-store)
        v (governor/check req {} (dissoc (log-op "EQ-1") :equipment-id) st)]
    (is (:hard? v))
    (is (some #(= :unknown-equipment (:rule %)) (:violations v)))))

(deftest hard-on-equipment-wrong-site
  (let [st (fresh-store)]
    (store/register-site! st {:site-id "site-2" :name "Other Yard"})
    (store/register-equipment! st {:equipment-id "EQ-2" :site-id "site-2"
                                    :name "forklift-099" :max-maintenance-cost 1000})
    (let [v (governor/check req {} (log-op "EQ-2") st)]
      (is (:hard? v))
      (is (some #(= :equipment-wrong-site (:rule %)) (:violations v))))))

(deftest hard-on-unknown-operator
  (let [st (fresh-store)
        v (governor/check req {} (schedule-op "OP-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-operator (:rule %)) (:violations v)))))

(deftest hard-on-missing-operator-id
  (let [st (fresh-store)
        v (governor/check req {} (dissoc (schedule-op "OP-1") :operator-id) st)]
    (is (:hard? v))
    (is (some #(= :unknown-operator (:rule %)) (:violations v)))))

(deftest hard-on-operator-wrong-site
  (let [st (fresh-store)]
    (store/register-site! st {:site-id "site-2" :name "Other Yard"})
    (store/register-operator! st {:operator-id "OP-2" :site-id "site-2" :name "Someone Else"})
    (let [v (governor/check req {} (schedule-op "OP-2") st)]
      (is (:hard? v))
      (is (some #(= :operator-wrong-site (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rationale-move-the-load
  (testing "a proposal narrating a load-movement finalization is a hard, permanent block even on an otherwise-allowlisted op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (log-op "EQ-1")
                                           :rationale "move the load to bay 4 without operator sign-off")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-exclusion (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rationale-initiate-lift-operation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (maintenance-op 500)
                                         :rationale "initiate the lift operation ahead of the maintenance window")
                           st)]
    (is (:hard? v))
    (is (some #(= :scope-exclusion (:rule %)) (:violations v)))))

(deftest hard-on-scope-excluded-rationale-override-operator-safety-judgment
  (let [st (fresh-store)
        v (governor/check req {} (assoc (schedule-op "OP-1")
                                         :rationale "override the operator's safety judgment to keep the shift moving")
                           st)]
    (is (:hard? v))
    (is (some #(= :scope-exclusion (:rule %)) (:violations v)))))

(deftest hard-on-scope-excluded-detail-field
  (testing "the scope-exclusion check also covers a :detail free-text field, not only :rationale"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (log-op "EQ-1")
                                           :detail "plan: drive the forklift to the dock")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-exclusion (:rule %)) (:violations v))))))

;; -- ESCALATE invariants ---------------------------------------------------

(deftest always-escalates-flag-safety-concern-even-at-high-confidence
  (testing "flag-safety-concern always requires human review; never auto-commit-eligible"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (safety-op) :confidence 0.99) st)]
      (is (not (:hard? v)))
      (is (:escalate? v))
      (is (not (:ok? v))))))

(deftest always-escalates-over-ceiling-maintenance-order-even-at-high-confidence
  (testing "a maintenance order above the equipment's registered cost ceiling always requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (maintenance-op 50000) :confidence 0.99) st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op "EQ-1") :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

;; -- self-tripping regression (CRITICAL: see governor namespace docstring
;;    rule 6) -------------------------------------------------------------

(deftest default-mock-advisor-proposals-never-self-trip-scope-exclusion
  (testing "the default mock advisor's own generated rationale for every allowlisted op never contains a scope-excluded finalization phrase"
    (let [st (fresh-store)
          adv (advisor/mock-advisor)
          requests [{:op :log-service-record :site-id "site-1" :equipment-id "EQ-1"
                     :record-type :usage :stake :low}
                    {:op :schedule-crew-operation :site-id "site-1" :operator-id "OP-1"
                     :stake :low}
                    {:op :flag-safety-concern :site-id "site-1" :equipment-id "EQ-1"
                     :concern-type :load-hazard :stake :high}
                    {:op :coordinate-maintenance-order :site-id "site-1" :equipment-id "EQ-1"
                     :cost 500 :stake :low}]]
      (doseq [request requests]
        (let [proposal (advisor/-advise adv st request)
              v (governor/check request {} proposal st)]
          (is (not (some #(= :scope-exclusion (:rule %)) (:violations v)))
              (str "self-tripped on default advisor proposal for " (:op request)
                   ": " (:violations v))))))))
