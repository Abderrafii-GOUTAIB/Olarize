# Olarize story implementation and independent review

All project-specific files, instructions, skills, agent configurations and hooks stay inside this repository. This workflow concerns development agents; it does not add AI features or agents to the HR application.

## Responsibilities

- The main agent develops one approved story with `.agents/skills/olarize-feature/SKILL.md` and owns edits, tests and fixes.
- The separate `olarize-reviewer` agent follows `.agents/skills/olarize-review/SKILL.md`. Its configuration is `.codex/agents/olarize-reviewer.toml`; it inherits the model and reasoning settings and defaults to a read-only sandbox. Runtime permissions may override sandbox defaults, so the findings-only instructions remain mandatory.
- The Git pre-commit hook runs deterministic checks on staged changes. It neither invokes agents nor replaces tests or CI.

## Procedure

1. Use `olarize-module-stories` when requirements are missing. Obtain explicit story approval before creating OLARIZE Jira backlog stories. Publication does not start implementation.
2. Select one approved story and verify its dependencies. Before implementation with `olarize-feature`, create and switch to an action-based ticket branch: `feature/OLARIZE-<number>` for features or `fix/OLARIZE-<number>` for bug fixes (for example `feature/OLARIZE-18`), based on `develop` unless the user specifies otherwise. Resume an existing verified ticket branch when present, preserve existing changes, and ask for the ticket key if it cannot be resolved. For connected workbench runs, use the existing run to create or select this branch without duplicating sessions. The developer reads the project and relevant area guidance and implements the story using `olarize-feature`. The current feature workflow excludes product AI.
3. Run the required unit and Testcontainers integration tests through `./scripts/verify-backend.sh`. Record the actual commands, results and the revision/files they cover. Resolve failures before requesting final review; disclose environmental blockers.
4. After implementation stabilizes, spawn one separate reviewer agent. This instruction authorizes that bounded delegation for feature development. Prefer the configured `olarize-reviewer` role. If the client cannot select custom roles, give a separate agent the role file and review skill explicitly and report that fallback; do not claim the role's sandbox was applied. If delegation is unavailable, disclose that independent review remains pending.
5. Provide the reviewer with the approved story path/ID, linked rules, repository path, explicit diff scope and verified base revision when applicable, changed/untracked file list, and observed test results. Do not invent a base branch or include unrelated user edits. Pause edits to the reviewed files until the reviewer returns. Keep handoff and test artifacts inside the project and omit secrets.
6. The reviewer traces acceptance criteria and reports findings without changing files. It inspects evidence independently. For backend changes, it treats the JPA/DTO/mapper and clean-code rules in `.instructions/backend.md` as review gates and checks any documented native-SQL exception and its PostgreSQL tests. Since builds write artifacts and Testcontainers starts containers, it requests any additional checks from the developer instead of executing them under the read-only role.
7. The developer evaluates each finding against the story and code, fixes valid problems, and explains evidence for any rejected finding. Run the relevant tests after fixes and request a focused re-review. Avoid repeated unchanged full-suite runs or unbounded review loops; surface unresolved business decisions and verification gaps. For every feature or fix, update `docs/features-and-flows.md` so its visual map, current flow and verification guidance stay aligned with the implementation. If the change does not alter a diagram, update the relevant feature/status/check entry to show what changed and how to verify it.
8. For every API feature that should show sample results in local Swagger, include synthetic records in the persistent local database setup, not only Testcontainers fixtures. Keep seed scripts/migrations restricted to the local profile, make them idempotent or one-time Flyway migrations, and document the local identity and exact Swagger calls that return the seeded rows. Never seed shared/prod databases, commit passwords, or bypass authorization to make sample responses nonempty. If safe authorized sample access needs extra local identity setup, document that setup.
9. Report implementation, review findings/dispositions, actual test results and remaining limitations. Do not claim readiness with unresolved material findings or missing required checks. The user authorizes successful ticket-branch commits to push and create or reuse a PR into `develop` through the post-commit hook, using the Jira title and link. Commit creation itself still follows the current user request. Jira transitions, merge and deployment require their own authorization.

## Local checks and CI

Enable versioned hooks once per clone using `./scripts/install-hooks.sh`. This sets repository-local `core.hooksPath`; global configuration is not modified. The installer refuses to replace an existing hook path or ordinary pre-commit, commit-msg or post-commit hook.

`.githooks/pre-commit` requires a branch matching `feature/OLARIZE-<positive ticket number>` or `fix/OLARIZE-<positive ticket number>` and runs `git diff --cached --check` with explicit whitespace rules. It rejects newly introduced trailing whitespace, space-before-tab indentation errors, extra blank lines at EOF and Git-detected conflict markers. It supports partial staging and never rewrites or stages files. `.githooks/commit-msg` requires the first line to start with the active branch ticket and a nonempty description, such as `OLARIZE-18: add company provisioning`. Missing or different tickets block the commit. Detached HEAD and direct commits on `develop` or `master` are rejected. Successful checks are silent. Verify hook behavior with `python3 scripts/test-git-hooks.py`. This is whitespace hygiene, not a full Java formatter, secret scanner or test suite. No Java formatter is configured yet. Full unit, integration and architecture checks stay in backend verification and CI. Git hooks can be bypassed, so they are not a security or merge enforcement boundary. Required CI checks must be configured in repository branch protection separately.

## Usage

`Use $olarize-feature to implement the selected approved story. Follow .instructions/development-workflow.md, delegate independent review to olarize-reviewer, fix actionable findings, and report verification results. Do not add AI.`

Custom agents use the project-scoped standalone TOML format documented at https://developers.openai.com/codex/subagents. Start a fresh Codex task opened in this repository if a newly added role is not discovered. Creating this file alone does not launch a reviewer or schedule automatic reviews.

## Connected developer workbench

For work on the development accelerator, follow `developer-workbench/SPECIFICATION.md` and `.instructions/tasks/003-connected-developer-workbench.md`. The target uses `develop` for story branches/PRs and `master` for production releases, with hotfix propagation back into `develop`. Verify actual branches before acting. Human approval of the exact reviewed diff precedes PR publication. These are target requirements, not a claim that connected execution or branch protection is configured.

## Shared connected-run entry point

When developing through the connected workbench from Codex, use `python3 developer-workbench/control.py runs` to find the story's run, then use the same local API to start, answer or retry it. Do not independently create a second branch or developer session for a story already managed by the workbench. The API requires a running local service and its project-local session token. In a workbench-assigned agent stage, implement or review that stage only; the service owns transitions, test execution and reviewer scheduling. Do not recursively start another workbench run.

## Automatic publication

Before feature implementation, save the live Jira key, exact summary and browse URL in `.github/jira/<ticket>.json`. The post-commit hook runs `scripts/publish-pr.py`, pushes the action-based ticket branch, and creates or reuses its PR into `develop`. The PR title includes the ticket and Jira summary; its body links Jira. Publication failures preserve the local commit and allow an explicit retry. This user-authorized direct workflow does not require the connected workbench or another publication approval.
