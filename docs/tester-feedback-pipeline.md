# Testplex tester feedback

## Intake

The public GitHub issue forms provide separate Bug and Idea paths. Bug reports ask for game, build, map/mode, phase, expected and actual behavior, reproduction steps, frequency, and optional media. Idea reports capture the game, player opportunity, proposed behavior, and any 1.8 reference. The forms warn testers that reports are public and to leave out credentials and private server details.

The `.github/workflows/triage-tester-feedback.yml` workflow recognizes submissions from these forms, applies `tester-feedback`, `tester-bug` or `tester-idea`, and `needs-triage`, then assigns the issue to `joshbet9`. Assignment creates the normal GitHub notification and gives each report a direct development owner.

After triage, use the issue lifecycle:

1. `needs-triage`: reproduce on the named build and record findings.
2. `accepted`: confirmed bug or approved idea.
3. Link the implementation PR to the issue and add a game label.
4. Close through the linked PR; ask the tester to recheck when live verification is needed.

This uses repository issues, so repository Issues must be enabled for the forms and assignment workflow to operate. Enabling Issues makes all submissions and discussion public.

## Deployment promotion

The existing Hyper-V workflow remains the staging environment. It builds and tests one Java 25 / Paper 26.3 artifact, deploys only changed plugin artifacts to the isolated VM, and verifies startup/readiness. A Fly tester promotion should consume that same successful artifact/image digest after an explicit production-environment approval; it must not rebuild between staging and promotion.

The repository currently has no Fly app configuration or Fly deployment workflow. The app name `monstermaze` refers to the existing standalone Monster Maze server and is a candidate to repurpose if a separate tester app would add cost. Before replacing it, inspect its Machine, image, region, volume, ports, secrets and current data; record the exact prior image digest; take and verify a volume backup; and prove the documented rollback can restore the prior app. Keep the current standalone server recoverable until a tester deployment passes external connection, join, game-start, shutdown, and persistence checks.

Do not place Fly credentials in files or report bodies. Add a least-privilege Fly deploy token as a GitHub Actions secret and use a protected `production` environment with manual approval. Fly app/volume costs and the existing app's details must be checked in the Fly account before promotion.
