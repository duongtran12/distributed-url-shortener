# Distributed URL Shortener & Analytics Platform

A portfolio project built incrementally with Java 21, Spring Boot, PostgreSQL, Redis, RabbitMQ, Docker, Nginx, and React.

## Current status

The backend includes authentication, URL management, Redis caching and distributed rate limiting, RabbitMQ click tracking, and analytics APIs. The React dashboard is under active development.

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

## Tests

The application context test uses Testcontainers to start an isolated PostgreSQL instance. Docker Desktop must be running, but the Compose database does not need to be started.

```powershell
.\mvnw.cmd clean verify
```
