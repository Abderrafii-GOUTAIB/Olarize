# Olarize story implementation and independent review

All project-specific files, instructions, skills, agent configurations and hooks stay inside this repository. This workflow concerns development agents; it does not add AI features or agents to the HR application.

## Responsibilities

- The main agent develops one approved story with `.agents/skills/olarize-feature/SKILL.md` and owns edits, tests and fixes.
- The separate `olarize-reviewer` agent follows `.agents/skills/olarize-review/SKILL.md`. Its configuration is `.codex/agents/olarize-reviewer.toml`; it inherits the model and reasoning settings and defaults to a read-only sandbox. Runtime permissions may override sandbox defaults, so the findings-only instructions remain mandatory.
- The Git pre-commit hook runs deterministic checks on staged changes. It neither invokes agents nor replaces tests or CI.

## Procedure

1. Use `olarize-module-stories` when requirements are missing. Obtain explicit story approval before creating OLARIZE Jira backlog stories. Publication does not start implementation.
2. Select one approved story and verify its dependencies. The developer reads the project and relevant area guidance and implements the story using `olarize-feature`. The current feature workflow excludes product AI.
3. Run the required unit and Testcontainers integration tests through `./scripts/verify-backend.sh`. Record the actual commands, results and the revision/files they cover. Resolve failures before requesting final review; disclose environmental blockers.
4. After implementation stabilizes, spawn one separate reviewer agent. This instruction authorizes that bounded delegation for feature development. Prefer the configured `olarize-reviewer` role. If the client cannot select custom roles, give a separate agent the role file and review skill explicitly and report that fallback; do not claim the role's sandbox was applied. If delegation is unavailable, disclose that independent review remains pending.
5. Provide the reviewer with the approved story path/ID, linked rules, repository path, explicit diff scope and verified base revision when applicable, changed/untracked file list, and observed test results. Do not invent a base branch or include unrelated user edits. Pause edits to the reviewed files until the reviewer returns. Keep handoff and test artifacts inside the project and omit secrets.
6. The reviewer traces acceptance criteria and reports findings without changing files. It inspects evidence independently. Since builds write artifacts and Testcontainers starts containers, it requests any additional checks from the developer instead of executing them under the read-only role.
7. The developer evaluates each finding against the story and code, fixes valid problems, and explains evidence for any rejected finding. Run the relevant tests after fixes and request a focused re-review. Avoid repeated unchanged full-suite runs or unbounded review loops; surface unresolved business decisions and verification gaps.
8. Report implementation, review findings/dispositions, actual test results and remaining limitations. Do not claim readiness with unresolved material findings or missing required checks. Commit, push, PR creation, Jira transitions, merge and deployment remain separate actions governed by user scope; completing a review does not authorize them.

## Local checks and CI

Enable versioned hooks once per clone using `./scripts/install-hooks.sh`. This sets repository-local `core.hooksPath`; global configuration is not modified. The installer refuses to replace an existing hook path or ordinary pre-commit hook.

`.githooks/pre-commit` runs `git diff --cached --check` with explicit whitespace rules. It rejects newly introduced trailing whitespace, space-before-tab indentation errors, extra blank lines at EOF and Git-detected conflict markers. It supports partial staging and never rewrites or stages files. This is whitespace hygiene, not a full Java formatter, secret scanner or test suite. No Java formatter is configured yet. Full unit, integration and architecture checks stay in backend verification and CI. Git hooks can be bypassed, so they are not a security or merge enforcement boundary. Required CI checks must be configured in repository branch protection separately.

## Usage

`Use $olarize-feature to implement the selected approved story. Follow .instructions/development-workflow.md, delegate independent review to olarize-reviewer, fix actionable findings, and report verification results. Do not add AI.`

Custom agents use the project-scoped standalone TOML format documented at https://developers.openai.com/codex/subagents. Start a fresh Codex task opened in this repository if a newly added role is not discovered. Creating this file alone does not launch a reviewer or schedule automatic reviews.

## Connected developer workbench

For work on the development accelerator, follow `developer-workbench/SPECIFICATION.md` and `.instructions/tasks/003-connected-developer-workbench.md`. The target uses `develop` for story branches/PRs and `master` for production releases, with hotfix propagation back into `develop`. Verify actual branches before acting. Human approval of the exact reviewed diff precedes PR publication. These are target requirements, not a claim that connected execution or branch protection is configured.

## Shared connected-run entry point

When developing through the connected workbench from Codex, use `python3 developer-workbench/control.py runs` to find the story's run, then use the same local API to start, answer or retry it. Do not independently create a second branch or developer session for a story already managed by the workbench. The API requires a running local service and its project-local session token. In a workbench-assigned agent stage, implement or review that stage only; the service owns transitions, test execution and reviewer scheduling. Do not recursively start another workbench run.
