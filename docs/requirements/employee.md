# Employee module — Olarize V1 rules and stories

Status: **APPROVED requirements — EMP-001–016 and EMP-019 approved by the user on 2026-09-23; all 17 published to the verified OLARIZE backlog on 2026-09-23.** Revised **2026-09-23**, preserving the existing EMP-001–018 identifiers. Scope: Moroccan private companies, approximately 20–200 employees. This is the documented employee-module V1 backlog, including explicit shared-module dependencies, not an exhaustive employment-law catalogue or legal certification. Legal retrieval limitations are recorded below.

> **Approved prerequisite sequence — 2026-09-28:** Start with ORG-001 / OLARIZE-18, then existing EMP-001 / OLARIZE-1, IAM-001 / OLARIZE-19 and IAM-002 / OLARIZE-20 before EMP-002. See [organization and identity requirements](organization-identity.md). This supersedes older “EMP-001 first / independently buildable” handoff wording below. Organization owns the company schema; identity owns memberships and trusted context. EMP-001 retains its acceptance criteria and uses ORG-001’s restricted operational boundary and atomic audit for initial administrator provisioning/revocation. The four-ticket sequence is published and ranked above employee creation. Production grant authority remains OD-03.

## 1. Boundary and current state

The employee module provides reliable, company-scoped employee identities, employment facts, reporting relationships, contract facts and history. Its first useful product outcome is: **an authorized HR user asks which contracts expire next month and receives the actual scoped records with explicit dates**.

The independently buildable first story is EMP-001, trusted company access. The smallest useful employee slice then comprises EMP-002, EMP-004 and EMP-005; EMP-006 makes that slice available in the primary conversational workspace. Deliver this before completing the rest of the directory product.

### Evidence of existing implementation

| Observed repository evidence | Existing behavior | Remaining prerequisite |
| --- | --- | --- |
| [SecurityConfiguration](../../backend/src/main/java/com/olarize/platform/internal/security/SecurityConfiguration.java), [CurrentUserController](../../backend/src/main/java/com/olarize/identity/internal/adapter/in/web/CurrentUserController.java), [FoundationIntegrationTests](../../backend/src/test/java/com/olarize/integration/FoundationIntegrationTests.java) | Validated Keycloak authentication; `/api/v1/me` returns the authenticated subject; unimplemented HR routes are denied. | Database memberships, company authorization, employee/account links and all employee use cases. Token validity alone grants no HR role. |
| [V1 migration](../../backend/src/main/resources/db/migration/V1__enable_pgvector.sql) | Enables pgvector. | No employee, company, contract or audit business schema. |
| [Development guide](../development.md), [architecture](../architecture.md), [task 001](../../.instructions/tasks/001-foundation.md) | Backend foundation and documented verification commands. | Frontend foundation, document storage and AI integrations are pending. Previous test results are documentation evidence; tests were not rerun for this backlog. |
| [Task 002](../../.instructions/tasks/002-first-ai-workflow.md) | Authentication prerequisite completed; first expiry workflow planned. | EMP-001/002/004/005/006 refine its remaining scope rather than create a competing implementation task. |

No employee-specific guide exists under `.instructions/modules/`. This review builds on the existing `docs/requirements/employee.md`; its story IDs are retained. Existing authentication must be reused. Source inspection confirms there are only platform/identity adapters and the pgvector migration; it does not establish runtime verification in this turn.

### Actors and access

Permissions below are proposed application permissions resolved from database memberships; they are not claims that Keycloak already contains these roles. A person can hold several explicitly granted roles.

| Actor | Permitted employee scope | Restrictions |
| --- | --- | --- |
| HR administrator | Company employee/contract reads and edits, reviewed imports, reporting lines, history and private administrative fields. | No cross-company access; salary, employer costs and contributions require separate explicit payroll permissions. |
| Manager | Current direct reports' minimal work directory: name, employee reference, job, department, work contact and reporting line. | No private contact, identity documents, contract attachments, salary or whole-company expiry query. Indirect reports require a later explicit policy decision. |
| Employee | Own allowlisted record and employee-visible documents through an explicit account link. | No colleague directory or self-editing of employment/contract facts. |
| Payroll operator | Authorized roster projection and stable identifiers for its company. | No automatic employee editing rights or medical/personnel dossier access. |
| Identity administrator | Provision/revoke app memberships and verified account links. | This administrative capability alone does not grant HR-record access. Initial provisioning is controlled operational work, not self-registration. |
| Assistant | Calls the same authorized public services on behalf of the current user. | Has no independent role, company selection authority or authority to decide employment outcomes. |

### Ownership and dependencies

| Owner | Authoritative data / contract |
| --- | --- |
| `identity` | Company memberships, roles, trusted context and the subject-to-employee link. Keycloak owns login identity. Employees need not have a login. |
| `organization` | Company identity and, when introduced, department/site reference data. One selected validated company per operation; consolidated multi-entity management is outside V1. |
| `employment` | Employee ID/reference, minimum identity/contact facts, employment episodes, dated job/reporting facts, contract metadata and links to document IDs. |
| `documents` | Private file content, immutable versions, access enforcement, scan state, retention and download grants. Employment never stores a public file URL. |
| `payroll` | Compensation, bank/payment inputs, payroll periods and immutable preparation snapshots. |
| `socialprotection` | CNSS affiliation/registration, CIMR enrollment/plan facts and reviewed contribution configuration. The employee ID joins these facts through public APIs. |
| `attendance` / `leave` | Schedules, presence, absences, entitlement and decisions. An employee being employed does not mean present today. |
| `assistant` / `audit` | Conversation/tool orchestration and confirmed proposals / durable business change evidence. Audit capture starts with the first write; the later history story adds a review interface. |

Foreign keys and public APIs must preserve company ownership. No consumer queries another module's repositories. The employee screen may compose authorized projections; it does not create competing copies of salary or scheme records.

Included: create/read/correct records, basic contract facts and expiry, reporting lines, reviewed CSV onboarding, account linking, own-record access, administrative departure recording, private contract attachments, history and downstream roster facts.

Excluded: recruitment, public-sector profiles, performance/training, medical dossiers, biometrics, legal contract generation or e-signature certification, automated discipline/dismissal, automatic contract renewal/reclassification, entitlement or severance calculations, and gross-to-net payroll. CNSS/CIMR remain mandatory in the wider V1 through their owning module; excluding their calculations here does not defer them out of the product.

### Data contract and routine assumptions

- A record needs a server-generated immutable employee ID, trusted company ID, company-unique employee reference, legal/display name and employment start date. Support Unicode names without requiring a Western first/last-name split. Do not invent missing dates or generate placeholder national identifiers. Missing historical facts can be reported as import exceptions.
- Employee references are strings; preserve leading zeroes. Trim surrounding whitespace and compare case-insensitively within the company; keep the displayed value. Names and emails are not unique keys. The same real person at another employer has a separate company-owned record.
- Optional work facts: job title, department/site label or valid company reference, work email/phone. Private contact/address and identity metadata require OD-02 and a separately approved extension; they are not silently included in EMP-007. Collect only fields justified in the customer's reviewed processing scope. Do not require CIN, birth date, marital status, children, bank details or scheme membership merely to create a directory record.
- Dates are civil dates; relative periods use `Africa/Casablanca`, with an injected clock. Employment start and last employed day are inclusive. `UPCOMING`, `CURRENT` and `ENDED` are derived as of a date from a recorded employment episode; they are administrative labels, not legal findings or attendance states.
- Contract ID, employee/episode ID, recorded type, start date, optional recorded end date, source reference and version are distinct from employment episode dates. An expired recorded contract does not automatically end employment. Unknown type/end date is explicit; no guessed expiry. Legal review state is separate from data completeness.
- Every changed fact has a recorded-at timestamp and version. Job/manager/employment changes also have an effective date so later corrections do not rewrite previously frozen payroll evidence. Ordinary contact/name corrections retain history without implying a change to legal employment status.

### Decisions and release gates

The following need an accountable customer/product/legal decision at the indicated boundary. They do not block synthetic-data development of EMP-001–006.

| ID | Decision or prerequisite | Safe behavior until resolved |
| --- | --- | --- |
| OD-01 | Customer workforce categories, applicable agreements and exceptions: foreign workers, minors, agency staff, apprentices and special regimes. | Capture factual records without an eligibility/compliance verdict. Confirm supported population before a pilot; specialized workflows require further stories and legal review. |
| OD-02 | Additional required dossier fields and their purpose, lawful basis, recipients and employee information notice. | Minimum fields above only; sensitive extensions stay disabled. No blanket consent checkbox is assumed sufficient. |
| OD-03 | Who may receive HR/payroll/identity-administrator rights; how first memberships are provisioned and revoked. | No self-granted roles. Use explicit controlled provisioning with auditable actor and company; test fixtures are synthetic. A reviewed production provisioning/revocation procedure is required before real users. |
| OD-04 | Retention periods by data class, legal holds, rights handling and disposal across DB, files, audit, backups and conversations. | No guessed duration or ordinary hard-delete endpoint. A restricted, tested retention/deletion procedure and rights-response owner are mandatory production prerequisites in the shared privacy/documents work. Archiving alone does not satisfy this gate; indefinite retention is not the proposed policy. |
| OD-05 | Hosting/provider countries, subprocessors, controller/processor terms and applicable CNDP formalities/evidence. | No real personnel data sent to an unreviewed model/storage destination. Development can use synthetic data and deterministic stubs. |
| OD-06 | Rehire/concurrent employment episodes and whether pilot data contains them. | Preserve facts; return an explicit unsupported-case exception instead of merging/overwriting. EMP-017 becomes mandatory before onboarding a customer requiring rehire. Concurrent episodes require a further scoped story. |
| OD-07 | Contract review responsibility and current legal text, including applicable CDD/foreign-worker rules. | Records remain facts supplied by HR; no automated legality, probation, renewal, notice or deadline rule is activated. |

