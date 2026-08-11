# Redirect load testing

This directory contains a k6 test for the public short-link redirect path. k6 runs in Docker, so no local k6 installation is required.

The test does not follow the redirect. It measures the application gateway and backend only, and never sends traffic to the destination website.

## Run the safe baseline

1. Start the complete application stack:

   ```powershell
   docker compose up -d --build --wait
   ```

2. Sign in to the application at `http://localhost:8080` and copy the code of an active, non-expired short link. For example, copy `abc1234` from `/abc1234`.

3. Run the test:

   ```powershell
   $env:LOAD_TEST_SHORT_CODE = 'abc1234'
   docker compose --profile load-test run --rm k6
   ```

The default test sends one request per second for 30 seconds. This stays within the default redirect limit of 60 requests per minute when no other traffic uses the same client address.

## Result checks

The command fails when any threshold is missed:

- more than 1% of HTTP requests fail
- fewer than 99% of checks pass
- redirect response p95 is 250 ms or slower

Each response must return HTTP 302 and include both `Location` and `X-Request-ID` headers.

## Run a higher-rate test

Higher request rates require a controlled local environment and a matching redirect rate limit. Set the backend limit above the planned request count, recreate the backend replicas, and then run k6. This example sends 50 requests per second for one minute:

```powershell
$env:RATE_LIMIT_REDIRECT_REQUESTS = '4000'
docker compose up -d --force-recreate --wait backend nginx

$env:LOAD_TEST_SHORT_CODE = 'abc1234'
$env:LOAD_TEST_RATE = '50'
$env:LOAD_TEST_DURATION = '1m'
$env:LOAD_TEST_PRE_ALLOCATED_VUS = '20'
$env:LOAD_TEST_MAX_VUS = '100'
docker compose --profile load-test run --rm k6
```

Restore the normal local limit after the test:

```powershell
Remove-Item Env:RATE_LIMIT_REDIRECT_REQUESTS -ErrorAction SilentlyContinue
Remove-Item Env:LOAD_TEST_RATE -ErrorAction SilentlyContinue
Remove-Item Env:LOAD_TEST_DURATION -ErrorAction SilentlyContinue
Remove-Item Env:LOAD_TEST_PRE_ALLOCATED_VUS -ErrorAction SilentlyContinue
Remove-Item Env:LOAD_TEST_MAX_VUS -ErrorAction SilentlyContinue
docker compose up -d --force-recreate --wait backend nginx
```

Only run higher-rate tests against systems you own and are authorized to test.
