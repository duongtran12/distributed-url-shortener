# Distributed URL Shortener & Analytics Platform

[![Continuous Integration](https://github.com/duongtran12/distributed-url-shortener/actions/workflows/ci.yml/badge.svg)](https://github.com/duongtran12/distributed-url-shortener/actions/workflows/ci.yml)

A portfolio project built incrementally with Java 21, Spring Boot, PostgreSQL, Redis, RabbitMQ, Docker, Nginx, and React.

## Current status

The application includes authentication, URL management, Redis caching and distributed rate limiting, RabbitMQ click tracking, analytics APIs, and a React analytics dashboard.

## Architecture

The system context, container topology, redirect and analytics sequences, data ownership, consistency model, security boundaries, scaling characteristics and current limitations are documented in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Prerequisites

- Java 21
- Docker Desktop with Docker Compose
- Node.js 24+

## Local setup

1. Create a local environment file:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Replace `change_me` in `.env` with a local development password.

3. Start PostgreSQL:

   ```powershell
   docker compose up -d postgres
   ```

4. Export the matching application variables, then run the application:

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = 'dev'
   $env:SPRING_DATASOURCE_PASSWORD = '<the password from .env>'
   .\mvnw.cmd spring-boot:run
   ```

5. Verify health:

   ```powershell
   Invoke-RestMethod http://localhost:8080/actuator/health
   ```

Stop local infrastructure with `docker compose down`. Add `--volumes` only when you intentionally want to delete local database data.

## Frontend

With the backend running on port `8080`, start the Vite development server:

```powershell
Set-Location frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies backend requests to `http://localhost:8080` during local development.

## Full Docker stack

The production-like Compose stack builds the Spring Boot and React images, serves the frontend through Nginx, and load-balances API traffic across two backend replicas.

1. Create `.env` from the example and replace every `change_me` value:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Generate a Base64-encoded JWT secret containing at least 32 bytes:

   ```powershell
   $jwtBytes = New-Object byte[] 32
   [Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
   [Convert]::ToBase64String($jwtBytes)
   ```

   Paste the printed value after `JWT_SECRET=` in `.env`. Keep `.env` local and never commit it.

3. Build and start the complete stack:

   ```powershell
   docker compose up -d --build --wait
   ```

4. Open the application at `http://localhost:8080`. RabbitMQ management is available at `http://localhost:15672`.

5. Inspect container health or follow application logs:

   ```powershell
   docker compose ps
   docker compose logs -f backend nginx
   ```

Stop the stack with `docker compose down`. This preserves named volumes; adding `--volumes` also deletes the local database and broker data.

## Production deployment

The production Compose override uses prebuilt registry images, exposes only the application gateway publicly, binds Grafana and Prometheus to loopback, disables Mailpit and enables secure authentication cookies. Deployment prerequisites, secret generation, SMTP configuration, validation and rollback commands are documented in [`ops/PRODUCTION.md`](ops/PRODUCTION.md).

Version tags matching `vMAJOR.MINOR.PATCH` automatically publish multi-platform backend and frontend images to GitHub Container Registry and create a GitHub Release with generated notes. Published images include semantic-version and commit-SHA tags plus signed build provenance attestations. See the [release runbook](ops/RELEASE.md) and [changelog](CHANGELOG.md) before publishing a version.

## Database recovery

PowerShell scripts for creating and restoring PostgreSQL custom-format backups are documented in [`ops/README.md`](ops/README.md). Backup output is excluded from Git. Restoration is destructive and requires an explicit `-Force` switch.

## Load testing

The redirect path has a Dockerized k6 performance test. It verifies redirect status, destination and request tracing without following the destination URL. See [`load-testing/README.md`](load-testing/README.md) for safe defaults, thresholds and higher-rate test instructions.

## Stack smoke test

After starting the Docker stack, run `.\ops\smoke-test.ps1` to verify the frontend, backend health, OpenAPI document, authentication boundary and a real short-link redirect. The same smoke test runs automatically in continuous integration.

## Security automation

GitHub Actions runs CodeQL analysis for Java and TypeScript and reviews dependency changes introduced by pull requests. The frontend CI also rejects high or critical npm advisories. Dependabot checks Maven, npm, Docker, Docker Compose and GitHub Actions dependencies every week; review and merge its pull requests only after all CI checks pass.

## API documentation

Interactive OpenAPI documentation is available at `http://localhost:8080/swagger-ui.html` when the application is running. Public authentication and redirect operations can be called directly. For protected operations, sign in through `/api/v1/auth/login`, copy the returned access token, select **Authorize**, and enter the token as the bearer credential.

The machine-readable OpenAPI document is available at `http://localhost:8080/v3/api-docs`.

## Request tracing

Every HTTP response includes an `X-Request-ID` header. Nginx generates the identifier at the application gateway and forwards it to the backend. Backend log lines include the same identifier, making it possible to correlate a browser/API response with both Nginx access logs and Spring Boot application logs. Direct backend requests may supply an identifier containing letters, numbers, `.`, `_`, or `-` with a maximum length of 64 characters; invalid values are replaced with a UUID.

## Metrics

The Docker stack runs Prometheus at `http://localhost:9090`. It discovers and scrapes every backend replica through Docker DNS.

Grafana is available at `http://localhost:3000`. Sign in with the admin credentials configured in `.env`, then open **Dashboards > URL Shortener > URL Shortener Overview**. The Prometheus datasource and dashboard are provisioned automatically.

Useful custom metrics include:

- `shortener_redirect_resolutions_total`, tagged by `source=cache|database`
- `shortener_redirect_failures_total`, tagged by `reason=not_found|unavailable`

JVM, HTTP server, HikariCP, and process metrics are also exported by Spring Boot Actuator. The Prometheus endpoint is available only on the backend management port inside the Docker network; Nginx exposes health but does not expose metrics publicly.

## Tests

The application context test uses Testcontainers to start an isolated PostgreSQL instance. Docker Desktop must be running, but the Compose database does not need to be started.

```powershell
.\mvnw.cmd clean verify
```