These are recorded open decisions, not invented company policies. The proposed direct-report access, CSV create-only behavior and date conventions are product choices that the user may revise before implementation.

**Approved product decisions (2026-09-23):** direct reports only; one primary manager; create-only, all-or-nothing imports; explicit employee references rather than name/email matching; inclusive employment dates; no former-employee portal; and EMP-019's single confirmed create action as the initial employee mutation in conversation. The user approved these product choices; they are not evidence of an existing customer policy. OD-01–07 remain separate customer/legal release gates. Approving stories does not attest that those gates have been met.

### V1 coverage matrix

Every minimum capability from product-v1 is mapped below. “External” means mandatory in its owning module, not removed from Olarize V1. A local story is complete only when its stated prerequisite works; a stub is insufficient for a production claim.

| Required capability / material workflow | Employee stories | Coverage / remaining dependency |
| --- | --- | --- |
| Trusted company scope, permission and revocation | EMP-001, EMP-011, EMP-013 | Required prerequisite; existing JWT validation reused. |
| Company-scoped directory and detail | EMP-002, EMP-003, EMP-007 | Create, find, correct and review facts; structured forms included. |
| Manager relationships | EMP-008, EMP-011, EMP-013 | Dated primary manager, direct-report projection, revocation and reassignment exceptions. |
| Employment facts and changes | EMP-002, EMP-007, EMP-013, EMP-015, EMP-016 | Identity, job/start/end dates, history and downstream versions. |
| Reviewed CSV import | EMP-009, EMP-010 | Mapping → preview/errors → explicit atomic commit → retry/result. |
| Contract facts and dates | EMP-004, EMP-007, EMP-013 | Versioned facts distinct from employment lifecycle; no legal validity decision. |
| Contract attachments and versions | EMP-014, EMP-012 | Required; depends on DOC-DEP below for private uploads and versions. |
| Contract expiry queries / next-month example | EMP-005, EMP-006 | Bounded service and early primary conversation, with explicit dates and errors. |
| Employee self-service: own facts and documents | EMP-011, EMP-012, EMP-014 | Verified own-record and document projections, conversation and accessible views. |
| Employee self-service: requests and status | EMP-011, EMP-016 | External leave/absence request, approval/cancellation and status stories remain required; employee links/roster are prerequisites, not a replacement for those stories. |
| AI: bounded reads, exact confirmed action | EMP-006, EMP-012, EMP-019 | Expiry, own facts and one bounded employee-create proposal; no autonomous employment decisions. |
| Audit and safe failure behavior | EMP-001–016, EMP-019 | Audit captured from first write, restricted history view, transaction rollback and sanitized errors. |
| Attendance / absent-today example | EMP-008, EMP-016 | External attendance owns schedules, manual/CSV records, corrections, validation, absence resolution and absent-today tool. |
| Leave and absences | EMP-008, EMP-011, EMP-016 | External leave owns reviewed rules, ledger, preview/submit/approval/cancellation and availability. |
| HR documents / approved corpus and cited RAG | EMP-014 | External documents/knowledge owns storage/access/versioning/policy approval/RAG; personnel attachments must never enter the shared policy index. |
| Payroll preparation / September variables / employer cost | EMP-016 | External payroll owns period inputs, variables, exceptions, review, frozen export, reconciliation and cost query. No compensation calculation here. |
| CNSS | EMP-016 | External socialprotection owns affiliation/applicability, reviewed contributions, employee/employer breakdown, declaration/payment evidence. Required wider V1. |
| CIMR | EMP-016 | External socialprotection owns enrollment/plan, reviewed contributions, employee/employer breakdown and reconciliation. Required wider V1. |
| Privacy, rights, retention and hosting | EMP-007, EMP-009, EMP-012, EMP-014, EMP-015 | PRIV-DEP and OD-02/04/05 below block affected live processing, not independent synthetic development. |
| Rehire | EMP-017 | Conditional; unsupported population cannot be silently imported as new identity. Decide OD-06 before pilot. |
| Directory export | EMP-018 | Optional; requires a concrete approved purpose. CSV import does not mandate this convenience. |
| Public-sector HR, recruitment, performance/training, autonomous dismissal, gross-to-net engine | None | Explicit product exclusions; not missing employee stories. |

### Shared prerequisites with acceptance evidence

These are dependency contracts, **not completed implementations or approved Jira stories**. The named owning module must draft its own independent stories before the dependent employee story starts. Record actual issue IDs here when available; none are assumed.

| Dependency | Required result / verification | Consumers / current state |
| --- | --- | --- |
| UI-DEP (task 001, frontend) | React/TypeScript/MUI shell, PKCE login, validated company selector using EMP-001, accessible routing/forms, 401/403/error rendering. No token persistence or cross-company cached data. Verify keyboard operation and company switch/revocation. | All structured views and EMP-006/012/019; pending. |
| DOC-DEP (documents) | Company/employee-scoped private upload, scan/quarantine, immutable versions, authorized retrieval and expiring grants, failure recovery. Reject foreign, unscanned or revoked files. Expose a public API, no direct repository coupling. | EMP-014; pending. |
| PRIV-DEP (privacy operations with identity/documents/assistant) | Accountable owner and reviewed purpose/notice, rights-response channel, per-class retention/holds/disposal, provider destinations and formalities. Verify removal from live stores/derived indexes and documented backup handling using synthetic data; no hard-coded retention rule. | All real-person processing; pending OD-02/04/05. This includes legally reviewed issuance/delivery of applicable hiring/work-card/cessation documents under EMP-LR-003/004. |
| PROV-DEP (identity operations) | Document controlled membership grants/revocation and verified account-link evidence with actor, company and reason. No public self-provisioning. Exercise revoked tokens and wrong-company targets. | EMP-001 defines procedure; EMP-011 uses it. Named production grant authority remains OD-03. |

ORG-001 / OLARIZE-18 supplies company identity before EMP-001; no employee or document module is needed. EMP-002 owns its minimal atomic audit persistence through a focused port/adapter if no audit service yet exists; a later history view does not postpone write auditing.

## 2. Rule catalogue

### Product and security rules

References: **P** = [product V1](../../.instructions/reference/product-v1.md); **S** = [security](../../.instructions/reference/security.md); **A** = [assistant](../../.instructions/modules/assistant.md); **D** = this proposed design; **H** = [legal-rule configuration](../../.instructions/reference/legal-rules.md). Source/version for P/S/A/H is the repository working tree inspected 2026-09-23; effective scope is proposed Olarize V1. “Required” means established project requirement; “proposed” identifies product design, not law; the user approved that design for EMP-001–016 and EMP-019 on 2026-09-23. Story numbers in tables expand to `EMP-NNN`.

