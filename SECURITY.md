# Security Policy

This project handles lifting truck operators warehouse-coordination
workflows -- real physical-safety stakes (load drop, tip-over, pedestrian
collision) sit downstream of this actor's scheduling/logistics decisions.
Treat vulnerabilities as potentially high impact even when the demo data is
synthetic.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real site, operator or equipment data exposure
- authorization bypass
- LiftingTruckOpsGovernor bypass
- any path that lets this actor finalize a lift-operation/load-movement
  decision or override an operator's on-site safety judgment
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on site/operator/equipment data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real site/operator/equipment data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
