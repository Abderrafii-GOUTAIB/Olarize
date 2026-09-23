# Keycloak and the first endpoint

Olarize uses self-hosted Keycloak 26.7.4 and Spring Security's OAuth2 resource server. There is no Cognito integration. Identity hosting can move independently of the HR application. AWS remains a possible target for other services, not an authentication requirement.

## Start locally

On first setup, copy .env.example to .env and replace all three example passwords with distinct alphanumeric values. Preserve an existing .env. The current workstation already has generated local Keycloak passwords in this ignored file.

```sh
docker compose --profile app up -d --build --wait
```

For IntelliJ, start `docker compose up -d db keycloak --wait`, select the local Spring profile, and run OlarizeApplication. Stop the Compose backend first if it occupies port 8080. No credentials need to be copied into the IDE.

Keycloak administration: http://localhost:8081/admin/ (username admin; password KEYCLOAK_ADMIN_PASSWORD in .env). Select the olarize realm. Create a synthetic local user, complete the profile, and set a password through Credentials. The bootstrap administrator belongs to the master realm and is not an HR user. There are no committed user passwords or default HR administrators.

The local realm uses sslRequired=none only for HTTP development. Never use that setting in a shared deployment. The local realm imports once; Keycloak skips import when that realm already exists. Editing JSON does not update an existing realm. Apply deliberate configuration changes through administration or a migration; never delete volumes just to reimport. Password environment changes do not rotate existing database/admin credentials.

## Login contract

The public client is olarize-web. Its local redirect URIs include the future frontend callback and Swagger UI at http://localhost:8080/swagger-ui/oauth2-redirect.html and http://127.0.0.1:8080/swagger-ui/oauth2-redirect.html. Keycloak permits these exact Swagger browser origins for the token exchange. Use authorization code with PKCE S256, a random state and nonce, and exact redirect http://localhost:5173/auth/callback. Origin is http://localhost:5173. Password grant, implicit flow, service accounts and public registration are disabled for this client. Frontend login is not implemented yet; the callback is its planned contract.

Issuer: http://localhost:8081/realms/olarize. Discovery: /realms/olarize/.well-known/openid-configuration. Request openid and profile.read. The API audience mapper adds olarize-api only to access tokens. Access tokens expire after five minutes. Keep tokens in memory and out of logs/URLs; require MFA for privileged users before a shared deployment.

## GET /api/v1/me

Supply Authorization: Bearer <access-token>. A successful response is:

```json
{"subject":"the-authenticated-keycloak-subject"}
```

The subject is the external identity-provider identifier, not an employee ID or an application membership. Future identity mapping must bind issuer + subject to an internal user. The endpoint accepts no target user or company parameter, performs no write and exposes no email, salary, role or tenant data. It is intentionally a thin web adapter; a pass-through service or repository would add no business boundary here.

Authentication validates RS256 signature from configured keys, issuer, expiration, not-before, required subject/issued-at/expiration, audience olarize-api, authorized party olarize-web and access-token claim typ=Bearer. Scope profile.read is required. Missing/invalid tokens return 401; a valid token without scope returns 403. Both use the existing sanitized problem contract and request ID. 401 includes WWW-Authenticate: Bearer. Other HR routes remain denied even with a valid token. No company membership or tenant authorization is implemented by this endpoint.

See api/openapi.yaml for the endpoint contract. Standard response headers prevent caching. CSRF remains enabled since this slice introduces only GET; revisit it explicitly when authenticated writes are implemented. CORS is not opened globally; the future frontend should use a local development proxy or reviewed exact origins.

## Configuration and security boundary

Outside the local profile, AUTH_ISSUER and AUTH_JWK_SET_URI must be supplied. AUTH_AUDIENCE defaults to olarize-api and AUTH_CLIENT_ID to olarize-web. Use HTTPS for shared deployments. Docker validates the same public localhost issuer as the browser, but obtains signing keys at the internal keycloak service URL. Do not replace the expected issuer with an internal Docker hostname.

Keycloak has a separate PostgreSQL service, credentials and persistent volume; its DB has no published host port. The local HTTP/start-dev configuration is only for local development. Production needs TLS, a fixed public hostname, production startup, secret rotation, backups/restore verification, restricted admin access, patching, monitoring and an availability plan. JWT validation is offline between key fetches: logout/revocation does not instantly invalidate every already-issued access token; short lifetimes limit that window. Membership revocation must also be enforced by Olarize when membership authorization is implemented.

## Tests

Run ./scripts/verify-backend.sh. Tests use disposable PostgreSQL through Testcontainers and a local HTTP JWKS server with generated RSA keys. They exercise the actual Spring decoder and HTTP filter chain, not a mocked authenticated principal. Cases include a valid response, missing bearer token, forged signature, expired token, future not-before, missing expiration/subject, wrong issuer/audience/client, ID token, missing scope, denied HR routes and attempted subject override. No AWS or Keycloak credentials are needed for this suite. Run `python3 scripts/smoke-keycloak.py` after the local stack is healthy for a live authorization-code/PKCE check. It creates and deletes a synthetic realm user, verifies the endpoint, and confirms that an ID token is rejected. The test client handles localhost Secure cookies as a browser does.

References: https://www.keycloak.org/getting-started/getting-started-docker and https://www.keycloak.org/server/containers.

Swagger UI is available only in the local profile at http://localhost:8080/swagger-ui/index.html. Click Authorize, select profile.read, and sign in with a synthetic realm user. It uses authorization code + PKCE without a client secret. To verify this exact Swagger redirect in the live smoke test, run `SMOKE_REDIRECT_URI=http://localhost:8080/swagger-ui/oauth2-redirect.html python3 scripts/smoke-keycloak.py`.
