# Governance

`cloud-itonami-isco-8344` is an OSS open-occupation blueprint. Governance covers
both code and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Warehouse Logistics Advisor cannot directly dispatch robot actions,
  operate the lifting truck, or finalize a lift-operation/load-movement
  decision.
- LiftingTruckOpsGovernor remains independent of the advisor.
- hard policy violations cannot be overridden by human approval.
- every commit, hold and approval path is auditable.
- real site/operator/equipment data stays outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or license
should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit, support and
data-flow review.

Certified operators can lose certification for:

- bypassing policy checks
- mishandling site/operator/equipment data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
- granting this actor direct lift-operation or load-movement authority
