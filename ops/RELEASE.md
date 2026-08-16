# Release runbook

This runbook publishes a versioned backend image, frontend image and GitHub Release from the same immutable Git tag.

## Before tagging

1. Confirm the release pull request is merged and every required GitHub check is green.
2. Confirm `pom.xml`, `frontend/package.json` and `frontend/package-lock.json` contain the intended version without the leading `v`.
3. Update `CHANGELOG.md` with the version, date and user-visible changes.
4. Pull the exact default branch and verify that the worktree is clean:

   ```powershell
   git switch main
   git pull --ff-only origin main
   git status --short
   ```

5. Run the local release gates:

   ```powershell
   .\mvnw.cmd clean verify
   Set-Location frontend
   npm ci
   npm audit --audit-level=high
   npm run lint
   npm run build
   Set-Location ..
   docker compose config --quiet
   docker compose --env-file .env.production.example -f docker-compose.yml -f docker-compose.production.yml config --quiet
   ```

## Publish v1.0.0

Create an annotated tag only from the verified `main` commit:

```powershell
git tag -a v1.0.0 -m "Release v1.0.0"
git show --no-patch v1.0.0
git push origin v1.0.0
```

The **Publish container images** workflow builds the backend and frontend for Linux AMD64 and ARM64. It pushes semantic-version and SHA tags to GHCR, records provenance attestations and then creates the GitHub Release. A failed image job prevents the release from being created.

Verify the workflow under **Actions > Publish container images**, then verify:

- the GitHub Release contains generated notes;
- both GHCR packages contain the exact release version;
- the production Compose configuration can pull both versioned images;
- the deployed health endpoint and smoke test pass.

## Failed release

Do not move or reuse a published version tag. Fix the failure on a branch, merge it, increment the patch version and publish a new tag. If the workflow fails before creating the GitHub Release, inspect and rerun the failed jobs only after confirming the original tagged source is still correct.

Application rollback uses the previous immutable image tags or digests as described in [`PRODUCTION.md`](PRODUCTION.md). Database migrations are forward-only; create and test a backup before every upgrade.
