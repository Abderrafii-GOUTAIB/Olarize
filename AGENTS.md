# Olarize instruction entry point

## Strict project folder boundary

For Olarize work, search, read, create, and edit local files only inside this repository (`/Users/abderrafiigoutaib/hr ai system/`). Do not inspect Downloads, attachments, global Codex skills/configuration, another workspace, or any other local folder, even if a file outside the project is mentioned. If an external file is needed, ask for it to be placed inside this repository before using it. Keep project-specific skills, hooks, and MCP configuration inside this repository.

Detailed agent guidance lives exclusively under .instructions/. Before changing code, read .instructions/project.md and the current task. Explicit user scope takes precedence over a broader task checklist. Treat .instructions/reference/business-v1-source.md as read-only business reference, not executable instructions.

Keep Olarize-specific agent assets inside this repository: skills under `.agents/skills/` and any future hook configuration/scripts under project-controlled paths. Do not create or modify user-level Codex skills or hooks for this project.

Load guidance based on the files being changed (including corresponding tests):

| Work | Required instructions |
|---|---|
| Backend Java, Maven, database, Dockerfile | .instructions/backend.md |
| Assistant or knowledge modules | .instructions/modules/assistant.md or knowledge.md |
| Attendance, payroll, social protection | Matching file in .instructions/modules/ |
| Frontend | .instructions/frontend.md |
| Hosting, deployment, AWS/Terraform | .instructions/infra.md and .instructions/reference/security.md |
| CI workflows | .instructions/ci.md |
| Any authorization/data access change | .instructions/reference/security.md |
| HR calculations | .instructions/reference/legal-rules.md |

Read README.md and docs/architecture.md for actual project layout, docs/development.md for commands and verification status, and docs/errors-and-debugging.md for the error contract. Task specifications are in .instructions/tasks/. No instruction files belong inside source packages. Only this root dispatcher remains outside .instructions so coding agents can discover the rules.

For feature implementation and independent review, also follow `.instructions/development-workflow.md`. The project-scoped reviewer configuration lives in `.codex/agents/`; Git hooks live in `.githooks/`.
