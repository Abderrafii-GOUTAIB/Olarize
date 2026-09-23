# CI and delivery instructions

Build and test changed application components on pull requests. Include backend tests and module verification, frontend type/lint/test/build checks, and Terraform formatting/validation when relevant. Use docs/development.md as the source of executable commands; add commands when implemented, never assume placeholders work.

Use minimal workflow permissions and pin third-party actions to reviewed immutable commits. Authenticate to AWS through OIDC federation scoped to repository, branch/environment and role. Do not store permanent AWS access keys.

Create immutable backend image tags in ECR and deploy the same tested image. Build React assets and publish through the approved pipeline. Run Flyway once in a controlled migration step, not concurrently from every application replica. Database changes must remain compatible with the rollout strategy.

Keep real Bedrock calls and deployment credentials out of ordinary untrusted pull-request execution. Production deployment follows explicit environment authorization. Do not label a build deployed merely because an image was pushed; verify health and the intended application version.
