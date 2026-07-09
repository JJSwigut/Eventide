# Eventide Development Workflow

## Roles

- Orchestrator thread: `Eventide Orchestrator` (`019f3da4-45e6-7c23-82f9-1388a76ce0fc`).
- Product repo: `/Users/swig/Development/Eventide`.
- Persistent ledger: `/Users/swig/thread-orchestrator/eventide.md`.
- Worker threads: one bounded implementation, review, or research lane per thread.

The orchestrator owns state, routing, permissions, release decisions, and worker prompts. Workers implement and verify assigned lanes. Workers do not create subworkers or edit the orchestrator ledger.

## Branches

- `main`: release branch. Merging or pushing here triggers Google Play release automation.
- `develop`: integration branch. Feature workers start from this branch.
- `swiggy/<slug>`: feature branches created by workers.

Current setup note: local `develop` tracks `origin/develop`.

## Feature Flow

When the owner says "add this feature":

1. Orchestrator reads `/Users/swig/thread-orchestrator/eventide.md`.
2. Orchestrator fetches latest refs and verifies `develop` is the intended base.
3. Orchestrator creates a new Eventide project worktree thread with starting state `develop`.
4. Orchestrator sends a bounded prompt that includes:
   - `Use $thread-worker`.
   - Specific feature scope and out-of-scope areas.
   - Authorized local actions.
   - Whether pushing/opening a PR is authorized.
   - Required proof: `tools/eventide_verify.sh`, `tools/eventide_smoke.sh`, and any feature-specific proof.
   - Stop conditions for product decisions, credentials, public mutation, or verification blockers.
   - Result semantics: setup-only work such as fetching refs, creating a branch, dependency setup, or repo inspection is a checkpoint, not completion or blockage.
   - `blocked` must include an exact owner action, permission, credential, external state change, or verification blocker; workers must not report `blocked` with `Decision needed: none`.
5. Worker implements, verifies, and reports with proof.
6. If public mutation is authorized, worker pushes `swiggy/<slug>` and opens a PR into `develop`.
7. Orchestrator records the PR, proof, blockers, and next owner decision in the ledger.

## Worker Monitoring

Worker reports stay inside their worker thread until the orchestrator reads them. The orchestrator should poll active workers before reporting status to the owner, and should use a heartbeat automation when ongoing background monitoring is expected.

If a worker reports `blocked` while also saying no decision/action is needed, the orchestrator should treat that as an invalid setup checkpoint: read the worktree state, record the event in the ledger, and resume the original assignment.

## Release Flow

When the owner says "cut a release":

1. Orchestrator confirms release permission and reads the ledger.
2. Orchestrator verifies no active worker has unmerged release-critical work.
3. Orchestrator confirms `release-notes/<versionName>.md` exists and is suitable for GitHub and Google Play.
4. Owner reviews and updates Google Play Data Safety disclosures before publishing if app data flows, permissions, or third-party services changed.
5. Orchestrator runs `tools/eventide_release_check.sh` from `develop`.
6. Orchestrator creates a release PR from `develop` into `main`.
7. PR checks run for `main`.
8. After explicit owner approval, orchestrator merges the PR.
9. The push to `main` triggers `.github/workflows/android-release.yml`, which builds the signed AAB, creates a GitHub release, and uploads to Google Play.

## Local Verification

Run:

```bash
tools/eventide_verify.sh
```

This exports the local Android SDK path, then runs:

- `:app:compileDebugKotlin`
- `:app:spotlessCheck`
- `:app:testDebugUnitTest`
- `:app:assembleDebug`

## Emulator/Device Smoke

Run:

```bash
tools/eventide_smoke.sh
```

The smoke script prefers an already connected Android target. If none is connected and the `EventideSmoke` AVD exists, it starts that emulator headlessly, waits for boot, installs the debug APK, grants runtime permissions where possible, launches Eventide, asserts a rendered product state, and captures a screenshot under `build/smoke/`.

By default, `tools/eventide_smoke.sh` launches a debuggable-only fixture with `eventide.SMOKE_FIXTURE=true`. The fixture renders the real station-detail components with deterministic tide, NDBC buoy, and NWS forecast content so workers and release checks do not depend on live upstream marine availability. The script waits for the fixture entry point, opens the station detail, asserts marine/tide/forecast text, verifies the app process is still running, and checks the screenshot is not black or mostly blank before reporting the screenshot path.

Set `ANDROID_SERIAL` to force a specific target.

Set `EVENTIDE_FORCE_EMULATOR=1` to force the `EventideSmoke` emulator even when a physical device is connected. Set `EVENTIDE_STOP_EMULATOR=1` to shut down an emulator started by the script after the screenshot is captured.

Set `EVENTIDE_SMOKE_FIXTURE=0` only when intentionally smoke-testing the normal live map launch path.

## Release Check

Run:

```bash
tools/eventide_release_check.sh
```

This runs local verification, forced `EventideSmoke` smoke, and release checks:

- `tools/eventide_verify.sh`
- `EVENTIDE_FORCE_EMULATOR=1 EVENTIDE_STOP_EMULATOR=1 tools/eventide_smoke.sh`
- `:app:spotlessCheck`
- `:app:testReleaseUnitTest`
- `:app:lintVitalRelease`

## Pull Requests

Feature PRs target `develop`. Release PRs target `main` from `develop`.

Use `.github/pull_request_template.md` to include the verification commands, smoke target, and screenshot path in every PR.

## Worker Cleanup

The orchestrator should clean up worker threads and worktrees once they are no longer useful:

1. Read the worker's latest thread state before cleanup.
2. Record the outcome in `/Users/swig/thread-orchestrator/eventide.md`: status, proof, PR URL if any, smoke target, screenshot path, and remaining decision.
3. Archive completed worker threads.
4. Remove the worker worktree only when `git status --short --branch` is clean or the useful work is already preserved on a pushed branch/PR.
5. Keep any dirty, unpushed, or ambiguous worktree visible in the ledger as `needs-owner`.

Cleanup is not allowed to discard live product work. The old overlay worker at `/Users/swig/.codex/worktrees/70d8/Eventide` is dirty and remains live until it is recovered into a focused PR or explicitly discarded.

## Permission Boundaries

Local-only setup and verification can proceed in worker threads when assigned. The following require explicit owner approval in the current task:

- Pushing branches.
- Opening, editing, commenting on, or closing PRs.
- Merging PRs.
- Triggering or approving deploy/release workflows.
- Using or changing credentials.
- Deleting branches, tags, worktrees, releases, or artifacts.

## Existing Work To Reconcile

No known dirty worker worktree currently needs recovery. The previous overlay worker was recovered into `develop` and archived.
