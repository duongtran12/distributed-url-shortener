[CmdletBinding()]
param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot '..\backups')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$backupDirectory = if ([System.IO.Path]::IsPathRooted($OutputDirectory)) {
    [System.IO.Path]::GetFullPath($OutputDirectory)
} else {
    [System.IO.Path]::GetFullPath((Join-Path $repositoryRoot $OutputDirectory))
}

Push-Location $repositoryRoot
try {
    $containerId = (& docker compose ps -q postgres).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($containerId)) {
        throw 'The PostgreSQL Compose service is not running. Start it before creating a backup.'
    }

    $databaseUser = (& docker exec $containerId printenv POSTGRES_USER).Trim()
    $databaseName = (& docker exec $containerId printenv POSTGRES_DB).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($databaseUser) -or
            [string]::IsNullOrWhiteSpace($databaseName)) {
        throw 'Could not read PostgreSQL connection settings from the container.'
    }

    New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
    $timestamp = [DateTime]::UtcNow.ToString('yyyyMMdd-HHmmss')
    $fileName = "shortwave-postgres-$timestamp.dump"
    $containerFile = "/tmp/$fileName"
    $backupFile = Join-Path $backupDirectory $fileName

    try {
        & docker exec $containerId pg_dump `
            "--username=$databaseUser" `
            "--dbname=$databaseName" `
            '--format=custom' `
            '--compress=9' `
            "--file=$containerFile"
        if ($LASTEXITCODE -ne 0) {
            throw 'pg_dump failed inside the PostgreSQL container.'
        }

        & docker cp "${containerId}:${containerFile}" $backupFile
        if ($LASTEXITCODE -ne 0) {
            throw 'Could not copy the PostgreSQL backup out of the container.'
        }
    } finally {
        & docker exec $containerId rm -f $containerFile 2>$null
    }

    $createdBackup = Get-Item -LiteralPath $backupFile
    if ($createdBackup.Length -eq 0) {
        throw 'The generated backup file is empty.'
    }

    Write-Output "Backup created: $($createdBackup.FullName)"
    Write-Output "Size: $($createdBackup.Length) bytes"
} finally {
    Pop-Location
}
