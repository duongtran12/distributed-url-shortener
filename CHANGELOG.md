# Changelog

All notable changes to this project are documented in this file. The project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-08-16

### Added

- JWT authentication with rotating refresh sessions, email verification and secure password reset.
- Short-link creation, editing, disabling, blocking, deletion, expiration, titles, pinning, search, filtering, sorting and bulk operations.
- Redis-backed redirect caching and distributed rate limiting.
- RabbitMQ-based asynchronous click ingestion and real-time analytics dashboards.
- Short-link audit history, QR codes and request correlation identifiers.
- PostgreSQL schema management through Flyway and automated data-retention jobs.
- React management dashboard and a production Nginx gateway with multiple backend replicas.
- Prometheus metrics and provisioned Grafana dashboards.
- Docker development and production stacks, backup and restore tooling, smoke tests, Playwright end-to-end tests and k6 load tests.
- Continuous integration, CodeQL, dependency review, Dependabot and multi-platform GHCR image publishing with provenance attestations.
- OpenAPI documentation, production runbook and distributed-system architecture documentation.

### Security

- Passwords are encoded with BCrypt and sensitive one-time tokens are stored only as SHA-256 hashes.
- Authentication responses prevent email-account enumeration.
- Production cookies are secure and internal infrastructure ports are not publicly exposed.
- The vulnerable transitive `nanoid` dependency is pinned to version `3.3.18`.

[1.0.0]: https://github.com/duongtran12/distributed-url-shortener/releases/tag/v1.0.0
