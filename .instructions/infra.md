# MVP hosting and infrastructure instructions

The accepted target is managed hosting in Morocco, with a provider still to be selected. Apply `docs/decisions/003-mvp-data-protection-and-hosting.md` and `.instructions/reference/security.md` before handling real customer data. Document actual processing/storage locations for the database, private object storage, Keycloak, logs and separate encrypted backups, plus support/subprocessor access. Require a tested restore procedure and named operations responsibilities. Do not assume that a provider's marketing label proves residency or that local hosting proves compliance.

Use a portable application container, private PostgreSQL and private object storage. Do not provision AWS or assume Bedrock/S3/RDS as current MVP dependencies. The following AWS guidance is retained only for a separately approved future hosting alternative.

## Conditional AWS alternative

Use Terraform with separate environment configuration and protected remote state. Never place credentials or confidential Terraform state in Git. Use pinned provider versions and a lockfile. Plan first; apply only to an authorized account/environment. Code generation is not authorization to create billable infrastructure.

## Baseline resources

- CloudFront and a private S3 frontend bucket with Origin Access Control.
- Self-hosted Keycloak with its own PostgreSQL database and credentials, TLS, backups, monitored upgrades and exact environment-specific redirect/logout URLs. Never deploy start-dev to shared environments.
- HTTPS Application Load Balancer and ECS Fargate service; ECR for images.
- RDS PostgreSQL with a supported pgvector extension and private network access.
- Separate private S3 documents bucket with versioning and deliberate retention.
- SES for notifications, Secrets Manager for secrets, IAM task roles for AWS calls.
- CloudWatch logs/alarms and budget alerts; DNS and TLS certificates for approved domains.

Place backend tasks and RDS in private subnets. Only the intended ingress reaches the backend; only application access reaches RDS. Explicitly design outbound connectivity or service endpoints. Do not omit NAT/endpoint and load-balancer costs from estimates. Block public document access and scope bucket/key permissions.

Select region and Bedrock models based on availability, customer processing requirements and reviewed costs. Cross-region inference is an explicit processing-location decision. Do not assume this architecture satisfies every public-sector hosting requirement.

Enable encryption, RDS backup/PITR and restoration procedures. Document Single-AZ/Multi-AZ and task-count availability tradeoffs. Keep an inexpensive development configuration distinct from pilot reliability requirements.

Run formatting/validation and inspect the plan. Use least privilege rather than blanket administrator task roles. Coordinate ECS health checks, graceful shutdown and migration execution. Do not apply destructive database changes automatically. Document rollback limitations and restore tests.

## V1 economics

Tag resources by environment and ownership; define shared-cost allocation per company and monitor model quotas and usage. Budgets are operational controls, not confirmed subscription plan limits. Do not auto-provision payment/billing infrastructure from the pricing hypotheses in the business document.
