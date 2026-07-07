# Eventide Agent Instructions

Eventide is an Android/Kotlin Compose app. Treat this repository as the product repo for the Tides app.

## Branch Model

- `main` is the release branch. A push or merge to `main` triggers the Android release workflow.
- `develop` is the integration branch. Feature work starts from latest `develop`.
- Feature branches should use the default Codex branch prefix: `swiggy/<short-task-slug>`.
- Pull requests for feature work target `develop`.
- Release pull requests target `main` from `develop`.

Do not push, open PRs, merge, deploy, publish, or trigger releases unless the current task explicitly authorizes public/external mutation.

## Worker Setup

For implementation work delegated by the Eventide orchestrator:

1. Use `$thread-worker`.
2. Work from a Codex project worktree based on `develop`.
3. Keep edits scoped to the assigned lane.
4. Preserve unrelated user or worker changes.
5. Run local verification before reporting completion.

Use these commands from the repository root:

```bash
tools/eventide_verify.sh
tools/eventide_smoke.sh
tools/eventide_release_check.sh
```

`tools/eventide_verify.sh` runs compile, formatting, unit tests, and a debug assemble. `tools/eventide_smoke.sh` builds, installs, launches the app, and captures a screenshot using a connected Android device or the `EventideSmoke` emulator.
`tools/eventide_release_check.sh` runs the local verification gate, forced emulator smoke, and release checks.

If emulator/device verification cannot be performed honestly, report the blocker with the exact command output instead of claiming completion.

## Release Setup

To cut a release, the orchestrator should:

1. Confirm `develop` has the intended changes and required PR checks.
2. Create a release PR from `develop` into `main`.
3. Require `tools/eventide_release_check.sh` proof before merge.
4. Merge to `main` only with explicit owner approval.
5. Let the `Android CI/CD Release` workflow build, create the GitHub release, and upload to Google Play.

Required GitHub release secrets/vars:

- `KEYSTORE_BASE64`
- `APP_KEY_ALIAS`
- `APP_KEY_PASSWORD`
- `APP_STORE_PASSWORD`
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`
- `PLAY_TRACK`
- `PLAY_RELEASE_STATUS`