| Rule ID | Statement | Type / applicability | Source; status | Stories |
| --- | --- | --- | --- | --- |
| EMP-BR-001 | Resolve authenticated subject and active database membership before choosing company scope. Recheck revoked membership on later requests. A header, URL, JWT role or model argument alone cannot confer membership. | Security; every access | S; required | 001–019 |
| EMP-BR-002 | Scope records, relationships, counts, searches, previews, jobs and documents to that company. Reject cross-company employee/manager/contract/document/account references without revealing their existence. | Security; every access | S; required | 001–019 |
| EMP-BR-003 | Apply the actor matrix to field projections as well as routes. Never expose payroll or private fields through aggregate, search, history or assistant output. | Security; all roles | S + D; required restriction, proposed projections | 003,005,006,008,011,012,014–018 |
| EMP-BR-004 | Stable employee ID is immutable; employee reference is unique per company, including ended records. Names/emails can repeat; never resolve a write by name alone or merge people automatically. | Product choice; employee records | P + D; proposed key semantics | 002,003,006,007,009,010,017,018 |
| EMP-BR-005 | Enforce the minimum/optional field contract above, Unicode support and explicit missing values. Reject invalid dates, blank required fields, unsupported fields and oversized inputs. Reject rather than truncate. | Product choice; data capture | D; proposed | 002,004,007,009,010 |
| EMP-BR-006 | Require authorized explicit confirmation for writes. Bind an AI proposal to actor, company, operation, exact payload, expiry and versions; chat text cannot execute it. Reads need no confirmation. | Security/product; mutations | P + A; required | 002,004,006–011,013,014,017,019 |
| EMP-BR-007 | Write business facts and audit evidence atomically; record actor, time, company, target/version, operation, reason when correcting, and correlation. Roll back if audit capture fails. Restricted history may retain necessary old/new facts; telemetry must not. | Security/product; all writes | S + D; required | 001,002,004,007–011,013–015,017,019 |
| EMP-BR-008 | Use optimistic versions and scoped idempotency keys for retriable writes. Same key/payload returns the prior outcome; different payload conflicts. Reject stale confirmation and concurrent lost updates. | Product; mutations/imports | Project instructions + D; required | 002,004,007,008,010,011,013,014,017,019 |
| EMP-BR-009 | Effective and recorded dates are distinct. Reject end before start; never silently shift dates. Correcting source facts preserves existing downstream snapshots and signals that later preparation needs review. | Product; employment history | H + D; required preservation, proposed date convention | 004,007,008,013,015–017 |
| EMP-BR-010 | Recording a contract type/date or uploading a scan does not certify validity, signature, eligibility or renewal. Missing end dates are unknown/open-ended, not infinite fixed-term contracts asserted to be valid. | Product/legal boundary; contracts | P + H + D; required | 004–006,009,010,014,017 |
| EMP-BR-011 | Expiry means a recorded end date in the inclusive requested interval. Return employee and contract IDs, dates, timezone, as-of and bounded pagination. Exclude null dates and superseded versions; disclose missing-date coverage separately. | Product; expiry reads | Task 002 + D; proposed query semantics | 005,006 |
| EMP-BR-012 | An employee has at most one current primary manager in this V1 design. Manager must belong to the same company; no self-manager or cycles. A reporting relationship does not itself grant the manager role. | Product/security; reporting lines | P + S + D; proposed primary-manager model | 003,008,012,013,016 |
| EMP-BR-013 | Imports are staged, mapped and previewed before confirmation. V1 import creates new minimum employee records only; duplicate references, updates and legal/financial fields are exceptions. A rejected batch creates no employee rows. | Product; CSV onboarding | P + D; proposed create-only scope | 009,010 |
| EMP-BR-014 | Bind preview to company, actor, file hash, mapping/schema version and expiry. Execute exactly that preview atomically only when all rows are valid; revalidate uniqueness and authorization. Bound file size/rows, treat formulas as untrusted text and never evaluate them. | Security/product; CSV | S + D; proposed atomic policy | 009,010,018 |
| EMP-BR-015 | Own-record access requires an explicitly verified subject-to-employee link within the company. No email/name matching. One active link per subject/company and per employee; revoke/relink explicitly with audit. | Security; self-service | S + D; proposed uniqueness | 011,012,013 |
| EMP-BR-016 | Recording a confirmed departure changes the episode facts, not a legal decision or payment. Contract expiry alone never triggers it. Ended employees leave current operational lists; historical access remains separately authorized and minimized. | Product; departure | P + D; required human decision | 003,013,015–017 |
| EMP-BR-017 | Document links reference private, authorized versions belonging to the same company and employee context. Reauthorize every download; role revocation invalidates new grants. Never index employee attachments in shared policy RAG. | Security; documents | S; required | 012,014,015 |
| EMP-BR-018 | Fail explicitly with sanitized problem details and request ID. Use 400 invalid input, 401 unauthenticated, 403 forbidden operation, scoped 404 for unavailable object, and proposed 409 for stale/duplicate conflict. Errors/logs contain no rejected confidential values. | Product/security; interfaces | [Error contract](../errors-and-debugging.md) + D; required shape, proposed new conflicts | 001–019 |
| EMP-BR-019 | Services own facts and authorization; tools are typed/allowlisted/bounded. Disambiguate names and periods; render missing/provider-failure states honestly. Retain safe outcome/usage metadata, not raw personnel prompts. | Product/security; AI | A + task 002; required | 006,012,019 |
| EMP-BR-020 | Supply effective-dated roster facts and source versions through a public employment API. Consumers determine entitlement, scheme applicability and payroll completeness; missing inputs are not zero or non-applicability. | Product; downstream consumers | P + payroll/socialprotection guides; required | 004,007,013,016,017 |
| EMP-BR-021 | Ordinary archive/end actions preserve history and are not deletion. Actual retention/disposal requires reviewed per-class policy, holds, current authorization and auditable execution through shared privacy operations. | Product/privacy; all employee data | S + H + D; required review, OD-04 unresolved | 009,010,013–015,018 |

### Legal evidence register

Legal rules below are **research findings requiring legal review for activation**. Their product consequences are conservative evidence/recordkeeping choices, not a claim that software can decide compliance. Each row inherits its source's direct URL, version and retrieval date below; no rate, entitlement, age threshold or deadline is activated by this backlog. Rechecking on 2026-09-23 retrieved L3 and the CNDP procedure. L1/L2 PDF retrieval failed; the prior draft's article findings are retained as unverified in this review. They must be checked against applicable current texts before legal-dependent stories are ready. Factual storage/security work does not implement those statutory conclusions.

