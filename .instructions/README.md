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

The project-local Codex skill at `.agents/skills/olarize-feature/SKILL.md` implements one backend feature from business rules you provide. Invoke it with a concrete request such as: `Use $olarize-feature. Business rule: HR can list contracts expiring within a requested date range for their company; ordinary employees cannot. Implement the backend with unit and integration tests. Do not add AI.` The skill follows the task and instructions, writes the code and tests, and runs backend verification. It does not invent the business rules or authorize production deployment. A versioned pre-commit hook lives in `.githooks/`; enable it per clone with `./scripts/install-hooks.sh`. It checks staged whitespace and conflict markers.

For a module that has not been specified yet, first use the project-local `.agents/skills/olarize-module-stories/SKILL.md`: `Use $olarize-module-stories for the employee module. Create the business rules and user stories, and recommend the first story.` It writes a reviewable backlog under `docs/requirements/`. Then build one selected story: `Use $olarize-feature. Implement story EMP-001 from docs/requirements/employee.md with unit and integration tests. Do not add AI.` Replace the example ID and filename with the actual output; discovery alone does not implement the module.

The project-local Atlassian MCP server is declared in `.codex/config.toml`. For module discovery, the story skill maps all required V1 capabilities to development-ready user stories with Given/When/Then criteria, presents the complete draft for explicit user approval, and only then creates approved Story issues in the OLARIZE Jira backlog. It resolves the real project key and backlog workflow through MCP rather than assuming them. Invoke it with: `Use $olarize-module-stories for the employee module. Draft all required V1 user stories, show them for my approval, then publish approved stories to the OLARIZE Jira backlog. Return the Jira keys in development order.` Publishing does not start implementation. Give `$olarize-feature` one approved local story ID or Jira key at a time.

After a story is implemented, use `.agents/skills/olarize-review/SKILL.md` for an independent, findings-only review of its local diff or PR branch: `Use $olarize-review to review EMP-002 against docs/requirements/employee.md and the current diff. Do not modify code.` The review checks acceptance criteria, authorization, company isolation, migration safety and test evidence. Backend CI remains the automated merge gate; the skill does not approve, merge or update Jira.

The complete developer → verification → independent reviewer → fixes workflow is in [development-workflow.md](development-workflow.md). The project-local reviewer is configured in `.codex/agents/olarize-reviewer.toml` and follows the existing review skill.

The connected dashboard specification is `developer-workbench/SPECIFICATION.md`; its implementation entry point is `tasks/003-connected-developer-workbench.md`. This development tool remains separate from the HR frontend/backend.

For a plain-language explanation of every instruction and the workflow, read [Understanding the workflow](../docs/understanding-the-workflow.md) or open `developer-workbench/instruction-guide.html` for the interactive file map. This is explanatory documentation, not an additional set of implementation rules.
