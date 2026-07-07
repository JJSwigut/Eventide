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

## Worker Cleanup

When a worker is done, the orchestrator should close the loop before creating more work:

1. Read the worker's latest report and record the result, proof, PR URL, and any screenshot path in `/Users/swig/thread-orchestrator/eventide.md`.
2. Confirm the worker worktree is safe to remove:
   - Clean, no useful local changes: archive the thread and remove the worktree.
   - Pushed PR branch with no untracked local-only work: archive the thread and remove the worktree after the PR/proof is recorded.
   - Dirty, unpushed, or ambiguous work: keep the worktree and mark it `needs-owner` in the ledger.
3. Do not delete a worktree that contains unrecovered product work, failed verification artifacts needed for debugging, or unrelated user changes.
4. Never use forced deletion/reset for cleanup unless the owner explicitly approves discarding the specific worktree.

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