| Source ID | Official source and version/effective-period evidence | Retrieval and review |
| --- | --- | --- |
| L1 | Ministry of Justice [Law 65.99, Labour Code PDF](https://adala.justice.gov.ma/api/uploads/2024/04/30/code%20du%20travail-1714463246806.pdf), promulgation 2003-09-11, **consolidated 2011-10-26**. Article 589 states commencement relative to publication; exact applicable effective intervals and subsequent amendments must be confirmed for production. The current [Adala social-law listing](https://adala.justice.gov.ma/fr/resources/420) still links this file; the 2024 upload path is not a 2024 consolidation. | 2026-09-23 recheck: Adala listing accessible; PDF retrieval timed out. Consolidation date/article findings above come from the prior draft and were not independently reverified here. A [CRI-hosted copy of the 2004 BO text](https://www.casainvest.ma/sites/default/files/2025-02/code_travail_fr.pdf) was accessible but text extraction was garbled. [SGG draft 032.26](https://www.sgg.gov.ma/Portals/1/PL/projet_loi_032.26.pdf) was retrieved; a draft is not proof of an enacted amendment. Applicable current text/effective intervals **need legal review**. |
| L2 | CNDP [Law 09-08 PDF](https://www.cndp.ma/wp-content/uploads/2023/11/Loi-09-08-Fr.pdf), Dahir 1-09-15 dated 2009-02-18, French BO 5714 dated 2009-03-05. Version: official published text, not a confirmed 2026 consolidation. | 2026-09-23 recheck: PDF retrieval timed out; article findings retained from the prior draft, not fully reverified. CNDP’s [security guidance](https://www.cndp.ma/securite-du-si/) reproduces arts. 23–24 and supports the security finding. Applicable basis, rights, transfers, exceptions and current effective text **need legal review**. |
| L3 | CNDP [Deliberation 298-AU-2014 PDF](https://www.cndp.ma/wp-content/uploads/2023/01/deliberation-n-298-AU-2014-11-04-2014.pdf), decision dated 2014-04-11, arts. 1–7,9–10,12. Current [HR notification procedure](https://www.cndp.ma/procedures-de-notification-process/) lists it with form F-113. | 2026-09-23; decision and current listing checked; customer applicability/formalities **need legal review**. No end date or superseding version established. |

| Rule ID | Legal finding | Type / employer and employee applicability | Source / review | Linked stories and product consequence |
| --- | --- | --- | --- | --- |
| EMP-LR-001 | The Code distinguishes indefinite, fixed-duration and specified-work contracts; fixed-duration use is conditional. | Statute; employers/employees within the Code, subject to sector/individual exceptions. | L1 arts. 16–17; needs legal review. | EMP-004/005: record type and evidence; no automatic legality or conversion decision. |
| EMP-LR-002 | Article 15 specifies formalities when a contract is written, including employee possession of a copy. | Statute; covered written employment contracts. | L1 art. 15; needs legal review. | EMP-004/014: track attachment/copy evidence; an upload does not certify formalities. |
| EMP-LR-003 | Work-card and written hiring-information duties exist; the work-card particulars depend on implementing regulation. | Statute; covered employers and employees. | L1 arts. 23–24 and cited decree 2-04-422; implementing text not independently verified, needs legal review. | EMP-002/007/014/016: expose factual inputs; reviewed templates/issuance belong to HR documents and remain a V1 dependency where applicable. |
| EMP-LR-004 | Cessation entails an employment-certificate duty with prescribed content and timing. | Statute; covered employers and departing employees. | L1 art. 72; needs legal review; no automatic deadline configured. | EMP-013/014/016: preserve entry/exit/job facts for a reviewed documents workflow. |
| EMP-LR-005 | Employment of foreign employees has authorization provisions and potentially applicable international agreements. | Statute; employers hiring foreign employees, subject to applicable exceptions. | L1 arts. 516–520; needs legal review. | EMP-004/014: retain reviewed evidence references when applicable; no eligibility verdict. OD-01/07 gate specialized support. |
| EMP-LR-006 | Personal-data processing must have defined purposes, proportionate accurate data and limited retention; information and access/rectification rights apply. | Statute; covered controllers processing identifiable employee data. | L2 arts. 2–5,7–8; needs legal review of customer application. | EMP-002/007/009/012/013/015: minimized fields, correction history and visible rights contact; OD-02/04 govern operation. |
| EMP-LR-007 | Security/processor obligations and conditions on transfers abroad apply. | Statute; covered employers/controllers and processors; employees whose data is processed/transferred. | L2 arts. 23–24,43–44; needs legal review. | EMP-001/006/010/014/018: isolation and restricted destinations; OD-05 must resolve live hosting/AI use. |
| EMP-LR-008 | CNDP provides an HR authorization model for qualifying private employers; the decision addresses purposes, data, recipients, retention, information and processing basis. Its listed fields are not a mandate to collect all of them. | Regulator decision; employers in L3 art. 1 and employees in art. 2. | L3 arts. 1–7,9–10,12 and current HR procedure; needs legal review. | EMP-002/007/012/014: customer-specific field/notice configuration and documented formalities before live use. |

Company policy inputs requiring later evidence include additional mandatory fields, wider manager visibility and retention schedules. No company policy was supplied or treated as law. Payroll/CNSS/CIMR parameters, probation, leave, severance and eligibility calculations are outside this catalogue and require their own reviewed rule packs.

## 3. Prioritized stories

**M0** = mandatory foundation/first conversational slice. **M1** = mandatory before claiming the employee capability complete for V1. **C** = conditional on actual customer workflow. **P2** = optional convenience. These are delivery recommendations; “mandatory” does not mean every story is directly required by statute.

EMP-001–016 and EMP-019 are approved requirements; EMP-017/018 remain draft. Every story includes the common contract here in addition to its specific scenarios:

- **Access:** Given no valid authentication, when calling the interface, then return 401; given valid authentication without permission, then return 403. Given an object from company B while operating in A, when reading/linking/mutating it, then return scoped unavailability and reveal no B facts, counts or mutation. Relationship joins and assistant paths receive the same tests.
- **Writes:** Given a preview or form awaiting confirmation, when canceled/unconfirmed, then no business change occurs. Given a stale version or lost membership before confirmation, when submitted, then fail without partial writes. Given a committed request retried with the same key/payload, then return the original outcome. Audit failure rolls back the write. Test these through the public API and real PostgreSQL.
- **Surfaces:** Backend stories are bounded API/service slices usable without AI. Structured views consume the same service. EMP-006 adds the initial conversational workspace; EMP-012 adds own-record conversation. EMP-019 supplies the first exact confirmed employee action. Other mutation tools remain outside this employee backlog; the structured authorized workflow is required for every included write.
- **Errors/testing:** Reuse the problem contract and synthetic fixtures. Unit tests exercise business decisions; integration tests exercise persistence, authorization, failure and concurrency. Frontend/tool stories additionally test rendered states and deterministic tool calls. An error is never rendered as success or as an empty roster.

- **Structured write surfaces:** Given an authorized user edits any included form/import/link, when reviewing then canceling, then no write occurs; when confirming valid data, then show the committed IDs/versions; when stale/invalid/denied, then show the real problem and require correction or renewed authorization. Test keyboard/focus, loading, success, field errors, conflict and retry. These UI requirements accompany the specific business use case, not a later unspecified UI project. UI-DEP is a prerequisite for every story with a structured screen.
- **Proposed bounded formats:** names/job/department/site labels allow Unicode up to 200 characters, references up to 64, email up to 254, phone up to 32, correction reason up to 1,000; no arbitrary dossier fields. Use ISO `YYYY-MM-DD` in API/CSV; format localized dates in UI. Contract type codes are `CDI`, `CDD`, `SPECIFIED_WORK`, `UNKNOWN`, representing supplied facts only. Department/site are labels in this slice; dedicated reference-data management is excluded. Read page size defaults to 25, maximum 100; expiry/roster windows at most 366 inclusive days, with longer requests split explicitly. Proposed import limits: 1 MiB, 1,000 data rows, one UTF-8 comma-delimited CSV with optional BOM, quoted-cell support, ISO dates and an explicit header map. Reject nonempty unmapped columns. Preview/proposal lifetime is 30 minutes; no expired artifact may be retrieved or executed. These are reviewable product/resource limits, not legal retention periods; source/artifact disposal is still PRIV-DEP. Production configuration can lower resource limits, must disclose them before upload, and cannot weaken authorization.
- **Legal readiness:** Requirements approval does not mark a story ready: verify its dependencies and applicable review gates before development/live use. Legal catalogue links describe contextual constraints; factual capture does not activate law. Any extension implementing a legal decision, document duty or production retention/transfer rule is withheld until applicable evidence is reviewed. Cross-module prerequisites must be satisfied before the dependent story is scheduled.

### EMP-001 — Resolve my authorized company context (M0; build first)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-1](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-1) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As an authenticated user, I want to see and select only my active company memberships so subsequent HR operations use the correct employer.

**Scope:** `identity` backend slice: membership schema consuming organization’s company ID/public contract from ORG-001 / OLARIZE-18, public trusted-context resolver and authenticated membership-list/context API. Reuse Keycloak and `/api/v1/me`. Excludes employee data, self-enrollment, role-management UI, frontend and AI. Controlled initial provisioning/revocation is documented, with no public bootstrap bypass.

**Preconditions/dependencies:** Existing authentication/database foundation and ORG-001 / OLARIZE-18; no employee or document module needed. **Permissions:** user reads own memberships; requested company is validated against the authenticated subject. **Rules:** EMP-BR-001/002/003/018, EMP-LR-007. **Data/state:** membership `(subject, company, explicit permissions, ACTIVE/REVOKED, version)`; selection is request context, not a global change to another user's session.

**Acceptance:**

- Given active memberships A and B and revoked C, when listing memberships, then return only A/B and their own authorized context; no employee data.
- Given A/B, when requesting context B, then resolve B from membership; when an ambiguous operation omits company, require selection rather than choose one silently.
- Given a malformed company identifier, when resolving it, then return 400; given no memberships, then return an empty membership list and deny HR scope.
- Given a valid user without A membership, when selecting A or supplying another subject, then deny access without disclosing A or the other subject's memberships.
- Given a previously active membership revoked in the database, when another request arrives with a still-valid token, then scope resolution fails.

**Tests:** unit membership selection/status rules; PostgreSQL/HTTP isolation, revocation and spoofed-subject cases using real signed-token test infrastructure. **Open decisions:** production provisioning procedure OD-03; it does not require rebuilding authentication or block the tested resolver.

### EMP-002 — Create a minimum employee record (M0)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-2](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-2) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As HR, I want to record an employee's identity and employment start so the person can be referenced reliably across HR workflows.

**Scope:** one explicit create action, accessible create/review form, persisted employee plus first employment episode, minimal response and atomic audit. Excludes accounts, contract validity, compensation and bulk import. **Preconditions/dependencies:** EMP-001; UI-DEP for the form; HR membership; minimum facts known. **Permissions:** HR create only. **Rules:** EMP-BR-001–009/018/020, EMP-LR-003/006/008. **State:** no record → employee and episode with version 1; current/upcoming is derived, not a hired-by-software decision.

**Acceptance:**

- Given valid minimum facts and HR confirmation, when creating, then return stable ID/reference, dates and version and persist audit once.
- Given a blank name, invalid civil date or unknown/oversized field, when creating, then return field constraint codes without echoing values and persist nothing.
- Given duplicate reference in the company, when creating, then conflict; the same reference in another company is allowed. Two equal names remain two employees.
- Given employee/manager role without HR create, when submitting, then deny; given a forged company field, then reject rather than write there.
- Given a future start date, when creating, then show UPCOMING as of today; do not activate a login or payroll eligibility.

**Tests:** unit normalization/date/missing-value rules; PostgreSQL uniqueness race, idempotency, rollback/audit and foreign-company payload cases. **Open decisions:** OD-01/02 constrain live data categories; no unsupported hiring-policy validation.

### EMP-003 — Find and inspect company employee records (M1)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-6](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-6) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As HR, I want a searchable, paginated directory and detail view so I can identify the correct employee before acting.

**Scope:** HR API and structured directory/detail; name/reference and status filters, explicit as-of date, stable ordering. Excludes bulk export and manager/private-field expansion. **Preconditions/dependencies:** EMP-002; frontend foundation for the view. **Permissions:** HR company read; separate projections arrive in EMP-008/012. **Rules:** EMP-BR-001–005/009/016/018. **State:** read-only.

**Acceptance:**

- Given two employees named Ahmed, when searching, then show both with stable references; never silently select one for a later operation.
- Given no match, when searching, then return an empty scoped result; invalid pagination/filter/date returns 400.
- Given ended records, when using the default current directory, then omit them and expose an explicit ended/all filter to HR.
- Given an ordinary employee requesting company search, when the directory or detail is requested, then deny; given B's employee ID from A, then return scoped 404 with no identifying detail.
- Given a database failure, when the directory request fails, then render a retryable error with request ID rather than an empty directory; pages have no foreign-company counts.

**Tests:** unit filters/projection/order; PostgreSQL pagination/status and count isolation; UI duplicate-name, loading, empty and error states. **Open decisions:** none beyond recorded defaults.

### EMP-004 — Record or correct contract facts (M0)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-3](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-3) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As HR, I want to record a contract's supplied facts so expiry queries use real dates with traceable origin.

**Scope:** API and accessible create/correction review form for one versioned contract record for an employee episode; type, start, optional end, source reference and review state. Correction supersedes a version, not a legal renewal. Excludes attachment storage, legal classification, signatures, renewal decisions and compensation. **Preconditions/dependencies:** EMP-002; UI-DEP. **Permissions:** HR contract write. **Rules:** EMP-BR-001/002/005–010/018/020, EMP-LR-001/002/005. **State:** absent → recorded v1; correction → v2 plus retained v1, reason and evidence reference. Review defaults to unreviewed; there is no automatic legally-valid state.

**Acceptance:**

- Given an employee in A, when HR confirms supplied contract facts, then store them and their source/version for A.
- Given end before start, invalid type code or unsupported field, when contract facts are submitted, then reject; a missing end is recorded as missing/open-ended with appropriate completeness, not invented.
- Given HR corrects an end date with current version and reason, when the correction commits, then expiry queries use the new version and historical evidence remains.
- Given non-HR access, when contract facts are submitted, then deny; given B's employee/contract reference, then reject the whole operation without exposing B.
- Given a recorded end passes, when employment and access state are resolved, then no employment episode or membership changes. A stale correction conflicts rather than overwrites.

**Tests:** unit dates/version transition/completeness; PostgreSQL company relation constraints, simultaneous corrections and audit rollback. **Open decisions:** OD-07 legal review; no rule activation is needed to store supplied facts.

### EMP-005 — List contracts with recorded expiry in a date range (M0)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-4](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-4) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As HR, I want the exact contracts ending in a specified interval so I can review upcoming work.

**Scope:** authorized `listExpiringContracts` public service and API; a bounded result with names/references, employee/contract IDs, recorded start/end, source version, date window, timezone and as-of. Excludes automatic notifications and mutation. **Preconditions/dependencies:** EMP-004. **Permissions:** HR contract read; ordinary employees and payroll-only users cannot list the company population. **Rules:** EMP-BR-001–003/010/011/018/019. **State:** read-only.

**Acceptance:**

- Given end dates on both boundaries and just outside, when requesting an inclusive range, then include only boundary/in-range current versions, sorted by end date then ID.
- Given an authorized empty range, when the expiry query runs, then return an explicit empty result; missing/inverted/invalid dates return 400, without an unbounded query.
- Given a CDI or incomplete record with no end date, when the expiry query runs, then exclude it from dated matches and return a scoped missing-date coverage indicator without asserting full completeness.
- Given an ordinary employee, when the expiry query runs, then deny; B's records never affect A's list, pagination or coverage count.
- Given multiple contracts for one employee, when the expiry query runs, then identify each separately; no silent per-person collapse or renewal inference.

**Tests:** unit inclusive windows, leap dates and deterministic ordering; PostgreSQL scoped joins/counts/pagination and corrected-version selection. **Open decisions:** none beyond approval of the common bounded formats; page/window limits are product limits, not law.

### EMP-006 — Ask which contracts expire next month (M0)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-5](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-5) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As HR, I want to ask the expiry question in conversation and review the actual service result without manual navigation.

**Scope:** one read-only allowlisted tool, conversation ownership, bounded provider adapter and conversational result card with accessible date controls/API fallback. Includes deterministic model-stub evaluation; live model validation is separate. Excludes RAG and write tools. **Preconditions/dependencies:** EMP-005; pending task-001 React foundation; assistant guidance; OD-05 before live data/provider use. **Permissions:** same HR contract read and trusted context as EMP-005; own conversation only. **Rules:** EMP-BR-001–003/006/010/011/018/019, EMP-LR-007. **State:** bounded request → authorized result, explicit denial, clarification or failure; no HR mutation.

**Acceptance:**

- Given the Casablanca clock at 2026-09-23, when asking in French, Arabic or English for next month's expiries, then resolve 2026-10-01 through 2026-10-31 and display the actual records and window.
- Given a December clock, when the expiry question is processed, then next month crosses the year correctly; use timezone rules rather than a fixed UTC offset.
- Given an ambiguous month without a resolvable year, when the expiry question is processed, then request clarification before querying; invalid tool arguments fail validation.
- Given ordinary employee access or model-supplied company/subject overrides, when the expiry question is processed, then reject or ignore the untrusted overrides and enforce the service boundary; B's conversation is inaccessible.
- Given zero matches, when the expiry question is processed, then say so; given provider timeout/tool failure, then show unavailable/error with structured fallback. Do not invent names/results or request confirmation for this read.

**Tests:** deterministic tool/argument, multilingual, prompt-injection, ownership and failure evaluations; PostgreSQL-to-card integration and UI states. Measure task outcome/latency/usage without payload logging. **Open decisions:** provider configuration and frontend foundation remain explicit dependencies. `$olarize-feature` is backend-only and cannot alone complete this story.

### EMP-007 — Correct employee administrative and job facts (M1)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-7](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-7) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As HR, I want to correct an employee's recorded details with a reason so other workflows use accurate facts and prior decisions remain explainable.

**Scope:** API and accessible correction/review form for one employee, versioned name/work-contact/reference corrections, employment-start correction and dated job/department changes. Private address/contact/identity extensions are excluded until OD-02 provides exact field definitions and a separately approved story; salary, bank, scheme records, health data, end-date edits and account linking are excluded. **Preconditions/dependencies:** EMP-002; UI-DEP; minimum work fields only. **Permissions:** HR write/private read; minimal directory projection excludes private fields. **Rules:** EMP-BR-001–009/018/020, EMP-LR-003/006/008. **State:** current version → corrected version/history, with effective date for employment-related facts.

**Acceptance:**

- Given current version and reason, when HR confirms a name/contact correction or dated job change, then store the new facts and restricted history.
- Given invalid date/contact syntax, blank mandatory name or duplicate employee reference, when the correction is submitted, then reject without truncation or partial change.
- Given same-name employees, when correcting, then require an employee ID; do not rename or merge another record by inference.
- Given an employment-start correction, when confirmed with reason/current version, then reject if it would place an existing job/reporting interval or recorded last day before the start or overlap another episode; otherwise version the start. Never shift contract dates or payroll snapshots automatically. Return the dependent-fact conflicts for separate correction.
- Given manager/employee write or attempts to change payroll fields, when the correction is submitted, then deny/reject; B's department/employee references cannot be linked.
- Given a previous payroll snapshot, when job data is corrected retroactively, then preserve the snapshot and make changed source versions visible for a later review.

**Tests:** unit correction/field allowlist/effective-date decisions; PostgreSQL stale writes/reference uniqueness/history isolation and source-version preservation. **Open decisions:** none for the minimum work-field slice. OD-02 blocks additional private/identity fields, not this correction behavior; no hard-coded CIN/passport assumptions.

### EMP-008 — Assign a primary manager and view direct reports (M1)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-9](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-9) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As HR, I want a dated reporting relationship so authorized managers can view the correct direct-report directory.

**Scope:** API and accessible assignment/review form to assign/change/remove one primary manager, plus a minimal direct-report view. Excludes approval delegation, indirect-team access and organization-chart editing. **Preconditions/dependencies:** EMP-002; manager role membership and verified employee link from EMP-011 for manager reads. Assignment can be built before account linking. **Permissions:** HR assignment; manager role plus own employee link for report read. **Rules:** EMP-BR-001–003/007–009/012/018. **State:** effective relationship absent/current → new dated relationship; prior relationship retained.

**Acceptance:**

- Given HR assigns M to E, when the effective date arrives, then linked manager M sees E's permitted work fields.
- Given self-reference, cycle, overlapping primary assignments or invalid date, when the reporting assignment is submitted, then reject. Concurrent opposing assignments must not create a cycle.
- Given an ended manager on the requested effective date, then reject new assignment; when HR removes a manager without a replacement, then store an explicit unassigned state with a null manager reference.
- Given employee/non-HR assignment attempt, when the reporting assignment is submitted, then deny; B's manager cannot be assigned to A's employee.
- Given removal/reassignment, when the old manager next queries, then E is absent even in a previously open conversation. Being named manager never grants the role itself.

**Tests:** unit graph/effective-date/projection rules; PostgreSQL concurrent cycle prevention, cross-company joins and immediate read-scope change. **Open decisions:** broader management/delegation policies are deferred, not implicitly granted.

### EMP-009 — Preview a CSV employee import (M1)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-10](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-10) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As HR, I want to review mapped CSV rows and errors before creating employees so spreadsheet mistakes do not become live HR records.

**Scope:** accessible upload/mapping/preview screen and API for UTF-8 CSV minimum employee fields from EMP-002, explicit header mapping, row validation, duplicate detection and private staged preview. Excludes updates, contracts, managers, private identifiers, payroll and automatic commit. **Preconditions/dependencies:** EMP-002; restricted staging storage owned by this slice and the common technical limits/expiry; no general documents service is needed for a small private CSV preview. **Permissions:** HR import; actor/company owns the preview. **Rules:** EMP-BR-001–005/007/013/014/018/021, EMP-LR-006/007. **State:** upload → VALID or INVALID preview; no employee records created.

**Acceptance:**

- Given a valid mapped file, when previewing, then show row count, exact proposed values and hash/version; no employees yet exist.
- Given duplicate references within file/in company, malformed encoding, invalid dates, ambiguous date formats or unknown columns, when the file is previewed, then show bounded row/field issues and prevent commit.
- Given references with leading zeroes, commas in quoted Unicode names or empty optional cells, when the file is previewed, then preserve their intended values under the published format.
- Given unauthorized role, when the file is previewed, then deny; B cannot inspect A's preview or infer duplicate references there.
- Given formula-like cells or oversized upload, when the file is previewed, then never evaluate content; reject unsafe/unsupported input explicitly. On staging failure, return a failure and no usable preview.

**Tests:** unit parser/mapping/validation with adversarial synthetic CSV; integration staging access, scoped duplicate lookup, expiry and no employee writes. **Open decisions:** the common bounded formats define size/row limits and preview lifetime; source disposal/retention requires OD-04 before real data use.

### EMP-010 — Confirm and commit a reviewed CSV import (M1)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-11](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-11) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As HR, I want the reviewed valid CSV batch created exactly once so onboarding a roster is predictable and traceable.

**Scope:** authenticated review/confirm/result screen and API for one immutable valid preview; atomic employee creation and batch/row audit. Excludes partial acceptance, updates and silent row skipping. **Preconditions/dependencies:** EMP-009; valid unexpired preview owned by caller/company. **Permissions:** current HR import/create. **Rules:** EMP-BR-001/002/004–009/013/014/018/021. **State:** VALID preview → COMMITTED with result IDs; invalid/stale previews cannot transition.

**Acceptance:**

- Given all rows valid, when explicitly confirming the preview, then create every row and return batch-to-employee mappings with one committed result.
- Given an invalid/expired preview or changed file/mapping, when commit is requested, then reject and require a fresh preview.
- Given a duplicate reference created after preview, when commit is requested, then commit none of the rows and explain the conflict without confidential values in telemetry.
- Given permission revoked after preview, when commit is requested, then deny; given another company's/user's preview ID, then expose no preview or result.
- Given timeout after commit, when retrying the same confirmed batch, then return the existing result; injected row/audit failure rolls back the entire batch.

**Tests:** unit batch transitions/key binding; real PostgreSQL uniqueness races, rollback, timeout/retry and cross-company batch isolation. **Open decisions:** none; partial imports are a later product decision.

### EMP-011 — Link a verified account to an employee (M1)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-8](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-8) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As an identity administrator, I want to link a verified app membership to its employee record so self-service uses an established identity.

**Scope:** one verified link, explicit revoke/relink and audit; controlled administrative API/procedure. Excludes invitations, email delivery, identity-provider user creation and changing HR facts. **Preconditions/dependencies:** EMP-001/002; verified membership/employee references supplied through the documented provisioning procedure. **Permissions:** dedicated identity-link administration; no self-link endpoint or automatic HR privilege. **Rules:** EMP-BR-001–003/006–008/015/018. **State:** UNLINKED → LINKED → REVOKED; a replacement requires explicit confirmation and records prior link.

**Acceptance:**

- Given verified active membership and employee in A, when administrator confirms the link, then own-record resolution returns that employee ID only.
- Given unknown/revoked membership, duplicate active link or malformed target, when linking is requested, then reject; matching names/emails alone cannot satisfy verification.
- Given HR without link-administration capability or an employee linking themself, when linking is requested, then deny.
- Given B's membership or employee, when linking is requested, then reject the relation without exposing the foreign record.
- Given a revoked/replaced link, when an old session next requests self-service, then old employee access fails; no historical conversation grants it back.

**Tests:** unit uniqueness/status rules; PostgreSQL relation constraints, concurrent linking, revocation and spoofed-subject HTTP calls. **Open decisions:** production identity-verification evidence is OD-03; no employee self-assertion is accepted as proof.

### EMP-012 — View my employee facts and privacy information (M1)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-12](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-12) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As an employee, I want to see my permitted recorded facts and the correction contact so I can identify errors without accessing anyone else's dossier.

**Scope:** own-record API, accessible structured view and bounded own-record conversational read; name/reference, own work/contact facts, dates, job/manager, own recorded contract metadata and approved privacy notice/contact. Employee-visible documents compose EMP-014. Excludes editing, internal HR reasons, audit actors, payroll and colleague searches. **Preconditions/dependencies:** EMP-011/004/006; versioned customer notice/rights contact reviewed under OD-02/04. **Permissions:** active employee membership/link, own projection only. **Rules:** EMP-BR-001–003/015/017–019, EMP-LR-006/008. **State:** read-only; opening a notice is not a consent or legal-waiver event.

**Acceptance:**

- Given linked employee E, when asking for own job/contract dates or opening the profile, then show E's recorded facts with as-of and correction contact.
- Given no link, when own-record access is requested, then show access not configured; invalid parameters fail instead of falling back to another employee.
- Given an ordinary employee asks about Ahmed or supplies a different ID, when own-record access is requested, then refuse colleague access, including through follow-up chat.
- Given B's IDs/conversation, when own-record access is requested, then return no data; role changes and link revocation apply before the next read.
- Given model failure, when own-record access is requested, then the structured own-profile remains usable. Missing metadata is shown as unknown; employee-visible fields never include private HR annotations or contribution totals.

**Tests:** unit own-projection rules; PostgreSQL current link/ownership isolation, deterministic assistant refusal/follow-up tests and UI unavailable/error/accessibility states. **Open decisions:** notice and rights-response process are release prerequisites; no automated request portal is assumed necessary.

### EMP-013 — Record a human-confirmed departure (M1)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-13](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-13) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As HR, I want to record an already decided last employed day so operational rosters stop treating the employee as currently employed while preserving history.

**Scope:** API and accessible review/confirm form to record/correct one episode's last employed day and evidence reference with explicit confirmation. Show affected manager relationships/account access as review items. Excludes dismissal decision-making, severance, payments, legal document generation and rehiring. **Preconditions/dependencies:** EMP-002/007; EMP-011 for linked access behavior. **Permissions:** HR employment lifecycle write; no manager/assistant autonomous decision. **Rules:** EMP-BR-001–003/006–009/012/015/016/018/020/021, EMP-LR-004/006. **State:** open episode → recorded end; ENDED only after the last employed day; mistaken end correction creates a new version.

**Acceptance:**

- Given evidence of a human decision and a valid last employed day, when HR confirms, then store the end/version/reason and derive CURRENT on that day, ENDED the next Casablanca day.
- Given end before start or stale version, when the departure is submitted, then reject without changes. Future departures remain visible as scheduled facts.
- Given an expired contract without a departure action, when the current employment roster is requested, then keep the employment episode unchanged.
- Given non-HR or B's employee ID, when the departure is submitted, then deny/scoped-404 with no lifecycle or account change.
- Given the end has taken effect, when roster or employment-derived access is requested, then exclude the employee from current operational rosters and deny employment-derived self-service/manager access. Preserve separately granted HR/admin memberships; do not delete the global Keycloak user. Existing links do not silently restore access after an end-date correction: administrator review is required.
- Given the departing person manages others, when the recorded end takes effect, then mark those relationships unavailable for current manager access and report them for HR reassignment; never promote another manager automatically.

**Tests:** unit inclusive dates/clock transitions and correction rules; PostgreSQL historical preservation, linked access after end, reporting-scope change, retries and audit rollback. **Open decisions:** former-employee portal is outside V1; a reviewed external rights/document-delivery channel remains required under OD-04.

### EMP-014 — Attach and retrieve a private contract document version (M1)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-14](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-14) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As HR, I want to link a private document version to a contract so authorized people can inspect its evidence without sharing public files.

**Scope:** accessible attachment/version list and link/retrieve API for employment-to-document linking, replacing the current reference while preserving older authorized versions, listing and controlled retrieval. Excludes implementing the whole storage/scanning service, medical documents, signature certification and shared RAG. **Preconditions/dependencies:** EMP-004; documents module provides private upload, scan/quarantine, immutable versions, lifecycle and current authorization. **Permissions:** HR link/read; linked employee only versions explicitly marked employee-visible; managers/payroll-only users denied. **Rules:** EMP-BR-001–003/006–008/010/017/018/021, EMP-LR-002/003/004/005/007/008. **State:** unlinked → linked document/version; replacement retains previous version and evidence history.

**Acceptance:**

- Given an available clean private version for the employee, when HR confirms linking, then show the evidence reference and allow authorized retrieval.
- Given quarantined, missing, unsupported or mismatched document/version, when linking or retrieval is requested, then reject the link and expose no content.
- Given ordinary manager access or employee access to an HR-only version, when linking or retrieval is requested, then deny; B's file cannot be linked or downloaded from A.
- Given a permission revoke, when a new download grant is requested, then no new download grant is issued; signed grants have a documented short lifetime and are never placed in AI prompts or logs.
- Given storage failure, when retrieval is attempted, then show document unavailable while preserving contract metadata; replacing a scan does not silently overwrite old evidence or certify legal validity.

**Tests:** unit visibility/version-link rules; PostgreSQL cross-company links/audit, storage adapter failure and expired/revoked authorization integration; distinguish stub storage from live-provider verification. **Open decisions:** OD-04/05 and shared documents prerequisites; no fake attachment feature can satisfy this story.

### EMP-015 — Review restricted employee change history (M1)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-15](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-15) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As HR, I want to inspect who changed employee facts, when and why so corrections can be investigated.

**Scope:** paginated company/employee history view over audit captured by prior stories, version differences and source/import references. Excludes broad staff surveillance, raw prompts, secret values, medical content and history editing. **Preconditions/dependencies:** EMP-002/004/007 audit evidence; frontend foundation. **Permissions:** explicit HR history read and field-level visibility; employee/manager roles cannot read internal history. **Rules:** EMP-BR-001–003/007/009/017/018/021, EMP-LR-006/007. **State:** read-only; audit retention follows OD-04 rather than “keep forever.”

**Acceptance:**

- Given create, correction and import events, when HR opens history, then show ordered actor/time/reason/correlation/source versions with allowed differences.
- Given invalid filter/version, when the history interface is called, then reject; a valid record with no later changes shows creation only rather than fabricated edits.
- Given manager/employee read or any attempt to alter audit records, when the history interface is called, then deny.
- Given B's target or foreign document reference, when the history interface is called, then reveal neither event metadata nor file content.
- Given restricted/removed data under a reviewed retention action, when the history interface is called, then show only permitted event metadata; never recover erased content from UI logs or embedded payloads.

**Tests:** unit diff/redaction/projection; PostgreSQL pagination/field and company isolation, no audit mutation API, safe error/log capture. **Open decisions:** OD-04 determines allowed audit retention and restricted access; capture was mandatory before this view existed.

### EMP-016 — Supply a versioned employment roster for a period (M1)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-16](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-16) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As an authorized payroll operator, I want employee facts overlapping a preparation period so payroll can assemble and reconcile its own roster.

**Scope:** public employment projection with employee/episode IDs, reference, employment and job-date intervals, relevant contract IDs/source versions and missing-fact indicators. Excludes payroll calculation, eligibility decisions, CNSS/CIMR defaults and payroll's snapshot storage. **Preconditions/dependencies:** EMP-002/004/007/013; payroll consumer stores its own immutable snapshot. **Permissions:** explicit company payroll-roster read; bounded consumer context always carries trusted company. **Rules:** EMP-BR-001–003/009/010/016/018/020, EMP-LR-003/004. **State:** read-only versioned projection as of a specified period and retrieval time.

**Acceptance:**

- Given a joiner, leaver, future joiner and established employee, when requesting a period, then return only episodes overlapping its inclusive dates, including a mid-period leaver and relevant dated job facts.
- Given reversed/invalid period, when the period roster is requested, then reject; missing contract details remain explicit exceptions and never become zero contributions or assumed non-applicability.
- Given employee/manager access, when the period roster is requested, then deny; A's roster/counts never include B even through joins or a background consumer.
- Given source correction after a frozen payroll snapshot, when the period roster is requested, then return new versions to a new request while the old consumer snapshot remains unchanged.
- Given a consumer timeout, when the roster read is retried, then retrying the read causes no employment mutation; consumer authorization does not grant private contacts, salary or documents.

**Tests:** unit interval overlap/leap/boundary and projection rules; PostgreSQL consistent-read/source versions, correction isolation and consumer contract tests. **Open decisions:** exact payroll completeness rules belong to payroll, not this API.

### EMP-017 — Record a returning employee's new episode (C)

**Review status:** DRAFT — awaiting explicit user approval. **Jira key / URL:** not published — outside the approved 17-story scope.

**Story:** As HR, I want to record a verified returning employee without losing the previous employment period.

**Scope:** append a new non-overlapping employment episode to a deliberately selected existing company employee; new contract records use EMP-004. Excludes concurrent episodes, automatic identity merging, seniority calculations and automatic access restoration. **Preconditions/dependencies:** EMP-013/016; pilot requires rehire under OD-06. **Permissions:** HR lifecycle write. **Rules:** EMP-BR-001/002/004/006–010/015/016/018/020. **State:** retained ended episode + new upcoming/current episode, same immutable employee ID.

**Acceptance:**

- Given an ended episode and verified returning employee ID, when HR confirms a later start, then append a distinct episode and preserve historical contracts/snapshots.
- Given overlap, invalid date or unresolved same-name identity, when the new episode is confirmed, then reject/require selection without merging records.
- Given non-HR or B's employee, when the new episode is confirmed, then deny without changing either population.
- Given repeat confirmation, when the episode write is retried, then create one episode; prior account access remains unavailable until explicitly reviewed/relinked.

**Tests:** unit interval/identity rules; PostgreSQL concurrent creation/idempotency, history, scope and roster integration. **Open decisions:** confirm supported rehire policy before build; any continuity-of-service calculation needs its own reviewed legal rules. Mandatory for a customer needing this workflow, otherwise defer.

### EMP-018 — Export an authorized minimal employee directory (P2)

**Review status:** DRAFT — awaiting explicit user approval. **Jira key / URL:** not published — outside the approved 17-story scope.

**Story:** As HR, I want an explicit minimal directory export for an approved administrative task.

**Scope:** reviewed column/filter selection and private bounded CSV download; minimal work fields only. Excludes full dossiers, salary, scheme data and automatic external delivery. **Preconditions/dependencies:** EMP-003; approved purpose, retention and private download facility. **Permissions:** separate HR export capability, checked again at download. **Rules:** EMP-BR-001–005/014/018/021, EMP-LR-006/007. **State:** requested → prepared → expired artifact with access audit; employee records unchanged.

**Acceptance:**

- Given allowed columns and scope, when explicitly requesting export, then produce only those company records/columns and record access evidence.
- Given unsupported/private column or invalid filter, when export is requested, then reject without preparing a broader export.
- Given unauthorized role or B's export ID, when export is requested, then deny without counts/content.
- Given formula-like name/reference, when the export is rendered, then neutralize spreadsheet execution while preserving a documented text representation; expired/revoked download grants fail.

**Tests:** unit column/filter/formula rules; integration scope, private retrieval, expiry and permission revocation. **Open decisions:** approve a concrete export purpose; import requirements do not imply export is mandatory V1 coverage.

### EMP-019 — Prepare and confirm one employee creation in conversation (M1)

**Review status:** APPROVED requirements by user on 2026-09-23; dependencies/release gates remain applicable. **Jira key / URL:** [OLARIZE-17](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-17) — published and verified in board 34 backlog, status À faire, no sprint (2026-09-23).

**Story:** As HR, I want to prepare an employee record from supplied facts in conversation and confirm the exact review card so I can complete routine data entry without the assistant deciding to hire someone.

**Scope:** one typed `prepareEmployeeCreation` tool, durable proposal and authenticated confirmation endpoint calling EMP-002; accessible review/edit/cancel/result card and structured create fallback. Includes only EMP-002's minimum/work fields. Excludes bulk import, account grants, contract/legal decisions, departure, arbitrary mutation tools and automatic execution from chat text. **Preconditions/dependencies:** EMP-002, EMP-006, UI-DEP; approved provider destinations/retention before live personnel data. No generic multi-action workflow is required. **Permissions:** HR create for the current trusted company, checked at prepare and confirmation; own proposal/conversation only. **Rules:** EMP-BR-001–008/018/019/021; legal context EMP-LR-006/007/008, with no law activated. **Data/state:** proposal binds actor, company, operation, exact normalized payload/hash, schema version, expiry and idempotency key; `PENDING → EXECUTED`, `CANCELED` or `EXPIRED`. Changed data produces a new proposal and invalidates the old one. The service revalidates uniqueness and permission at execution; server receipt before expiry does not bypass transactional checks.

**Acceptance:**

- Given HR supplies minimum facts for an already decided employment, when preparing, then show exact name/reference/start/work fields and company without creating an employee; when the same HR user confirms that card, then create one record through EMP-002 and show verified IDs/version.
- Given missing facts, malformed dates, unsupported fields or ambiguous references, when preparing, then request correction without inventing values or creating a usable proposal. Duplicate names alone are allowed; reference uniqueness remains authoritative.
- Given ordinary employee/manager access or HR permission revoked after prepare, when preparing/confirming, then deny without a write. Given another user's or company's proposal/conversation ID, then expose no payload and execute nothing.
- Given chat says “confirmed,” a document contains tool instructions, or the model supplies actor/company overrides, when processed, then no mutation occurs; only the authenticated confirmation endpoint can execute the owned proposal with trusted scope.
- Given cancel, expiry, payload edits or a newly conflicting reference, when confirming the original proposal, then reject with no partial write and require a fresh review. The card clearly distinguishes proposed facts from committed facts.
- Given a timeout after the business transaction commits, when retrying confirmation, then return the original result without duplicates. Given proposal/audit persistence failure, when executing, then roll back the employee and proposal transition atomically; model failure leaves the structured form usable and never produces a fabricated success.

**Tests:** unit proposal transitions/payload binding/expiry; real PostgreSQL/HTTP ownership, permission revocation, uniqueness race, atomic rollback and idempotent retries; deterministic model tests for multilingual input, injection and false confirmation; UI card edit/cancel/confirm/recovery and keyboard tests. Live-provider evaluation remains separate. **Open decisions:** one-action scope and 30-minute lifetime approved on 2026-09-23; OD-04/05 gate live data. Assistant/frontend work requires explicit implementation scope beyond the current backend-only feature skill.

## 4. Delivery recommendation and handoff

### Mandatory story list

| Priority | Story | Why build it |
| --- | --- | --- |
| M0 | EMP-001 — Trusted company context | Prerequisite for every company-owned operation; authentication already exists. |
| M0 | EMP-002 — Create minimum employee | Stable identity and actual persisted data. |
| M0 | EMP-004 — Contract facts | Supplies the first operational question with real source facts. |
| M0 | EMP-005 — Expiry query service | Deterministic, authorized answer independent of a model. |
| M0 | EMP-006 — Conversational expiry query | Delivers the primary interaction early, as required by task 002. |
| M1 | EMP-003 — Directory and detail | Find/review employees safely. |
| M1 | EMP-007 — Corrections and dated job facts | Maintain accurate records without erasing evidence. |
| M1 | EMP-011 — Verified account linking | Reliable self-service identity. |
| M1 | EMP-008 — Reporting relationships | Required manager relationships and constrained team access. |
| M1 | EMP-009 — CSV preview | Review real onboarding data before writes. |
| M1 | EMP-010 — CSV commit | Complete the required reviewed-import workflow. |
| M1 | EMP-012 — Own-record access | Required employee self-service. |
| M1 | EMP-013 — Departure facts | Keep current and historical populations accurate. |
| M1 | EMP-014 — Private contract evidence | Required contract attachments and controlled access. |
| M1 | EMP-015 — Change-history review | Make business changes investigable; audit capture starts earlier. |
| M1 | EMP-016 — Period roster contract | Supplies downstream payroll and social-protection integration safely. |
| M1 | EMP-019 — Confirmed conversational creation | Completes an exact employee action through the primary interaction. |

Recommended order: **001 → 002 → 004 → 005 → 006**, then **003 → 007 → 011 → 008 → 009 → 010 → 012 → 013 → 014 → 015 → 016 → 019**. This is a serial recommendation, not a requirement to wait on unrelated modules: UI-DEP must precede the first structured form in 002; private documents must precede 014. Audit capture accompanies each write. EMP-019 may move immediately after 006 once its proposal/security prerequisites are met. If real pilot data requires returning employees, include **017** before importing/using that population. **018** can wait.

Completion of the 17 mandatory stories is employee-module coverage, not full Olarize V1. Required cross-module/release dependencies remain: reviewed privacy/retention/deletion and provisioning procedures; documents storage and applicable work-card/hiring-information/certificate workflows; leave/attendance; payroll preparation; reviewed CNSS and CIMR capabilities; other modules' confirmed transactional AI workflows and full pilot verification. Missing reviews block the affected live processing or legal automation, not independent factual/synthetic-data development. All external V1 workflows in the matrix still need their owning-module backlog; this document does not claim they have been drafted or delivered.

### Approval and publication register

**EMP-001–016 and EMP-019** were explicitly approved on 2026-09-23 by the user (“yes i approve”) in response to the 17-required-story review. **EMP-017/018** remain draft and are not approved. All 17 approved Stories were published through project-local Atlassian MCP and verified on 2026-09-23. Project identity: Olarize / OLARIZE, project ID 10033, cloud ID 1aec8f44-2f88-4a2b-a142-355798d76fa4. Create permission was verified, Story type ID 10040 was available, and searches for all 17 stable IDs returned no existing issues before creation. Board 34 (OLARIZE board) is the verified backlog. All issues read back as Story, project OLARIZE, status À faire (10037), and appeared in the backlog query with `sprint IS EMPTY`. Read-back descriptions matched the prepared content after normalizing Markdown bullets/whitespace. No existing issues were modified. The per-story status lines above are the authoritative slots for subsequent approval scope and Jira linkage.

Recommended first implementation: **EMP-001**, the independently buildable trusted-context slice. The 17 required stories and stated product choices have been approved. Behavioral changes require revision/reapproval. EMP-017 remains conditional on OD-06; EMP-018 is withheld pending an approved export purpose. Unreviewed legal behavior/private-field extensions are withheld, even if the overall backlog is approved. For dependency-bound stories, approval is requirements approval and does not mark them ready for development or live operation.

After approval, use only project-configured Atlassian MCP: verify exact OLARIZE project identity/key, create permission, Story type and actual backlog representation; deduplicate stable IDs; publish one issue per approved publishable story with expanded shared acceptance/rule/source content and dependencies; verify backlog placement and no active sprint; read back content and record actual Jira key/URL. Existing issues are preserved unless explicit synchronization is requested. Connection/permission/placement failures leave the approved local document intact; no REST fallback.

### Published Stories in development order

| Order | Stable ID | Jira Story |
| --- | --- | --- |
| 1 | EMP-001 | [OLARIZE-1](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-1) |
| 2 | EMP-002 | [OLARIZE-2](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-2) |
| 3 | EMP-004 | [OLARIZE-3](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-3) |
| 4 | EMP-005 | [OLARIZE-4](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-4) |
| 5 | EMP-006 | [OLARIZE-5](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-5) |
| 6 | EMP-003 | [OLARIZE-6](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-6) |
| 7 | EMP-007 | [OLARIZE-7](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-7) |
| 8 | EMP-011 | [OLARIZE-8](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-8) |
| 9 | EMP-008 | [OLARIZE-9](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-9) |
| 10 | EMP-009 | [OLARIZE-10](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-10) |
| 11 | EMP-010 | [OLARIZE-11](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-11) |
| 12 | EMP-012 | [OLARIZE-12](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-12) |
| 13 | EMP-013 | [OLARIZE-13](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-13) |
| 14 | EMP-014 | [OLARIZE-14](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-14) |
| 15 | EMP-015 | [OLARIZE-15](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-15) |
| 16 | EMP-016 | [OLARIZE-16](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-16) |
| 17 | EMP-019 | [OLARIZE-17](https://goutaibabderrafii.atlassian.net/browse/OLARIZE-17) |

Withheld: EMP-017 (conditional rehire; OD-06 unresolved) and EMP-018 (optional export; approved purpose unresolved). Neither was included in the user’s 17-required-story approval. Approved issue descriptions retain all applicable dependency, legal-review and live-processing gates; publication does not certify legal compliance or mark dependencies delivered.

### Next development handoff

Build **EMP-001** first. It is a complete backend read slice with its own schema, public API, trusted authorization resolver and PostgreSQL tests, and requires no unfinished HR module, frontend, AI provider or legal calculator. Reuse the completed token validation. Refine task 002's membership prerequisite without marking its employee/AI outcome complete.

Exact invocation:

```text
Use $olarize-feature with docs/requirements/employee.md, story EMP-001.
Implement only the trusted company-context backend slice and its linked rules
and acceptance scenarios. Reuse the existing Keycloak authentication. Include
unit tests, PostgreSQL/HTTP integration tests, migrations, API documentation
and the controlled provisioning/revocation procedure. Do not add AI or frontend.
```

The installed `$olarize-feature` is backend-only by default. EMP-001 fits that scope exactly. A backend-only invocation cannot mark a story containing UI/assistant acceptance complete. For those stories, after approval and dependencies, the proposed explicit invocation is: `Use $olarize-feature with docs/requirements/employee.md, story EMP-NNN. Implement the full approved vertical slice, including only the frontend and assistant surfaces explicitly required by that story; this request overrides the skill’s backend-only restriction for this story. Follow the applicable frontend/assistant instructions and verify every acceptance scenario.` Replace EMP-NNN with one approved ID. This text is a future handoff, not authorization to implement now; this requirements request invokes neither implementation workflow.

### Verification of this requirements artifact

Repository references, source layout/security adapters, ownership and task dependencies were inspected. Official sources were researched with the successful retrievals and failed rechecks recorded in the legal register; current Labour Code completeness is not established. Document checks cover local link targets, unique story/rule IDs and story dependency references. No application code was changed and no application test run or live AI/storage integration is claimed. Jira publication/read-back/backlog verification is evidenced above.
