# OLARIZE-8 verification and review evidence

Date: 2026-10-01. Branch: feature/OLARIZE-8. Base: a1eb4944c0c1d4d9c0146ad3631b95d26c68f3cb. Evidence covers the uncommitted implementation and tests on this branch.

| Observed command | Result |
|---|---|
| `./scripts/verify-backend.sh -Dtest=EmployeeAccountLinkChangeTests,CompanyContextIntegrationTests,ArchitectureTests` | 48 passed, zero failures/errors/skips; package succeeded. |
| `./scripts/verify-backend.sh -Dtest=CompanyContextIntegrationTests` after operator procedure fix | 46 passed, zero failures/errors/skips; package succeeded. |
| Final `./scripts/verify-backend.sh` | 138 passed, zero failures/errors/skips; BUILD SUCCESS and packaged backend, 56.346 s. |
| `git diff --check` | Passed. |

The first focused run found missing helper-owner policy-predicate EXECUTE grants; these were fixed without granting link admins HR reads. Temporary diagnostic logging was removed before passing verification.

Independent findings-only reviewer reviewed the stable feature and full 136-test initial evidence. It found one P2: no controlled provisioning path for identity.link.admin. Added V14 dedicated operator grant/revoke with immutable before/after permission audit, current versions, scoped exact retries, preserved unrelated permissions and restricted operator role tests. Focused re-review observed 46 focused / 138 final full passing tests and found no new actionable issues; original P2 resolved.

Persistent local smoke used the packaged jar with local profile and ephemeral HTTP ports against existing Compose PostgreSQL. V13/V909 and then V14 applied successfully; readback showed versions 13, 14 and 909 success=true, and synthetic company001/member020/employee070 LINKED/version1. Generated `/v3/api-docs` includes link inspect/change/own paths and REVOKE request example. Temporary backend processes stopped; no reset of local database volume.

No live authenticated Keycloak verification, browser flow, UI completion or production authority claim. OD-03 remains a production prerequisite. Generated logs were inspected during verification/review and removed; Surefire reports remain in ignored backend/target until the next clean build.
