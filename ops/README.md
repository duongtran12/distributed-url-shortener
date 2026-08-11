# PostgreSQL backup and restore

The scripts in this directory operate on the PostgreSQL service managed by Docker Compose. Backup archives are stored outside the container so they survive container replacement.

## Create a backup

Start PostgreSQL, then run:

```powershell
.\ops\backup-postgres.ps1
```

By default, the script writes a timestamped custom-format archive to the ignored `backups/` directory. To choose another location:

```powershell
.\ops\backup-postgres.ps1 -OutputDirectory 'D:\ShortwaveBackups'
```

Keep production backups encrypted and outside the application host. Regularly copy them to independent storage and test restoration.

## Restore a backup

Restoration replaces the current database and therefore requires the explicit `-Force` switch:

```powershell
.\ops\restore-postgres.ps1 -BackupFile '.\backups\shortwave-postgres-20260811-030000.dump' -Force
```

When backend or Nginx containers are running, the restore script stops them before `pg_restore` and starts them again afterward. PostgreSQL must already be running. After restoration, verify container health and application data:

```powershell
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

Never restore an untrusted archive. Create a fresh backup of the current database before any planned restoration.

## Docker stack smoke test

With the application stack running, verify the frontend, gateway, backend health, OpenAPI document, authentication boundary and redirect path:

```powershell
.\ops\smoke-test.ps1
```

The script creates a uniquely named database fixture, verifies a real HTTP 302 response without following the destination, and removes the fixture in a `finally` block. Use `-BaseUrl` when the stack is exposed on a non-default port:

```powershell
.\ops\smoke-test.ps1 -BaseUrl 'http://localhost:8090'
```
