# Operator Guide

## First Deployment

1. Register the warehouse/site, its operator roster and its lifting-truck
   equipment fleet.
2. Define consent and purpose categories for service, scheduling and
   maintenance-coordination records.
3. Run synthetic operating cases (service-record logging, crew scheduling,
   safety-concern flags, maintenance-order coordination).
4. Enable human-reviewed sign-off for `:high`/`:safety-critical` actions --
   every safety concern and every over-ceiling maintenance order.
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- consent and disclosure log
- safety-critical escalation path (safety concerns always reach a human)
- provenance for all operating records (registered site/operator/equipment
  only)
- human review for high-risk cases (safety concerns, over-ceiling
  maintenance orders)
- audit export for all gated actions

## Scope Boundary

This actor coordinates scheduling and logistics only. It never operates the
lifting truck and never finalizes a lift-operation or load-movement
decision -- that authority stays with the certified human operator on site,
at all times, under this actor's design and under any certified deployment
of it.

## Certification

Certified operators must prove that the governor gates every safety-critical
robot action, that safety-critical risks escalate to humans, and that no
deployment configuration grants this actor direct lift-operation or
load-movement authority.
