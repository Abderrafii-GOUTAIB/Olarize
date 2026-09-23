# ADR 002: Self-hosted identity with Keycloak

Status: accepted and implemented for local development, superseding the Cognito choice in ADR 001.

The owner requires authentication that is independent of AWS. Use Keycloak with a separate PostgreSQL database and standard Spring Security JWT resource-server integration. Olarize owns company membership and business permissions; identity-provider roles do not grant HR access automatically. Provider-specific claim validation stays in the platform security adapter.

The first endpoint is GET /api/v1/me and returns only the verified external subject. It requires API audience, authorized client and profile.read scope. No artificial service/interface layer is introduced for a projection without business logic. Membership resolution will introduce a real application boundary when implemented.

Self-hosting replaces managed identity charges with operational responsibilities; it is not a promise of lower total cost. The local development Compose stack is not a production deployment. Tokens last five minutes; offline JWT validation does not provide immediate token revocation. Shared deployments must address TLS, MFA, monitoring, patching, backups and session/revocation requirements.

Other planned AWS services remain separate decisions. Identity-provider migration still requires user/account and issuer-subject mapping work. Follow docs/authentication.md for configuration and limits.
