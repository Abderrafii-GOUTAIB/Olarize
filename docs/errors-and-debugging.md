# Errors and debugging

## API error contract

Controller errors and security 401/403 responses use application/problem+json. Standard fields are type, title, status and detail; Olarize adds code and requestId. Validation may include errors containing field and constraint code, without rejected values. The X-Request-ID response header matches requestId. Client IDs are accepted only when 1-64 safe characters; otherwise a UUID is generated. Correlation is not identity or authorization.

Example:

```json
{"type":"urn:olarize:problem:http-401","title":"Unauthorized","status":401,"detail":"Authentication is required.","code":"HTTP_401","requestId":"example-123"}
```

Spring MVC validation/malformed requests are 400, unknown resources 404 once authorized routing exists, and unexpected exceptions 500. Security currently blocks every non-health route before controllers; those calls return 401/403 rather than exposing route existence. Health failure responses intentionally keep the Actuator health shape, not a problem document.

Default logs record error type and request ID, never raw request bodies, salary values or SQL exception messages. ApiExceptionHandler DEBUG logging can show a diagnostic stack and exception message; enable it only locally with synthetic data, never indiscriminately in production. Do not expose stack traces to clients. Async context propagation must be implemented and tested when asynchronous workflows are added; current correlation is for synchronous HTTP requests.

## Host debugging

```sh
docker compose up -d db --wait
./scripts/run-backend.sh --debug
```

Attach IntelliJ or VS Code with a remote JVM debugger at localhost:5005 (or DEBUG_PORT). Breakpoints can be placed in SecurityConfiguration, RequestIdFilter and ApiExceptionHandler. No debugger port is opened during a normal start.

## Docker debugging

```sh
docker compose -f compose.yaml -f compose.debug.yaml --profile app up -d --build --wait
```

Attach at localhost:5005. The JVM listens on the container interface, but the host publishes only 127.0.0.1. Never use the debug override in shared deployments. Remove it by stopping that Compose stack and starting the ordinary configuration.

Use docker compose logs --tail=100 backend and the response request ID to correlate a failure. Readiness includes the DB; liveness indicates the process lifecycle. A missing Docker socket in tests is a local environment problem: use scripts/verify-backend.sh to select the Docker context. Do not disable tests or delete the persistent volume to hide startup failures.

## Operational logs

Local host runs use readable console logs; the Docker backend uses one ECS JSON record per log event on stdout. RequestIdFilter adds one completion event for each non-health synchronous HTTP request, with method, status, elapsed milliseconds and requestId in MDC. It never records request paths, query strings, headers, bodies or bearer tokens. Health polling is omitted to reduce noise. Use `docker compose logs --tail=100 backend` and match the `X-Request-ID` response header to the JSON `requestId` field. A container restart does not preserve logs indefinitely; configure collection, access and retention for deployments. Business audit events are separate from operational logs and are still a future module.
