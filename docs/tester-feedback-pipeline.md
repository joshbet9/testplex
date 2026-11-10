# Testplex tester feedback

## Intake

The public GitHub issue forms provide separate Bug and Idea paths. Bug reports ask for game, build, map/mode, phase, expected and actual behavior, reproduction steps, frequency, and optional media. Idea reports capture the game, player opportunity, proposed behavior, and any 1.8 reference. The forms state that reports are public and warn testers not to include credentials or private server details. Submit one independently actionable report at a time.

The `.github/workflows/triage-tester-feedback.yml` workflow recognizes these forms, applies `tester-feedback`, `tester-bug` or `tester-idea`, `needs-triage`, and a `game:<slug>` label, assigns the report to `joshbet9`, and adds it as a native GitHub sub-issue under the matching game tracker. Monster Maze modes share a tracker while the selected mode stays on the report. New reports receive a comment linking to the parent. If the sub-issue relationship cannot be created, the action fails visibly so the report is not silently misrouted.

The [tracker directory](tester-feedback-trackers.md) links to all 30 game trackers and the shared lobby/global tracker. GitHub Actions needs repository `Issues: write` and `Contents: read`; GitHub Issues must be enabled. The reports and discussion are public.

After intake, use the issue lifecycle:

1. `needs-triage`: acknowledge, reproduce on the named build, and add the result/build to the report.
2. For confirmed work, apply `accepted` and create a focused PR linked with `Fixes #<report>`; for an idea, discuss and record the decision before implementation.
3. Apply `in-progress` while work is active and `needs-tester-check` when a staging build is ready for the reporter.
4. Close through the linked PR after deployment/verification. Keep the parent tracker open so it remains the per-game inbox and progress view.

This uses repository issues, so repository Issues must be enabled for the forms and assignment workflow to operate. Enabling Issues makes all submissions and discussion public.

## Hyper-V staging and Fly production promotion

The supported first deployment target is the isolated Hyper-V Testplex server. Build on `feat/testplex-modern-26.3-platform`, wait for the matching Java 25 / Paper 26.3 build to pass, then run **Deploy Testplex Modern Paper 26.3 to isolated Hyper-V** with `deploy_to_modern_test_server=true` and that successful build's run ID. The workflow transfers only changed plugin JAR deltas and required changed map assets, takes a modern-server-only backup, restarts `testplex-modern.service`, and requires runtime readiness before it reports success. Check the deployment summary and live service identity before asking testers to verify. A workflow run that merely built or uploaded artifacts is not a deployment.

Fly is the later production promotion target, not staging. PR #202 only provides a **non-deploying preflight**; the merged integration branch currently has no production Fly deploy workflow. Do not promote by rebuilding from a different source revision. First complete Hyper-V acceptance, create a release image from that exact source commit, record and verify its immutable image digest and plugin hashes, then require a protected `production` environment approval before any `fly deploy` or machine change.

The previous user-supplied Fly inventory showed app `monstermaze` with stopped legacy `mm18` and `mm21` Machines, a dedicated IPv4, and no volumes. That is historical and must be rechecked with `fly status`, `fly machine list`, `fly volumes list`, `fly ips list`, and `fly config show` immediately before production work. The prior authorization allows repurposing the app if it reduces costs, provided both legacy server images/config/plugins can be retrieved and redeployed. Save the old image digests, full app config, secrets references, process commands, and machine sizes; preserve verified backups. Keep `mm18` and `mm21` reversible until the Testplex target passes an external connection, wake/autostop, whitelist/admin, game start/end, and rollback acceptance.

Persistent player data is a production decision, not a staging behavior. The prior Fly preflight recorded that tester world/stats data should reset on deploy and initially proposed no volume. If persistent stats are required, select an external Oracle-backed database or add a mounted Fly volume before production; test backup/restore and migration first. Never put Fly tokens, database credentials, or private server details in issues, artifacts, or source. Store an app-scoped deploy token only in a protected GitHub `production` environment and require manual approval.
