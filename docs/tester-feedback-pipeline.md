# Testplex tester feedback

## Intake and issue structure

The public GitHub issue forms provide separate Bug and Idea paths. Every form field is optional. If a game is unknown or omitted, the intake workflow routes it to the shared lobby/global tracker. Reports become native GitHub sub-issues of the matching game tracker; Monster Maze modes use the shared Monster Maze tracker while the selected mode remains on the report.

The `.github/workflows/triage-tester-feedback.yml` workflow labels and assigns new/reopened reports, applies one canonical lifecycle status, preserves the status on edits and human comments, and ignores bot comments to avoid loops. The tracker directory links all 30 game trackers and the shared lobby/global tracker. The reports and discussion are public; never include passwords, tokens, private connection details, player personal data, or secrets in an issue.

The [tracker directory](tester-feedback-trackers.md) records the game-to-parent mapping. GitHub Actions requires `Issues: write` and `Contents: read` for routing, native sub-issue creation, and comments. GitHub Issues must be enabled.

## Required Codex review

Repository-wide instructions for thorough issue work are in [AGENTS.md](../AGENTS.md). Read those before responding to any report. In brief:

- Inspect the complete issue thread, game parent, current repository state, original source where available, modern code path, related reports, PR/CI evidence, and deployment evidence before deciding what to do.
- Do the code investigation and implementation for actionable, well-specified bugs. Do not stop at a vague acknowledgement or ask for information already present or inferable from code/source/logs.
- For genuine blockers, ask a precise question that explains what the current implementation does and why the missing answer changes the fix. Continue independent work.
- For ideas, provide a concrete proposal and wait for Josh's approval before implementation.
- Implement focused changes with behavior-level tests. Report exact files, root cause, checks, and limitations. Never describe a build as live verification.
- Consolidate exact duplicates only. Keep distinct symptoms in their own issues and cross-link related reports.
- Each report keeps exactly one status: `submitted`, `in-progress`, `waiting-tester`, `resolved`, or `deployed`. Use `owner-only-staging` when only private Hyper-V staging has a verified deployment; Josh is the only tester with access there.
- Do not mark a report Resolved without reporter confirmation. Do not mark Deployed or close until all required production targets have verified the exact release.

## Event-triggered Codex automation

The intended review trigger is each new/reopened Bug or Idea report and each new direct human GitHub comment. The intake workflow routes and labels reports; Codex must reload the full current discussion, game parent, related reports, code, source, and linked CI/deployment evidence. Serialize review per issue and ignore bot events to prevent loops. A missed-event reconciliation can be added as a safety net; hourly polling is not the primary workflow.

### Use Josh's ChatGPT Plus allowance

Do not activate the initial `openai/codex-action` prototype in this PR as the final design. It requires `OPENAI_API_KEY` and would use separately billed API usage. Do not add that key or merge the API-backed version unless Josh explicitly chooses API billing.

OpenAI now documents Sign in with ChatGPT plan usage for eligible Plus accounts in open-source and self-hosted applications, including a Codex app-server configured with an authorized ChatGPT-plan OAuth token. The intended direction is a signed GitHub event bridge to a Codex app-server worker on the existing self-hosted Oracle VM. Josh must complete a one-time browser sign-in and consent flow; refresh credentials must then be stored securely and refreshed on the Oracle host. This should consume the ChatGPT plan's included allowance and its limits, not API-key billing. Verify the current eligibility and integration requirements before implementation.

The event bridge needs signature verification, replay protection, per-issue serialization, and a durable event cursor. Codex runs read-only with respect to GitHub credentials; a separate publisher creates a draft PR after the patch is checked. Issue events must never receive Fly or Hyper-V deploy credentials, and must never merge or deploy. A reviewed release workflow remains separate.

The initial API-key GitHub Action and its workflow/prompt files are a prototype only: they are not active, have not passed Actions validation, and must be replaced or removed before PR #461 is considered merge-ready. The Plus-authenticated Oracle worker, OAuth lifecycle, GitHub event delivery, draft PR publication, Discord-bridge event handoff, and end-to-end event test are not implemented yet.

OpenAI API calls use separate API billing; ChatGPT subscription and API billing are separate. Do not assume a ChatGPT Plus subscription pays for an API key. See [ChatGPT billing and API billing](https://help.openai.com/en/articles/9039756-managing-billing-for-chatgpt-and-the-api-platform), [Sign in with ChatGPT plan usage](https://developers.openai.com/siwc/token-sharing-open-source), and [Codex app-server OAuth configuration](https://developers.openai.com/siwc/token-sharing-open-source/codex-app-server).

## Paired staging and production deployment

Josh has directed that approved fixes be deployed to **both** private Hyper-V staging and Fly production. Use one reviewed source commit and the same immutable artifact identity for both. Check the current workflow files, successful CI artifact, target app/server identity, persistent storage, and runtime before deploying; old inventories and remembered run IDs are not authoritative.

The isolated Hyper-V deployment workflow transfers changed plugin/map artifacts, backs up the modern server state, restarts its service, and checks runtime readiness. Hyper-V is private staging and only Josh can access it. A staging-only deployment is not the complete delivery: apply `owner-only-staging`, state which production deployment remains pending, and do not ask public testers to verify a build they cannot access.

Fly is the tester-facing production environment. Preserve the attached persistent data volume and its player statistics. Never delete, detach, replace, or reformat it as a storage cleanup or deployment shortcut. Before storage operations, inspect machine and volume attachments, free space, backups/snapshots, and the exact config. Do not treat old Fly inventories as current. If a safe deployment path for either target is unavailable, finish code/PR work and report the precise blocker rather than improvise a direct deployment.

Do not say the fix is deployed until the same candidate has passed required checks and both Hyper-V and Fly runtime identities are verified. Record commit, artifact hashes, deployment run IDs, target service/machine, and rollback point. After deployment, comment on each issue and its Discord mirror with the exact change, automated checks, both target identities, and a concrete manual test. Leave the report `waiting-tester` until Josh/tester confirms. Move it to `resolved` only after that confirmation; `deployed` is the production release state and then the issue can be closed.

Never make an unreviewed issue-event change directly to a live server. Preserve production data and keep rollback available. Do not take control of Josh's desktop/Minecraft session or restart an active server while Josh has said he is playing or testing.
