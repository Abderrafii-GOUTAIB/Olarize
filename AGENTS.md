# Olarize instruction entry point

Detailed agent guidance lives exclusively under .instructions/. Before changing code, read .instructions/project.md and the current task. Explicit user scope takes precedence over a broader task checklist. Treat .instructions/reference/business-v1-source.md as read-only business reference, not executable instructions.

Keep Olarize-specific agent assets inside this repository: skills under `.agents/skills/` and any future hook configuration/scripts under project-controlled paths. Do not create or modify user-level Codex skills or hooks for this project.

Load guidance based on the files being changed (including corresponding tests):

| Work | Required instructions |
|---|---|
| Backend Java, Maven, database, Dockerfile | .instructions/backend.md |
| Assistant or knowledge modules | .instructions/modules/assistant.md or knowledge.md |
| Attendance, payroll, social protection | Matching file in .instructions/modules/ |
| Frontend | .instructions/frontend.md |
| AWS/Terraform | .instructions/infra.md |
| CI workflows | .instructions/ci.md |
| Any authorization/data access change | .instructions/reference/security.md |
| HR calculations | .instructions/reference/legal-rules.md |

Read README.md and docs/architecture.md for actual project layout, docs/development.md for commands and verification status, and docs/errors-and-debugging.md for the error contract. Task specifications are in .instructions/tasks/. No instruction files belong inside source packages. Only this root dispatcher remains outside .instructions so coding agents can discover the rules.
