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

The intended automation is triggered by a new/reopened Bug or Idea report and each new human comment on an open Bug/Idea report. The intake workflow handles issue routing and labels; the Codex workflow reloads the complete current report/thread and performs investigation. Events are serialized by issue and bot-authored comments are ignored. A missed-event reconciliation schedule may be used as a safety net; hourly polling is not the primary workflow.

The Codex workflow uses the official `openai/codex-action` in a restricted workspace. It may investigate and prepare a focused implementation as a **draft PR** when a bug is actionable. Ideas wait for Josh's approval. Repository automation/instruction files are protected from issue-driven edits. Codex receives no GitHub write token or deployment credential; a separate workflow step can publish a draft PR and issue summary. No event run may merge or deploy a change. The normal PR checks and release process remain mandatory.

Activation requires a GitHub Actions secret named `OPENAI_API_KEY`. Calls use the OpenAI API and are billed separately from ChatGPT/Codex subscription usage. Public reports/comments can trigger runs, so keep the workflow scoped to Bug/Idea reports, ignore bots, serialize runs, and monitor Actions/API usage. If the secret is absent or the run fails, the issue must remain open and the workflow failure must be visible; automation must not imply review or implementation occurred.

## Paired staging and production deployment

Josh has directed that approved fixes be deployed to **both** private Hyper-V staging and Fly production. Use one reviewed source commit and the same immutable artifact identity for both. Check the current workflow files, successful CI artifact, target app/server identity, persistent storage, and runtime before deploying; old inventories and remembered run IDs are not authoritative.

The isolated Hyper-V deployment workflow transfers changed plugin/map artifacts, backs up the modern server state, restarts its service, and checks runtime readiness. Hyper-V is private staging and only Josh can access it. A staging-only deployment is not the complete delivery: apply `owner-only-staging`, state which production deployment remains pending, and do not ask public testers to verify a build they cannot access.

Fly is the tester-facing production environment. Preserve the attached persistent data volume and its player statistics. Never delete, detach, replace, or reformat it as a storage cleanup or deployment shortcut. Before storage operations, inspect machine and volume attachments, free space, backups/snapshots, and the exact config. Do not treat old Fly inventories as current. If a safe deployment path for either target is unavailable, finish code/PR work and report the precise blocker rather than improvise a direct deployment.

Do not say the fix is deployed until the same candidate has passed required checks and both Hyper-V and Fly runtime identities are verified. Record commit, artifact hashes, deployment run IDs, target service/machine, and rollback point. After deployment, comment on each issue and its Discord mirror with the exact change, automated checks, both target identities, and a concrete manual test. Leave the report `waiting-tester` until Josh/tester confirms. Move it to `resolved` only after that confirmation; `deployed` is the production release state and then the issue can be closed.

Never make an unreviewed issue-event change directly to a live server. Preserve production data and keep rollback available. Do not take control of Josh's desktop/Minecraft session or restart an active server while Josh has said he is playing or testing.
