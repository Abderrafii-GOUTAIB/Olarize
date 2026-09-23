# Task 001 Establish the development foundation

Status: backend and Docker implementation in progress; frontend outside the current request

## Outcome

A developer can start local PostgreSQL, build the backend, run a React shell and execute CI without paid AI calls or provisioned AWS application infrastructure.

## Scope

Select and pin a compatible Java LTS, Spring Boot and Modulith set and verify the intended Spring AI compatibility; create backend/pom.xml and Maven wrapper; add Flyway and PostgreSQL configuration. Create the application entry point under com.olarize. Add module boundary verification when modules exist; do not create empty domain implementations solely to fill the architecture diagram.

Create frontend/ with React, TypeScript, Vite, MUI and npm lockfile. Establish development, typecheck, lint, test and build scripts. Render the shell of the primary conversational workspace with an explicit backend health state. It must show unavailable/unimplemented AI honestly; do not simulate real HR answers in the product. Use a development proxy or precise CORS configuration; do not allow arbitrary production origins.

Create compose.yaml with a persistent local PostgreSQL/pgvector database service. Add safe example configuration, ignore rules, documented toolchain requirements and CI. The initial health endpoint must reveal only service health. Do not expose employee endpoints or a production authentication bypass. Keycloak, membership authorization and the first live-data AI tool are the next delivery slice, specified in .instructions/tasks/002-first-ai-workflow.md.

## Acceptance checks

- A fresh checkout installs with the committed lockfile and Maven wrapper.
- Local database starts and a Flyway migration applies successfully.
- Backend verifies against PostgreSQL; failed connection is diagnosable.
- Frontend typecheck, lint, meaningful shell test and production build pass.
- The running shell displays actual backend availability, including an unavailable state.
- CI runs without AWS secrets, paid model calls or confidential data.
- docs/development.md records real commands, selected versions and prerequisites.

## Completion

Report actual verification results and anything requiring local tooling or user cloud configuration. Do not deploy or generate the full HR domain in this task.
