[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$BackupFile,

    [switch]$Force
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (-not $Force) {
    throw 'Restore replaces the current database. Re-run with -Force after confirming the backup file.'
}

$resolvedBackup = (Resolve-Path -LiteralPath $BackupFile).Path
$backupInfo = Get-Item -LiteralPath $resolvedBackup
if ($backupInfo.Length -eq 0) {
    throw 'The selected backup file is empty.'
}

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$containerFile = "/tmp/shortwave-restore-$([Guid]::NewGuid().ToString('N')).dump"
$restartApplication = $false
$restoreSucceeded = $false
$containerId = $null

Push-Location $repositoryRoot
try {
    $containerId = (& docker compose ps -q postgres).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($containerId)) {
        throw 'The PostgreSQL Compose service is not running. Start it before restoring a backup.'
    }

    $databaseUser = (& docker exec $containerId printenv POSTGRES_USER).Trim()
    $databaseName = (& docker exec $containerId printenv POSTGRES_DB).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($databaseUser) -or
            [string]::IsNullOrWhiteSpace($databaseName)) {
        throw 'Could not read PostgreSQL connection settings from the container.'
    }

    $runningBackend = @(& docker compose ps --status running -q backend)
    $runningNginx = @(& docker compose ps --status running -q nginx)
    $restartApplication = $runningBackend.Count -gt 0 -or $runningNginx.Count -gt 0
    if ($restartApplication) {
        & docker compose stop nginx backend
        if ($LASTEXITCODE -ne 0) {
            throw 'Could not stop application services before the restore.'
        }
    }

    & docker cp $resolvedBackup "${containerId}:${containerFile}"
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not copy the backup into the PostgreSQL container.'
    }

    & docker exec $containerId pg_restore `
        "--username=$databaseUser" `
        "--dbname=$databaseName" `
        '--clean' `
        '--if-exists' `
        '--no-owner' `
        '--no-privileges' `
        '--exit-on-error' `
        $containerFile
	if ($LASTEXITCODE -ne 0) {
		throw 'pg_restore failed. Application services have been left stopped to protect the database.'
	}

	$restoreSucceeded = $true
	Write-Output "Database restored from: $resolvedBackup"
} finally {
    if ($null -ne $containerId) {
        & docker exec $containerId rm -f $containerFile 2>$null
    }
	if ($restartApplication -and $restoreSucceeded) {
		& docker compose up -d backend nginx
	} elseif ($restartApplication) {
		Write-Warning 'Backend and Nginx remain stopped because the restore did not complete successfully.'
	}
    Pop-Location
}
