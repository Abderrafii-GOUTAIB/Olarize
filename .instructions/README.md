# Olarize development guidance

This folder contains coding-agent rules, business references and task specifications. The root AGENTS.md explicitly routes agents here; these files are not automatically loaded just because they exist.

- project.md: shared working rules and scope.
- backend.md, frontend.md, infra.md, ci.md: area-specific guidance.
- modules/: specialized rules for AI, RAG, attendance, payroll and contributions.
- reference/: product, security, legal-rule and measurement requirements; original business source preserved.
- tasks/: foundation and first AI workflow specifications.
- CHANGELOG.md: history of the original instruction kit.

Runtime source is in backend/. Project documentation is in docs/. There are no prompt files inside application packages.

## Using these files for one feature

1. Describe one observable feature, its user role and expected result. If there is no task file yet, use `tasks/TEMPLATE.md` to record the contract and acceptance checks under `tasks/`.
2. Ask the coding agent to implement that task in this project. Root `AGENTS.md` routes it to `project.md` and only the area, module and reference files relevant to the work. The instruction files are guidance; they are not commands and do not contain the application's business logic.
3. Put the implementation in `backend/src/main/java/com/olarize/<owning-module>/` (and `frontend/` when a UI exists). Put tests beside the corresponding source tree, migrations in `backend/src/main/resources/db/migration/`, and update the task with real verification results.

The project-local Codex skill at `.agents/skills/olarize-feature/SKILL.md` implements one backend feature from business rules you provide. Invoke it with a concrete request such as: `Use $olarize-feature. Business rule: HR can list contracts expiring within a requested date range for their company; ordinary employees cannot. Implement the backend with unit and integration tests. Do not add AI.` The skill follows the task and instructions, writes the code and tests, and runs backend verification. It does not invent the business rules or authorize production deployment. No project hooks are configured yet; add them inside this repository only when a specific workflow needs them.
