# cloud-itonami-isco-8344

Open Occupation Blueprint for **ISCO-08 8344**: Lifting Truck Operators.

This repository designs a forkable OSS business for an independent warehouse-crew scheduling/logistics coordination practice: a scheduling/logistics robot manages lifting-truck (forklift) fleet service records, crew-shift scheduling and maintenance-order coordination under a governor-gated actor, so a warehouse keeps its own service and safety records instead of renting a closed fleet-management SaaS.

**Maturity: `:implemented`.** `src/liftingtruckops/` implements the
`LiftingTruckOpsActor` as a `langgraph.graph/state-graph`
(`liftingtruckops.actor`) wired to a `Warehouse Logistics Advisor`
(`liftingtruckops.advisor`) and an independent `LiftingTruckOpsGovernor`
(`liftingtruckops.governor`), following the itonami actor pattern
(ADR-2607011000 / ADR-2607121000): `:intake -> :advise -> :govern -> :decide
-+-> :commit (:ok?) +-> :request-approval (:escalate?, human-in-the-loop
interrupt) +-> :hold (:hard?)`. 27 tests / 76 assertions green
(`clojure -M:test`).

**This actor coordinates SCHEDULING and LOGISTICS ONLY. It never operates
the lifting truck and never finalizes a lift-operation or load-movement
decision** -- that authority stays with the human operator on site. The
closed four-op proposal allowlist (`:log-service-record`,
`:schedule-crew-operation`, `:flag-safety-concern`,
`:coordinate-maintenance-order`) has no op resembling operating a lift truck
or finalizing a load movement -- it is structurally absent, not merely
gated. In addition, a content-based scope-exclusion HARD block
independently catches a compromised advisor that tries to smuggle a
lift-operation/load-movement finalization, or an override of an operator's
on-site safety judgment, into the free-text rationale of an otherwise
allowlisted op -- phrased as finalization/execution ACTIONS (e.g. "initiate
the lift operation", "move the load"), never bare nouns ("lift", "load"),
to avoid false-tripping on the advisor's own default rationale text (see
`liftingtruckops.governor`'s namespace docstring and the dedicated
regression test in `governor-test` asserting the default mock advisor's
proposals for all four ops never self-trip it).

HARD invariants (always hold, never overridable): site provenance,
no-actuation (`:effect` must be `:propose`), the closed op-allowlist, a
registered equipment basis for any service-record or maintenance-order
proposal, a registered operator basis for any crew-scheduling proposal, and
the content-based scope-exclusion block above. Always-escalate (human
sign-off regardless of confidence, mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)): `:flag-safety-concern`
(never auto-commit-eligible) and any `:coordinate-maintenance-order` above
the equipment's registered maintenance-cost ceiling.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical/administrative domain work**. Here a
scheduling/logistics robot performs service-record logging, crew-shift
scheduling and maintenance-order coordination for a warehouse's lifting-truck
fleet under an actor that proposes actions and an independent **Lifting
Truck Ops Governor** that gates them. The governor never dispatches
hardware itself and never operates the lifting truck; `:high`/
`:safety-critical` actions (any equipment-defect/load-hazard/
operator-fatigue safety concern, and any maintenance order above the
equipment's registered cost ceiling) require human sign-off. This actor
does **not** operate the lifting truck -- the human operator's on-site
safety judgment during any actual lift or load movement is never
overridden or automated.

## Core Contract

```text
site service history + operator/equipment roster + maintenance policy
        |
        v
Warehouse Logistics Advisor -> Lifting Truck Ops Governor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
suppress an operating record, finalize a lift-operation/load-movement
decision, or override an operator's on-site safety judgment.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `8344`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
