# Distributed URL Shortener & Analytics Platform

A portfolio project built incrementally with Java 21, Spring Boot, PostgreSQL, Redis, RabbitMQ, Docker, Nginx, and React.

## Current status

Phase 1 foundation is in progress. The current setup contains the Spring Boot application and a Dockerized PostgreSQL database. Product features are not implemented yet.

## Prerequisites

- Java 21
- Docker Desktop with Docker Compose

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

## Tests

The application context test uses Testcontainers to start an isolated PostgreSQL instance. Docker Desktop must be running, but the Compose database does not need to be started.

```powershell
.\mvnw.cmd clean verify
```
