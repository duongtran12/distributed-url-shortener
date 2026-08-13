# Production deployment

The production override is designed for a single Docker host behind an HTTPS reverse proxy or cloud load balancer. TLS must terminate before traffic reaches the Nginx container. Do not expose this stack directly over plain HTTP because production refresh cookies are marked `Secure`.

## Host prerequisites

- a Linux host with current Docker Engine and Docker Compose
- a domain whose DNS points to the host or load balancer
- HTTPS termination with a valid certificate
- a container registry containing versioned backend and frontend images
- an SMTP provider that supports authenticated STARTTLS
- firewall rules allowing public traffic only to the HTTPS proxy

## Configure secrets

Create the private production environment file:

```powershell
Copy-Item .env.production.example .env.production
```

Generate independent values instead of reusing passwords:

```powershell
$jwtBytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
[Convert]::ToBase64String($jwtBytes)

$secretBytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Fill($secretBytes)
[Convert]::ToBase64String($secretBytes)
```

Set `BACKEND_IMAGE` and `FRONTEND_IMAGE` to immutable version tags or image digests. Set `PUBLIC_BASE_URL` to the final HTTPS URL including the trailing slash. Configure real PostgreSQL, RabbitMQ, Grafana and SMTP credentials. Never commit `.env.production`.

## Publish application images

Pushing a semantic version tag builds Linux AMD64 and ARM64 images and publishes them to GitHub Container Registry:

```powershell
git switch main
git pull --ff-only origin main
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

The workflow publishes these packages:

- `ghcr.io/duongtran12/distributed-url-shortener-backend`
- `ghcr.io/duongtran12/distributed-url-shortener-frontend`

A `v1.2.3` release produces `1.2.3`, `1.2`, `1`, `latest` and commit-SHA tags. The workflow can also be launched manually from **Actions > Publish container images** for a SHA-tagged test build. Package visibility is managed separately in the repository's **Packages** settings.

Use a full image digest for the strongest deployment reproducibility, or use the exact `1.2.3` tag for simpler operations. Do not deploy from `latest` when rollback predictability matters.

## Validate and deploy

Use both Compose files for every production operation:

```powershell
$compose = @(
  '--env-file', '.env.production',
  '-f', 'docker-compose.yml',
  '-f', 'docker-compose.production.yml'
)

docker compose @compose config --quiet
docker compose @compose pull backend nginx
docker compose @compose up -d --wait
docker compose @compose ps
```

The production override does not publish PostgreSQL, Redis or RabbitMQ ports. Prometheus and Grafana listen only on `127.0.0.1`; access them through an authenticated tunnel or private network.

Run the smoke test against the public HTTPS endpoint:

```powershell
.\ops\smoke-test.ps1 -BaseUrl 'https://short.example.com'
```

## Upgrade and rollback

Create and verify a database backup before upgrading. Change both image tags in `.env.production`, pull and recreate the application:

```powershell
.\ops\backup-postgres.ps1 -OutputDirectory 'D:\ShortwaveBackups'
docker compose @compose pull backend nginx
docker compose @compose up -d --wait backend nginx
```

If application verification fails, restore the previous image tags and run the same `pull` and `up` commands. Database migrations are forward-only; review Flyway migrations before deployment and restore a tested backup only when a schema rollback is necessary.

## Operations

```powershell
docker compose @compose logs -f backend nginx
docker compose @compose restart backend nginx
docker compose @compose down
```

Do not add `--volumes` to production shutdown commands. That option deletes persistent application data.
