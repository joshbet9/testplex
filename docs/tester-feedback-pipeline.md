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

Create a recurring **Codex app Automation** named **Testplex feedback review**, scoped to the `joshbet9/testplex` workspace or an equivalent Codex workspace with the GitHub connection enabled. Set it to run hourly in `Australia/Brisbane`. The automation reviews new Bug/Idea reports, edits, and human comments since its previous run; checks linked trackers, PRs/commits, and recorded deployment evidence; asks for missing reproduction details; keeps exactly one lifecycle status; and posts a concise GitHub comment for meaningful status changes or questions. Each run sends Josh a short digest, including when nothing changed.

Codex Automations run Codex in the background and place finished results in the app's review queue. Keep GitHub issue write access connected for status and comment updates. Review the automation's permissions and its first few results. The task may triage and update feedback, but it must not implement code, merge PRs, or deploy. Those remain separate, explicit steps. A report stays Submitted until implementation actually starts; it becomes In Progress once a PR is opened. Pause or edit the automation if hourly reviews are too noisy or costly.

Use this task prompt:

> Review new and updated Bug/Idea reports in `joshbet9/testplex`, including human comments on open reports, since your previous check. Read the report, parent game tracker, linked PRs/commits, and deployment comments before changing anything. Keep exactly one lifecycle status per report. New reports start as Submitted. Use In Progress only when a concrete implementation PR has started; use Waiting Tester when a reproduction detail or named staging verification is needed; use Resolved only after the reporter confirms the fix; use Deployed only after production deployment is verified, then close the report. Ask one concise question when information is missing. Add a GitHub comment only for a meaningful status change, a specific question, or new implementation/deployment evidence; do not repeat comments or claim unverified fixes. Never implement code, merge, deploy, or claim code work. At the end, send Josh a short digest with changed issue links, status changes, questions waiting for him/testers, duplicate reports linked or consolidated, and blockers. If nothing changed, say there were no new reports or comments.


This uses repository issues, so repository Issues must be enabled for the forms and assignment workflow to operate. Enabling Issues makes all submissions and discussion public.

## Hyper-V staging and Fly production promotion

The supported first deployment target is the isolated Hyper-V Testplex server. Build on `feat/testplex-modern-26.3-platform`, wait for the matching Java 25 / Paper 26.3 build to pass, then run **Deploy Testplex Modern Paper 26.3 to isolated Hyper-V** with `deploy_to_modern_test_server=true` and that successful build's run ID. The workflow transfers only changed plugin JAR deltas and required changed map assets, takes a modern-server-only backup, restarts `testplex-modern.service`, and requires runtime readiness before it reports success. Check the deployment summary and live service identity before asking testers to verify. A workflow run that merely built or uploaded artifacts is not a deployment.

Fly is the later production promotion target, not staging. PR #202 only provides a **non-deploying preflight**; the merged integration branch currently has no production Fly deploy workflow. Do not promote by rebuilding from a different source revision. First complete Hyper-V acceptance, create a release image from that exact source commit, record and verify its immutable image digest and plugin hashes, then require a protected `production` environment approval before any `fly deploy` or machine change.

The previous user-supplied Fly inventory showed app `monstermaze` with stopped legacy `mm18` and `mm21` Machines, a dedicated IPv4, and no volumes. That is historical and must be rechecked with `fly status`, `fly machine list`, `fly volumes list`, `fly ips list`, and `fly config show` immediately before production work. The prior authorization allows repurposing the app if it reduces costs, provided both legacy server images/config/plugins can be retrieved and redeployed. Save the old image digests, full app config, secrets references, process commands, and machine sizes; preserve verified backups. Keep `mm18` and `mm21` reversible until the Testplex target passes an external connection, wake/autostop, whitelist/admin, game start/end, and rollback acceptance.

Persistent player data is a production decision, not a staging behavior. The prior Fly preflight recorded that tester world/stats data should reset on deploy and initially proposed no volume. If persistent stats are required, select an external Oracle-backed database or add a mounted Fly volume before production; test backup/restore and migration first. Never put Fly tokens, database credentials, or private server details in issues, artifacts, or source. Store an app-scoped deploy token only in a protected GitHub `production` environment and require manual approval.
