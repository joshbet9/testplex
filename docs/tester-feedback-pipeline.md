# Testplex tester feedback

## Intake

The public GitHub issue forms provide separate Bug and Idea paths. Every form field is optional; if no game is selected, the report is routed to the shared lobby/global tracker. Bug reports ask for game, build, map/mode, phase, expected and actual behavior, reproduction steps, frequency, and optional media. Idea reports capture the game, player opportunity, proposed behavior, and any 1.8 reference. The forms state that reports are public and warn testers not to include credentials or private server details. Submit one independently actionable report at a time.

The `.github/workflows/triage-tester-feedback.yml` workflow runs for new, reopened, edited, and human-commented Bug/Idea reports. It applies `tester-feedback`, `tester-bug` or `tester-idea`, a `game:<slug>` label, the canonical `submitted` lifecycle status for new/reopened reports, and assigns the report to `joshbet9`. It also adds the report as a native GitHub sub-issue under the matching game tracker. Monster Maze modes share a tracker while the selected mode stays on the report. Human comment events re-check routing without resetting an existing lifecycle status; bot comments are ignored. New reports receive a comment linking to the parent. If the sub-issue relationship cannot be created, the action fails visibly so the report is not silently misrouted.

The Oracle Discord bridge is deployed separately as `oracle-v1.3.25`. It mirrors report discussions and lifecycle labels between GitHub and the matching Discord game thread, and archives the Discord thread when a report reaches Deployed.

The [tracker directory](tester-feedback-trackers.md) links to all 30 game trackers and the shared lobby/global tracker. GitHub Actions needs repository `Issues: write` and `Contents: read`; GitHub Issues must be enabled. The reports and discussion are public.

After intake, keep exactly one lifecycle status on every report:

1. **Submitted** — the report is in the maintainer queue. Triage it, inspect any linked work, and record the next step in a comment.
2. **In Progress** — a concrete implementation has started. Link the PR or commit; triage alone is not implementation.
3. **Waiting Tester** — a reporter needs to clarify reproduction details or verify a named staging build. State the exact question or test and build.
4. **Resolved** — the reporter confirms the behavior is fixed after staging verification.
5. **Deployed** — the confirmed change is deployed to production. Close the GitHub report; the Discord thread is then archived by the bridge.

Do not mark a report Resolved or Deployed from a build result alone. Do not merge or deploy from the hourly review task.

### Hourly review routine

Create a recurring ChatGPT Scheduled task named **Testplex feedback review** and connect it to the GitHub account that can read and update `joshbet9/testplex`. Set it to run hourly in `Australia/Brisbane`. The task reviews new Bug/Idea reports, edits, and comments since its previous run; reconciles the issue body with linked PRs/commits and recorded deployment evidence; asks for missing reproduction details; updates the single lifecycle status; and posts a concise GitHub comment when it changes status or needs a tester. It sends Josh a digest of changed issues and any blockers each run.

The hourly task can triage and update GitHub feedback, but it must not claim code changes, merge PRs, or deploy. Those remain a separate Codex implementation/review/deploy step. A report that needs code remains Submitted until implementation actually starts; it becomes In Progress once a PR is opened. Recheck ChatGPT Scheduled if runs stop, require approval, or the GitHub connection expires. The hourly task uses background model allowance even on quiet runs, so pause it when you do not want continuous monitoring.

This uses repository issues, so repository Issues must be enabled for the forms and assignment workflow to operate. Enabling Issues makes all submissions and discussion public.

## Hyper-V staging and Fly production promotion

The supported first deployment target is the isolated Hyper-V Testplex server. Build on `feat/testplex-modern-26.3-platform`, wait for the matching Java 25 / Paper 26.3 build to pass, then run **Deploy Testplex Modern Paper 26.3 to isolated Hyper-V** with `deploy_to_modern_test_server=true` and that successful build's run ID. The workflow transfers only changed plugin JAR deltas and required changed map assets, takes a modern-server-only backup, restarts `testplex-modern.service`, and requires runtime readiness before it reports success. Check the deployment summary and live service identity before asking testers to verify. A workflow run that merely built or uploaded artifacts is not a deployment.

Fly is the later production promotion target, not staging. PR #202 only provides a **non-deploying preflight**; the merged integration branch currently has no production Fly deploy workflow. Do not promote by rebuilding from a different source revision. First complete Hyper-V acceptance, create a release image from that exact source commit, record and verify its immutable image digest and plugin hashes, then require a protected `production` environment approval before any `fly deploy` or machine change.

The previous user-supplied Fly inventory showed app `monstermaze` with stopped legacy `mm18` and `mm21` Machines, a dedicated IPv4, and no volumes. That is historical and must be rechecked with `fly status`, `fly machine list`, `fly volumes list`, `fly ips list`, and `fly config show` immediately before production work. The prior authorization allows repurposing the app if it reduces costs, provided both legacy server images/config/plugins can be retrieved and redeployed. Save the old image digests, full app config, secrets references, process commands, and machine sizes; preserve verified backups. Keep `mm18` and `mm21` reversible until the Testplex target passes an external connection, wake/autostop, whitelist/admin, game start/end, and rollback acceptance.

Persistent player data is a production decision, not a staging behavior. The prior Fly preflight recorded that tester world/stats data should reset on deploy and initially proposed no volume. If persistent stats are required, select an external Oracle-backed database or add a mounted Fly volume before production; test backup/restore and migration first. Never put Fly tokens, database credentials, or private server details in issues, artifacts, or source. Store an app-scoped deploy token only in a protected GitHub `production` environment and require manual approval.
