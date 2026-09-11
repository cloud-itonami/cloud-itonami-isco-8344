(ns liftingtruckops.store
  "SSoT for the ISCO-08 8344 lifting truck operators warehouse
  coordination actor (itonami actor pattern, ADR-2607011000 /
  ADR-2607121000 / CLAUDE.md Actors section; README's 'Robotics
  premise' -- a warehouse-crew scheduling/logistics coordination robot
  performs service-record logging, crew-shift scheduling and
  maintenance-order coordination for lifting-truck (forklift) fleets
  under this advisor/governor pair, which never dispatches hardware
  itself, never operates a lifting truck, and never finalizes a
  lift-operation/load-movement decision). Modeled on
  cloud-itonami-isco-3313's accountingsupport.store.

  Domain:

    site      -- a registered warehouse/site {:site-id :name}
    operator  -- a registered lift-truck operator/crew-roster member
                 {:operator-id :site-id :name}
    equipment -- a registered lifting truck {:equipment-id :site-id
                 :name :max-maintenance-cost number}.
                 `:max-maintenance-cost` is the registered ceiling a
                 proposed maintenance-order cost must not exceed
                 without escalation -- a maintenance order above the
                 site's registered ceiling always requires human
                 sign-off, but is routine procurement (not a
                 lift-operation risk), so it escalates rather than
                 hard-blocks.
    record    -- a committed operating record (a logged service
                 record, a scheduled crew operation, a coordinated
                 maintenance order) -- written ONLY via commit-record!.
    ledger    -- append-only audit trail, commit or hold.

  This actor coordinates SCHEDULING/LOGISTICS ONLY -- it never
  operates the lifting truck and never finalizes a lift-operation or
  load-movement decision. That authority stays with the human
  operator on site; see liftingtruckops.governor for the hard,
  permanent scope-exclusion block on any such proposal.")

(defprotocol Store
  (site [s site-id])
  (operator [s operator-id])
  (equipment [s equipment-id])
  (records-of [s site-id])
  (ledger [s])
  (register-site! [s site])
  (register-operator! [s op])
  (register-equipment! [s eq])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (site [_ site-id] (get-in @a [:sites site-id]))
  (operator [_ operator-id] (get-in @a [:operators operator-id]))
  (equipment [_ equipment-id] (get-in @a [:equipment equipment-id]))
  (records-of [_ site-id] (filter #(= site-id (:site-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-site! [s site]
    (swap! a assoc-in [:sites (:site-id site)] site) s)
  (register-operator! [s op]
    (swap! a assoc-in [:operators (:operator-id op)] op) s)
  (register-equipment! [s eq]
    (swap! a assoc-in [:equipment (:equipment-id eq)] eq) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:sites {} :operators {} :equipment {}
                                     :records [] :ledger []}
                                    seed)))))
