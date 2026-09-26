"""Generate the human-readable instruction map from its explicit, reviewed catalogue."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
entries=[]
def add(path,group,name,purpose,when,example,stages,mode='Guidance; not executable'):
    assert (ROOT/path).is_file(), path
    entries.append(dict(path=path,group=group,name=name,purpose=purpose,when=when,example=example,stages=stages,mode=mode))
add('AGENTS.md','Start here','The entry point','Directs the coding agent to the right project instructions and enforces your project-folder boundary. Its filename does not mean it is an agent.','At the beginning of repository work.','For an employee API, it directs the agent to backend and security guidance.',['rules','stories','build','review'])
add('.instructions/README.md','Start here','The instruction index','Explains how the instruction folders and three skills fit together.','When you need to find guidance or understand how to invoke a skill.','Look here for the story-skill and feature-skill workflow.',['rules'])
add('.instructions/project.md','Start here','Shared engineering rules','Defines product scope, modular architecture, SOLID, useful design patterns, safety boundaries and the definition of done.','For every implementation task, together with its specific requirements.','An agent keeps business rules in the domain and avoids adding unnecessary interfaces.',['rules','stories','build','review'])
add('.instructions/development-workflow.md','Start here','The delivery procedure','Defines implementation → checks → independent review → fixes and the handoff to your decision. Also explains shared workbench runs.','When implementing or reviewing a feature.','The developer fixes findings; the reviewer reports findings without editing.',['rules','build','tests','review','publish'])
add('.instructions/CHANGELOG.md','Start here','History of the instruction kit','Records why the original instruction pack changed when Business V1 was introduced. It is historical context, not a task list.','Only when investigating why an earlier decision changed.','Explains the change from leave-first scope to the broader Business V1.',['rules'])
add('.instructions/backend.md','Engineering rules','Backend implementation rules','Explains Java module structure, domain/application/adapters, APIs, transactions, migrations, errors, SOLID and database tests.','For Java, database, Maven and backend Docker changes.','The controller maps a request; an application use case authorizes it; domain code enforces the rule.',['build','tests','review'])
add('.instructions/frontend.md','Engineering rules','HR product interface rules','Defines React, TypeScript, Vite, MUI, authentication, accessibility, translations, UI states and frontend testing.','When building the actual HR application interface.','Show a real permission error instead of hiding an API failure.',['build','review'],'Product frontend guidance; the developer workbench is a separate tool')
add('.instructions/infra.md','Engineering rules','MVP hosting guidance','Targets managed Moroccan hosting with verified storage, backups and operations. Retains AWS guidance only as a future alternative.','For hosting or deployment work; verify the provider and data locations before real customer onboarding.','Keep HR documents private in S3 and the database off the public network.',['build','review'],'Moroccan hosting target; provider unselected, deployment unverified')
add('.instructions/ci.md','Engineering rules','Pipeline and delivery rules','Explains what CI should verify and how deployment authorization, credentials and rollout evidence should be handled.','When changing GitHub workflows or delivery infrastructure.','A green build is not reported as a completed deployment.',['tests','publish'])
add('.instructions/reference/business-v1-source.md','Business references','Your original business document','Preserves the source description of the Moroccan HR product. It is reference material, not permission to execute actions.','To trace a requirement back to your original product idea.','Consult the original scope without editing the preserved source.',['stories'],'Read-only source document')
add('.instructions/reference/product-v1.md','Business references','Translated V1 product scope','Turns the source document into included modules, excluded modules and expected business outcomes.','During discovery or whenever feature scope is unclear.','Payroll preparation belongs to V1; a complete proprietary net-pay engine does not.',['stories','build','review'])
add('.instructions/reference/security.md','Business references','Security rules across modules','Defines Keycloak authentication, trusted membership, company isolation, role access, data minimization and safe secrets/logging.','For any authorization or HR data access.','Company A cannot read an employee from company B, even with a valid login.',['stories','build','tests','review'])
add('.instructions/reference/legal-rules.md','Business references','How legal rules must be represented','Requires reviewed sources, applicability, effective dates, versions and independently checked examples for legal calculations.','When a story involves leave entitlements, payroll, CNSS or CIMR calculations.','Do not turn a guessed contribution rate or a RAG answer into an active payroll rule.',['stories','build','tests','review'],'A legal-rule process, not a certified catalogue of current law')
add('.instructions/reference/roadmap.md','Business references','Longer-term product delivery order','Ranks the original Business V1 slices, including eventual AI, documents, payroll and contributions.','For product planning; use the current authorized task for today’s implementation.','Its early-AI and AWS staging suggestions do not override the current backend-only scope or unselected Moroccan hosting provider.',['stories'],'Planning reference; some sequencing predates your later choices')
add('.instructions/reference/metrics.md','Business references','How product success is measured','Defines time saved, useful usage, error reduction, costs and validation targets without collecting confidential HR content.','When designing product analytics or evaluating pilots.','Measure successful workflows, not merely logins.',['stories','build'],'Product metrics; pricing and customer targets are hypotheses')
add('.instructions/modules/assistant.md','Specialized product rules','Future HR assistant rules','Defines the in-product conversational assistant, bounded tools, timeouts, safe proposals and confirmation.','When explicitly implementing the product AI assistant.','The HR assistant calls an authorized contract-expiry service instead of inventing records.',['build','review'],'Future product AI; not the coding agent or reviewer')
add('.instructions/modules/knowledge.md','Specialized product rules','Future RAG rules','Defines document extraction, chunking, embeddings, permissions, publication versions and real citations.','When explicitly building the knowledge/RAG module.','Retrieve only approved company policies and cite the original passage.',['build','tests','review'],'Future product RAG; not required to generate application code')
add('.instructions/modules/attendance.md','Specialized product rules','Attendance-specific rules','Defines schedules, manual/CSV input, corrections, absence classification and payroll-facing summaries.','For attendance features.','A missing clock-in must remain unknown rather than automatically becoming misconduct.',['stories','build','tests','review'])
add('.instructions/modules/payroll.md','Specialized product rules','Payroll-preparation rules','Defines periods, versioned inputs, validation, batch review, export and reconciliation.','For payroll-preparation features.','A prepared payroll batch is not the same thing as a paid salary.',['stories','build','tests','review'])
add('.instructions/modules/socialprotection.md','Specialized product rules','CNSS and CIMR rules','Defines scheme enrollment, reviewed contribution configuration, calculations and declaration/payment evidence.','For social-protection features.','Calculate from approved effective-dated rules and distinguish employee and employer portions.',['stories','build','tests','review'])
add('.instructions/tasks/TEMPLATE.md','Task contracts','A blank task contract','Provides a reusable structure for outcome, scope, acceptance checks and verification evidence.','When a new task needs a written implementation contract.','Describe a single feature and what will prove that it works.',['stories','build'])
add('.instructions/tasks/001-foundation.md','Task contracts','Original foundation task','Defines backend, Docker, database, CI and a planned React shell. The document records the narrower implementation scope.','For foundation work and its remaining acceptance items.','Backend setup does not mean the frontend shell is already delivered.',['build','tests'],'Historical task with remaining scope; not a command')
add('.instructions/tasks/002-first-ai-workflow.md','Task contracts','Planned first product AI workflow','Specifies the eventual authenticated question about contracts expiring next month.','When that AI slice is explicitly selected.','Authentication is a prerequisite; conversation and company memberships are separate deliverables.',['build','tests'],'Planned product work; current feature skill excludes AI')
add('.instructions/tasks/003-connected-developer-workbench.md','Task contracts','Development-tool implementation task','Tracks the dashboard/service work that coordinates stories, agents, checks, human review and PRs.','When changing the developer workbench itself.','Its verification distinguishes service tests from unverified live provider execution.',['build','tests'],'Developer tooling, not HR application scope')
add('.agents/skills/olarize-module-stories/SKILL.md','Skills','Specify a module','A reusable procedure for drafting the full module backlog, rules, sources, dependencies and Given/When/Then scenarios; it requires your approval before Jira publication.','When you select a module that needs stories.','Employees → draft stories → your approval → deduplicated OLARIZE Jira backlog stories.',['stories','approve'],'Instructions executed by an agent; cannot run by itself')
add('.agents/skills/olarize-feature/SKILL.md','Skills','Implement one backend story','A reusable procedure for implementing one agreed backend slice with authorization, unit tests and PostgreSQL integration tests.','After one story and its dependencies/scope are selected.','EMP-001 becomes Java code, migration if needed, and security tests.',['build','tests'],'Backend-only implementation skill; product AI excluded')
add('.agents/skills/olarize-review/SKILL.md','Skills','Review the implementation','A reusable review procedure that checks acceptance criteria, security, architecture, migrations and meaningful test evidence.','After stable implementation and verification.','Report a missing cross-company check with its triggering scenario and file location.',['review'],'Findings-only review; no code edits or automatic merge')
add('.codex/agents/olarize-reviewer.toml','Agent and connector setup','Named reviewer role','Defines a project-local reviewer identity and instructions, with a read-only sandbox default, using the review skill.','When a Codex client supports selecting this custom agent role.','A separate reviewer session examines the developer’s diff.',['review'],'Role configuration; workbench currently starts its own separate read-only CLI review session')
add('.codex/config.toml','Agent and connector setup','Project-local Jira connection','Declares the Atlassian MCP server. MCP gives an agent a way to use external tools such as Jira when authenticated.','When the story skill reads or publishes approved Jira stories.','This URL is a connector declaration; it does not prove login or grant story approval.',['stories','approve'],'Configuration, not an agent, skill or credential')
add('.githooks/pre-commit','Automatic checks','Git pre-commit hook','Runs a fast check of staged whitespace and Git-detected conflict markers before a commit. It does not format Java or run all tests.','Automatically on git commit when the local hooks path is installed.','Reject staged trailing whitespace; leave your files unchanged.',['tests','publish'],'Executable hook; deterministic code, no AI')
add('scripts/install-hooks.sh','Automatic checks','Hook installer','Enables the versioned hook in this repository’s local Git configuration and refuses to replace another hook setup silently.','Once per clone, or when enabling the hook intentionally.','Point local core.hooksPath at .githooks.',['tests'],'Executable setup script; not a recurring hook')
add('scripts/verify-backend.sh','Automatic checks','Backend verification entry point','Connects Testcontainers to the selected Docker context and runs Maven clean verify.','After backend changes and during workbench verification.','Run the actual unit, integration and architecture checks against disposable PostgreSQL.',['tests'],'Executable test command; needs Java and Docker')
add('scripts/run-backend.sh','Automatic checks','Local backend launcher','Starts the backend for development, with the project configuration and optional local debugging.','When you want to use or debug the running HR API.','Start the API and test it through Swagger.',['build'],'Executable launcher; not part of automatic code review')
add('scripts/smoke-keycloak.py','Automatic checks','Authentication smoke-check helper','Exercises the project’s local authentication smoke-check flow.','For focused local Keycloak/authentication verification.','Use the documented auth setup to check actual integration, separately from ordinary unit tests.',['tests'],'Executable test helper; not a production authentication component')
add('.github/workflows/backend.yml','Automatic checks','Backend GitHub CI','Runs backend tests, Compose validation and a Docker build for matching pull requests and pushes to master/develop.','When matching changes reach GitHub.','A remote CI check independently verifies the submitted branch.',['publish'],'Automatic GitHub workflow; branch protection must be configured separately')
add('.github/workflows/workbench.yml','Automatic checks','Workbench GitHub CI','Runs the development tool’s service tests, JavaScript checks and interaction checks.','When matching workbench changes reach GitHub.','Changing the developer dashboard gets its own test pipeline.',['publish'],'Automatic GitHub workflow; not HR production deployment')
add('docs/requirements/employee.md','Requirements and architecture','Employee feature requirements','Contains the employee rules, story IDs, acceptance scenarios, dependencies, recorded approvals and Jira links.','When selecting, implementing or reviewing an employee story.','The feature skill receives EMP-001 from this document; the document is not application code.',['stories','approve','build','review'],'Documented requirements snapshot; remote Jira status needs verification')
add('docs/architecture.md','Requirements and architecture','Where application code belongs','Explains the modular monolith and inward dependency direction between domain, use cases and adapters.','When deciding where to put new business logic.','employment/internal/domain contains the pure rule; its web adapter maps HTTP.',['build','review'])
add('docs/development.md','Requirements and architecture','How to run and verify the project','Documents local startup, tests, profiles, Swagger, logging and verification results.','When running the app, debugging or checking what was actually verified.','Use the documented backend verification entry point.',['build','tests'])
add('docs/errors-and-debugging.md','Requirements and architecture','Error handling and debugging guide','Explains the HTTP error contract, safe logs, request correlation and debugger setup.','When implementing failures or diagnosing a request.','Return a sanitized error with a request ID.',['build','tests','review'])
add('developer-workbench/SPECIFICATION.md','Workbench','The agreed automation target','Defines the dashboard journey, branch/worktree rules, approvals, recovery and delivery milestones.','When deciding how the development accelerator should behave.','One story → one isolated branch → checks → human diff approval → draft PR.',['rules','build','publish'],'Target contract; not proof that all live integrations are validated')
add('developer-workbench/README.md','Workbench','Workbench launch and operating guide','Explains startup, local authentication, provider prerequisites, shared API usage and actual limits.','When opening or operating the development dashboard.','The file-based planner and connected service are different modes.',['rules','build','publish'])
add('developer-workbench/index.html','Workbench','Original manual planner','Displays an interactive process map with manual notes and copied prompts.','For planning or learning without a connected execution service.','Marking a step manually does not run tests or approve code.',['rules'],'UI code; manual mode')
add('developer-workbench/connected.html','Workbench','Connected dashboard screen','Provides story selection, approval, run controls, code changes, test/review evidence and PR controls.','When opened through the local service’s private launch URL.','Inspect the diff before pressing Approve this diff & create draft PR.',['stories','approve','build','tests','review','publish'],'UI code; requires the local service')
add('developer-workbench/connected.js','Workbench','Dashboard behavior','Calls the authenticated local API, renders run state and events, and sends your explicit actions.','While the connected dashboard is open.','The Start button requests a run; it does not itself implement Java code.',['build','review','publish'],'Executable browser code')
add('developer-workbench/service/workbench.py','Workbench','The workflow coordinator','Persists runs in SQLite, launches bounded Codex sessions, executes checks, schedules independent review and handles guarded PR publication.','When the local workbench service is running and a run is requested.','After implementation it starts a fresh reviewer session; it owns the sequence and retry limits.',['stories','approve','build','tests','review','publish'],'Executable service; separate Codex sessions, nested subagents disabled')
add('developer-workbench/start.sh','Workbench','Connected service launcher','Starts the project-local workflow service and prints a private launch URL.','When you want the connected dashboard rather than the manual planner.','The default keeps live CLI execution gated by the login-access permission.',['rules'],'Executable launcher')
add('developer-workbench/control.py','Workbench','Codex/terminal entry into the same runs','Uses the local service API, so terminal/Codex requests and the dashboard can refer to the same persisted run.','When operating an existing workbench run from Codex or the terminal.','Read run status or submit an answer without creating a duplicate branch.',['build','review','publish'],'Executable API client; not another agent')
add('developer-workbench/service/test_workbench.py','Workbench','Service tests','Checks SQLite state, Git snapshots, HTTP protection, approvals, deduplication and recovery with synthetic provider fixtures.','When changing the coordinator.','A stale revision must not be published as approved.',['tests'],'Tests; mocked providers do not establish live connectivity')
add('developer-workbench/test_connected.js','Workbench','Dashboard interaction checks','Checks screen wiring and interaction rules without launching a real browser.','When changing dashboard behavior.','An unsaved draft cannot be started or published.',['tests'],'Tests; not visual browser verification')

expected={str(p.relative_to(ROOT)) for p in (ROOT/'.instructions').rglob('*.md')}
expected|={str(p.relative_to(ROOT)) for p in (ROOT/'.agents').rglob('SKILL.md')}
assert expected <= {e['path'] for e in entries}, expected-{e['path'] for e in entries}
(ROOT/'developer-workbench/instruction-catalogue.js').write_text('const CATALOGUE = '+json.dumps(entries,ensure_ascii=True).replace('<','\\u003c')+';\n')
intro='''# Understanding Olarize's instructions and development tools

This is an explanatory map of the repository, not another implementation policy. You do not need to memorize or manually run every file. The agent reads the applicable guidance; you choose the feature and make the business/publication decisions.

## Two separate systems

**Olarize is the HR product:** the Spring Boot backend, database, Keycloak, future HR frontend and later product AI/RAG. **The development workshop builds that product:** Codex sessions, skills, instructions, hooks, GitHub checks and the workbench dashboard. The workshop is stored in the same repository for your convenience; it is not part of the HR runtime or its production deployment.

```mermaid
flowchart LR
  Request[Your feature request] --> Agent[Developer agent]
  Rules[Project instructions] -. guides .-> Agent
  Skill[Feature skill] -. procedure .-> Agent
  Story[Approved story] -. requirements .-> Agent
  Agent --> Code[HR application code and tests]
  Code --> Tests[Verification]
  Tests --> Review[Separate reviewer]
  Review --> Human[Your code review]
  Human --> PR[Draft PR and GitHub CI]
```

## Six terms, in plain language

| Term | Meaning | Does it execute itself? |
| --- | --- | --- |
| Instruction file | Rules the agent should follow | No |
| Skill | A reusable procedure for a type of task | No; an agent follows it |
| Agent | An AI execution session that can use tools to perform work | It runs after a user or coordinator starts it |
| Subagent | An agent delegated a bounded task by another agent | It has its own context; it is not a special HR module |
| Hook | A script triggered by an event, such as a Git commit | Yes, once installed and the event occurs |
| CI | Automated checks run by GitHub for configured events | Yes, when the configured GitHub event occurs |

MCP is the connection that exposes tools such as Jira to an agent. The workbench service is ordinary program code coordinating the workflow; it is not a master AI model. An agent does not need a different model to be a separate reviewer.

## What runs today versus what is planned

There are three project skills, one named reviewer configuration, and one Git pre-commit hook. There is no fleet of permanently running agents. When a subagent appears in a Codex conversation, it is a temporary delegated session. The one used during workbench development reviewed the tool's code; it is not another component added to Olarize.

Two review paths exist. In a Codex task, a client can use the project-local `olarize-reviewer` role and review skill. In the connected service, Python starts a separate read-only `codex exec` session with the review skill; it does not directly load the named reviewer TOML as its role, and sets `agents.enabled=false` to prevent nested delegation. Both intend independent review, but their configuration mechanisms differ.

The manual planner requires prompt copying and manual notes. The connected service contains the execution adapters and progress controls, but live Codex/Jira/GitHub execution has not been validated end to end. Local service checks are not evidence that external authentication or PR publication works.

The long-term source and roadmap discuss early AI and AWS. Your later explicit choices take priority: current feature development is backend-only, product AI is deferred for now, and the MVP hosting target is Morocco, with a provider still to be verified. Reading an AWS or RAG instruction file does not deploy or implement it. No employee-specific module instruction file exists; employee rules currently live in `docs/requirements/employee.md`.

## Walkthrough: implement trusted company access

1. You select EMP-001 and agree its scope and prerequisites. The story defines what success means; the instruction files do not invent the feature for you.
2. The developer starts with `AGENTS.md`, `project.md`, `backend.md`, `security.md`, the architecture guide and the selected story. Unrelated payroll/RAG/infrastructure guidance is not needed.
3. `olarize-feature` guides implementation: pure rules in `internal/domain`, use-case coordination and authorization in `internal/application`, HTTP in `internal/adapter/in/web`, persistence in `internal/adapter/out/persistence`.
4. The developer adds unit tests and real PostgreSQL integration tests, then runs `scripts/verify-backend.sh`. Test code lives in the backend test tree, not in skills.
5. A separate reviewer uses `olarize-review`. The developer resolves valid findings and reruns affected checks. The reviewer does not silently fix its own findings.
6. You inspect the diff and results. The connected workflow asks you to approve publication of that exact revision.
7. The Git pre-commit hook checks staged whitespace/conflict markers. GitHub CI runs its configured tests/build after the PR is submitted. A hook passing is not equivalent to tests passing; a PR existing is not equivalent to merging or deploying.

The branch plan is story branches from develop, story PRs into develop, releases into master, and hotfixes from master propagated back to develop. The service currently automates the normal story path; release/hotfix execution and production deployment remain separate work.

## What makes the result maintainable

Instructions create consistency, not guarantees. Clean domain boundaries, cohesive modules and explicit application interfaces reduce coupling. Unit/integration tests verify behavior; architecture checks detect prohibited dependencies. The reviewer adds another check, and you retain the publication decision. Runtime scalability still depends on actual database queries, capacity, background jobs and deployment choices—not on how many coding agents exist.

For now, use the smallest routine: select one approved story, let the developer implement it with tests, get independent review, inspect the diff and publish a PR. Understand this flow before adding more skills or agents. The workbench automates coordination; it should not become a requirement to understand or run the HR application.

## Complete instruction and tool catalogue

'''
lines=[intro]
for group in dict.fromkeys(e['group'] for e in entries):
    lines.append('### '+group+'\n\n| File | What it does | When it is used |\n| --- | --- | --- |\n')
    for e in entries:
        if e['group']==group:
            lines.append('| ['+e['path']+'](../'+e['path']+') | '+e['purpose']+' | '+e['when']+' |\n')
    lines.append('\n')
lines.append('Other workbench assets (`styles.css`, `connected.css`, `app.js`, `stories.js`, `refresh-stories.py`) style or support the interfaces and static story snapshot. They are application assets, not extra skills, agents or instructions. `instruction-guide.html`, its catalogue and builder are this explanatory guide only. Generated `.state/` data contains runs/artifacts; it is ignored by Git and is not an instruction folder.\n')
(ROOT/'docs/understanding-the-workflow.md').write_text(''.join(lines))
print('Generated a catalogue of %s files; every .instructions markdown file and project skill is covered.'%len(entries))
