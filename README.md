# Shinchoku Services

Spring Boot 4.1.2, Java 21, Maven, PostgreSQL 15+. Implements the 71 application API operations in the supplied Aperture contract under `/api/v1`.

Start with [the backend reading guide](docs/BACKEND_GUIDE.md) for the request flow, naming conventions, and a walkthrough of issue creation/editing.

## Start locally on Windows

Requirements: JDK 21, Docker Desktop running. The Maven wrapper is included.

In this folder:

```powershell
docker compose up -d --wait
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

- API: http://localhost:8080/api/v1
- Health: http://localhost:8080/actuator/health
- Frontend: run `npm ci` and `npm run dev` in the sibling `Shinchoku-UI-Dev` folder; open http://localhost:5173.
- PostgreSQL is bound to `127.0.0.1:5432`. Local database/user: `shinchoku`; local password: `shinchoku-local`. These are development defaults.
- Flyway applies migrations automatically. PostgreSQL data persists in a Docker volume. `docker compose down` stops the database without removing that volume.

The `local` profile binds the API to loopback, allows HTTP cookies for development, and exposes a fixed local developer login. This profile must stay local. The developer account has no automatically granted production access. The local database may contain workspaces created during contract verification.

## Authentication

The application uses its own hashed sessions table, not Spring Session's storage schema.

For a local browser/client, fetch `GET /api/v1/auth/csrf`, then send its `token` in the `X-XSRF-TOKEN` header on cookie-authenticated mutations. Send `Origin: http://localhost:5173`; the browser does this for fetch mutations. Keep cookies enabled.

`POST /api/dev/login` creates/signs in the fixed `developer@shinchoku.local` account only in the local profile, and returns an HttpOnly session cookie. No body is needed. The frontend API client exposes `api.localLogin()` for this workflow. Local sessions are not valid production identities.

Authenticated API documentation is available at `GET /api/v1/openapi`. The static source is `contracts/openapi.json`.

For API keys, send `Authorization: Bearer <token>`. Keys are workspace-bound, hashed at rest, expire, and only reduce their owner's current access. The raw token is returned once by creation. Bearer requests do not require a CSRF token. Session-only operations reject API keys.

The default production cookie is Secure, HttpOnly, and SameSite=Lax. Cookie mutations validate Origin and CSRF. Configure HTTPS and a same-origin reverse proxy for `/api`, `/oauth2`, and `/login/oauth2`; change `FRONTEND_URL` to that origin. The Vite proxy already does this during development.

## Identity provider setup

OIDC is optional until credentials are supplied. Enable the `oidc` profile in addition to `local` for local identity-provider testing, or use `oidc` alone with HTTPS for production.

```powershell
$env:OIDC_ISSUER_URI = 'https://your-identity-provider.example'
$env:OIDC_CLIENT_ID = 'your-client-id'
$env:OIDC_CLIENT_SECRET = 'your-client-secret'
$env:OIDC_REDIRECT_URI = 'http://localhost:8080/login/oauth2/code/google'
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,oidc"
```

The sample registration ID is `google`; the issuer is configurable. Rename the registration in `application-oidc.properties` and its callback path if using another provider name from the API contract. Register the exact callback URI with the provider. Begin login at `/oauth2/authorization/google`. Authorization uses state and PKCE. The provider must support OIDC with a verified email claim. Existing email addresses are never linked automatically; account linking requires an authenticated application session that remains valid through callback. Provider MFA assurance is accepted from `amr=mfa` or combined `pwd` and `otp` claims. MFA enrollment/recovery and organization SSO belong to the identity provider.

The OAuth connection endpoint supports configured providers and an exact frontend return URI; unconfigured providers return a clear 503 response. It does not pretend that arbitrary provider accounts are already connected. Plain OAuth-only GitHub/Slack account linking needs a provider-specific verified-identity adapter if required; the supplied OIDC login adapter intentionally requires verified OIDC claims.

## Configuration

Set environment variables in the shell/process launching the application. Spring Boot does not automatically load a `.env` file.

| Variable | Purpose |
| --- | --- |
| `DATABASE_URL` | JDBC URL, e.g. `jdbc:postgresql://host:5432/shinchoku` |
| `DATABASE_USERNAME`, `DATABASE_PASSWORD` | Database credentials |
| `FRONTEND_URL` | Exact allowed UI origin, default `http://localhost:5173` |
| `COOKIE_SECURE` | Defaults to true; local profile explicitly uses false |
| `SESSION_HOURS` | Absolute session lifetime, default 24 hours |
| `PORT` | API port, default 8080 |
| `MAIL_ENABLED`, `MAIL_FROM` | Enable production invitation emails and set the sender |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | SMTP configuration; configure TLS according to your provider |
| `OIDC_ISSUER_URI`, `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET`, `OIDC_REDIRECT_URI` | Identity provider configuration when the `oidc` profile is active |

The local profile writes invitation messages to ignored `local-mail/*.eml` files; tokens never appear in invitation list responses. Other profiles require configured SMTP delivery and fail explicitly if it is unavailable. No real emails were sent during verification.

GitHub repository/link metadata is implemented. Installation, synchronization workers, conflict resolution, and secret-manager integration need actual GitHub app credentials and a separate integration implementation, as described in the supplied contract. No sync job is falsely reported as connected. Insights uses authorized database data and is explicitly `rule_based`; no AI provider is called.

## Implementation structure

The backend uses conventional typed layers: **controller -> request DTO -> transactional service -> Spring Data JPA repository -> entity**. Services map results to explicit response DTOs before returning them.

- `entity`: 25 persistent entity classes, their composite IDs, enums and converters.
- `repository`: 25 Spring Data JPA repository interfaces and the atomic issue-number allocator.
- `dto`: typed request/response records and PATCH classes that distinguish omitted fields from null.
- `api`: seven resource controllers, request-contract validation, shared errors and support endpoints.
- `security`: entity-backed authentication, roles, grants and OIDC login.
- `service`: business rules, transactions, DTO mapping and derived metrics.
- `persistence`: typed Criteria and cursor-pagination support.
- `src/main/resources/db/migration`: unchanged Flyway migrations.
- `src/test`: HTTP regressions and direct JPA persistence tests on real PostgreSQL.

The former generic `Database`, `ApiGateway` and map-based request context have been removed from production. Maps remain only where the data is genuinely dynamic: OpenAPI validation metadata and JSONB audit changes. Responses are explicit DTOs, so entities and credential hashes are never returned directly.

Hibernate validates the existing schema with `ddl-auto=validate`; Flyway remains the schema authority. Issue edits use `@Version`. Workspace ownership, cycles and triage use repository locks. Guest grants are included in Criteria queries before pagination and counts. A small native `UPDATE ... RETURNING` helper retains atomic issue-number allocation.

See [the backend reading guide](docs/BACKEND_GUIDE.md) for entity/repository/DTO examples and a complete request walkthrough.

## Verify and package

```powershell
.\mvnw.cmd verify
```

Docker Desktop must be running. Tests create disposable PostgreSQL containers and do not depend on the Compose database. Maven builds `target/shinchokuapp-0.0.1-SNAPSHOT.jar`.

```powershell
java -jar target/shinchokuapp-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Verification covers tenant isolation, guest grants/counts, key scope and revocation, secret redaction, CSRF/Origin enforcement, last-owner and membership lifecycle rules, optimistic updates, filter/cursor semantics, concurrent issue allocation and triage decisions, cycle overlaps, invitation acceptance, provider connection protection and PKCE. External identity-provider callbacks and SMTP/GitHub services require real configuration for end-to-end external testing.
