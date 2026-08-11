param(
    [string]$BaseUrl = ''
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.Net.Http

$fixtureSuffix = [Guid]::NewGuid().ToString('N').Substring(0, 12)
$fixtureEmail = "smoke-$fixtureSuffix@example.invalid"
$shortCode = "smoke$fixtureSuffix"
$destination = 'https://example.com/smoke-target'
$fixtureCreated = $false

$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.AllowAutoRedirect = $false
$client = [System.Net.Http.HttpClient]::new($handler)
$client.Timeout = [TimeSpan]::FromSeconds(15)

function Invoke-ComposeCommand {
    param([string[]]$Arguments)

    $output = & docker compose @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Docker Compose command failed: docker compose $($Arguments -join ' ')`n$output"
    }
    return ($output -join "`n").Trim()
}

function Invoke-DatabaseSql {
    param([string]$Sql)

    $databaseUser = Invoke-ComposeCommand -Arguments @('exec', '-T', 'postgres', 'printenv', 'POSTGRES_USER')
    $databaseName = Invoke-ComposeCommand -Arguments @('exec', '-T', 'postgres', 'printenv', 'POSTGRES_DB')
    Invoke-ComposeCommand -Arguments @(
        'exec', '-T', 'postgres',
        'psql', '-U', $databaseUser, '-d', $databaseName,
        '-v', 'ON_ERROR_STOP=1', '-Atc', $Sql
    ) | Out-Null
}

function Invoke-SmokeRequest {
    param([string]$Path)

    $response = $client.GetAsync("$BaseUrl$Path").GetAwaiter().GetResult()
    $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    return [PSCustomObject]@{
        Status = [int]$response.StatusCode
        Headers = $response.Headers
        Body = $body
    }
}

function Assert-Equal {
    param($Actual, $Expected, [string]$Message)

    if ($Actual -ne $Expected) {
        throw "$Message Expected '$Expected', received '$Actual'."
    }
}

function Assert-True {
    param([bool]$Condition, [string]$Message)

    if (-not $Condition) {
        throw $Message
    }
}

function Assert-RequestId {
    param($Response, [string]$Endpoint)

    $found = $Response.Headers.Contains('X-Request-ID')
    $requestId = if ($found) { $Response.Headers.GetValues('X-Request-ID') | Select-Object -First 1 } else { '' }
    Assert-True ($found -and -not [string]::IsNullOrWhiteSpace($requestId)) "$Endpoint did not return X-Request-ID."
}

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $publishedAddress = Invoke-ComposeCommand -Arguments @('port', 'nginx', '80')
    Assert-True ($publishedAddress -match ':(\d+)$') "Could not determine Nginx's published port from '$publishedAddress'."
    $BaseUrl = "http://localhost:$($Matches[1])"
}
$BaseUrl = $BaseUrl.TrimEnd('/')

try {
    $timestamp = 'CURRENT_TIMESTAMP'
    $fixtureSql = @"
WITH smoke_user AS (
    INSERT INTO users (email, password_hash, display_name, role, enabled, created_at, updated_at)
    VALUES ('$fixtureEmail', 'not-used-by-smoke-test', 'Smoke Test', 'USER', TRUE, $timestamp, $timestamp)
    RETURNING id
)
INSERT INTO short_urls (user_id, short_code, original_url, status, custom_alias, created_at, updated_at)
SELECT id, '$shortCode', '$destination', 'ACTIVE', FALSE, $timestamp, $timestamp
FROM smoke_user;
"@
    Invoke-DatabaseSql $fixtureSql
    $fixtureCreated = $true

    $frontend = Invoke-SmokeRequest '/'
    Assert-Equal $frontend.Status 200 'Frontend request failed.'
    Assert-True ($frontend.Body -match '<div id="root"></div>') 'Frontend response did not contain the React root element.'
    Write-Host '[PASS] Frontend is served by Nginx.'

    $health = Invoke-SmokeRequest '/actuator/health'
    Assert-Equal $health.Status 200 'Health request failed.'
    Assert-Equal (($health.Body | ConvertFrom-Json).status) 'UP' 'Backend health is not UP.'
    Write-Host '[PASS] Backend health is available.'

    $openApi = Invoke-SmokeRequest '/v3/api-docs'
    Assert-Equal $openApi.Status 200 'OpenAPI request failed.'
    Assert-True (($openApi.Body | ConvertFrom-Json).openapi -match '^3\.') 'OpenAPI response is invalid.'
    Assert-RequestId $openApi '/v3/api-docs'
    Write-Host '[PASS] OpenAPI document is available.'

    $protectedApi = Invoke-SmokeRequest '/api/v1/urls'
    Assert-Equal $protectedApi.Status 401 'Protected API did not reject an anonymous request.'
    Assert-RequestId $protectedApi '/api/v1/urls'
    Write-Host '[PASS] Protected API rejects anonymous access.'

    $redirect = Invoke-SmokeRequest "/$shortCode"
    Assert-Equal $redirect.Status 302 'Short-link redirect failed.'
    Assert-Equal $redirect.Headers.Location.AbsoluteUri $destination 'Redirect destination is incorrect.'
    Assert-RequestId $redirect "/$shortCode"
    Write-Host '[PASS] Short-link redirect resolves through the complete stack.'

    Write-Host 'Docker stack smoke test passed.'
}
finally {
    if ($fixtureCreated) {
        Start-Sleep -Milliseconds 500
        Invoke-DatabaseSql "DELETE FROM click_events WHERE short_code = '$shortCode'; DELETE FROM users WHERE email = '$fixtureEmail';"
    }
    $client.Dispose()
    $handler.Dispose()
}
