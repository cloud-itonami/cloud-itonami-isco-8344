# Contributing

`cloud-itonami-isco-8344` accepts contributions to the OSS actor, policy
tests, documentation, examples and open occupation blueprint.

## Development

```bash
kbb -M:test
```

Keep changes small and include tests for policy, audit, store or disclosure
behavior.

## Rules

- Do not commit real site, operator or equipment data, credentials or
  operating documents.
- Keep production writes and disclosures behind LiftingTruckOpsGovernor.
- Treat this occupation's workflows as high-risk (real physical-safety
  stakes downstream): add tests for permission, purpose, scope-exclusion,
  safety and audit logging.
- Never widen the closed op-allowlist to include anything resembling
  operating the lifting truck or finalizing a lift-operation/load-movement
  decision.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
