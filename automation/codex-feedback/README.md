# Testplex Codex feedback worker

This is an event-driven GitHub Issues worker intended for the Oracle VM. It uses
Sign in with ChatGPT plan authorization for Codex work, then prepares a **draft
PR only**. It never merges or deploys. A source edit is not treated as deployed
or resolved; Josh reviews the PR and handles the paired Hyper-V/Fly release.

## What it does

1. GitHub sends `issues` (`opened`, `reopened`) and `issue_comment` (`created`)
   webhooks to Oracle. The loopback receiver verifies HMAC, checks repository
   and `[Bug]`/`[Idea]` title, ignores bot comments, then durably queues it.
2. The worker fetches the latest open issue and all comments, serializes work per
   issue, clones `stable`, and runs Codex in a separate unprivileged account.
   Issue content is explicitly treated as untrusted. Bugs with sufficient
   evidence get a focused implementation; ideas wait for Josh's approval; a
   genuine blocker produces a specific question.
   For an Idea, code is discarded unless the triggering comment is a direct human
   comment by Josh that explicitly approves the concrete proposal. Accepted forms
   include `Approved`, `I approve`, `Agree`, and `I'll go with your recommendation`
   in response to a recommendation; leading Discord bridge display-name formatting
   is ignored. The publisher independently checks the same approval flag.
3. Codex receives no GitHub token, webhook secret, Fly token, SSH key or real
   ChatGPT token. Its app-server sends requests to the loopback OAuth proxy with
   a separate random local proxy key. Only that proxy reads and refreshes the protected ChatGPT
   credentials and forwards Responses API calls to the fixed OpenAI host.
4. The publisher independently checks the issue is still open, human discussion
   has not changed since review, patch size and paths are allowed, and no
   symlink/submodule is introduced. It creates a draft PR and comments the
   findings, or posts a specific question when no patch is appropriate.
5. Deployment credentials are not installed in this system. No event can merge,
   deploy, edit workflow/security instructions, or claim a release is deployed.

## Requirements and cost

- Ubuntu Oracle VM; Node.js 22+ with the built-in `node:sqlite` module; `git`,
  GitHub CLI (`gh`), Codex CLI and `sudo`.
- One GitHub webhook secret, a fine-grained read token for the issue worker, and
  a separate fine-grained publisher token for issue labels/comments, repository
  contents and pull requests. Those GitHub credentials never enter Codex.
- Josh completes Sign in with ChatGPT once on his Windows PC. Codex requests use
  the approved ChatGPT plan allowance shared with his other Codex use; they do
  not use an OpenAI API key. Sign-in with ChatGPT is a preview and account usage
  attribution/revocation for transferred VM sessions has current limitations.
- Five Linux service identities isolate webhook, issue worker, Codex runner,
  publisher and OAuth proxy privileges. The OAuth credential file remains
  owner-only. The Codex runner gets only the local proxy key, never the real
  ChatGPT access or refresh tokens.

## Install on Oracle

Review this directory and `install-oracle.sh` before running it. Copy the
`automation/codex-feedback` directory to Oracle, then run:

```sh
sudo bash automation/codex-feedback/install-oracle.sh
```

It installs root-owned Node scripts, creates isolated service users/groups and
systemd units, configures one narrowly scoped `sudo` rule for the fixed Codex
launcher, and writes private configuration templates. It deliberately does
**not** start services, configure Caddy, create a webhook, or authorize ChatGPT.
The installer requires Node 22+, `git`, `gh`, `codex`, `sudo`, and `openssl`.

Review and fill the four private environment files under
`/etc/testplex-feedback/`. Use a random webhook secret. Create separate
fine-grained GitHub tokens, grant only the permissions listed in the templates,
and keep them out of the Codex service account. Do not post secrets in issues or
this chat.

Add `caddy-webhook-snippet.txt` to the existing HTTPS site configuration. Do
not expose port 8787 directly. In GitHub repository settings, add a webhook at
`https://YOUR_EXISTING_ORACLE_DOMAIN/testplex/github/webhook`, use the same
secret, JSON payloads, and subscribe to Issues and Issue comments.

## Sign in locally and transfer credentials

After installing the Oracle files, read the stable host ID printed from:

```sh
sudo cat /var/lib/testplex-siwc/host-id
```

On Josh's Windows PC, from the repository checkout, run this in PowerShell
(replace the value with that host ID):

```powershell
node .\automation\codex-feedback\siwc-auth.mjs authorize `
  --host-id urn:uuid:<ORACLE-HOST-UUID> `
  --output "$env:LOCALAPPDATA\TestplexFeedbackAgent\credentials.json"
```

Sign into the intended ChatGPT account and approve the plan-use scope. The helper
validates the issued client ID, ID-token signature, issuer, audience, nonce,
expiry and permission scope; it writes the credential file privately and never
prints token values. Transfer it over the existing `ssh monster` alias without
opening it or pasting it into chat:

```powershell
scp "$env:LOCALAPPDATA\TestplexFeedbackAgent\credentials.json" monster:/tmp/testplex-feedback-credentials.json
```

Then on Oracle, install it with owner-only access and remove the transfer copy:

```sh
sudo install -o testplex-feedback-siwc -g testplex-feedback-siwc -m 0600 \
  /tmp/testplex-feedback-credentials.json /var/lib/testplex-siwc/credentials.json
sudo rm -f /tmp/testplex-feedback-credentials.json
```

Enable the four services only after the webhook, tokens, and credential file
are configured:

```sh
sudo systemctl enable --now testplex-feedback-oauth-proxy.service testplex-feedback-webhook.service testplex-feedback-worker.service testplex-feedback-publisher.service
```

Check service health and logs with `systemctl status` and `journalctl`. Do not
enable a service if a secret is still a `REPLACE_...` placeholder.

## Operations and safeguards

- Events run when a report is opened/reopened or a new human comment is added;
  a scheduled reconciliation can later be added as a missed-event safety net.
- Identical webhook deliveries and comments are deduplicated. Work on a single
  issue is serialized. Failed work is retried with backoff and becomes visible
  in the service log after the retry limit.

### Choose the model and trace work

The worker model is configured in `/etc/testplex-feedback/worker.env` as
`CODEX_MODEL`. It currently defaults to `gpt-6.1-sol`. Set it to another model
ID supported by the installed Codex CLI, then restart the worker; the value is
logged for each Codex turn.

Follow the full path from webhook receipt through publishing with:

```sh
sudo journalctl -f -u testplex-feedback-webhook.service \
  -u testplex-feedback-worker.service -u testplex-feedback-publisher.service
```

The log records the issue number, delivery ID, clone stage, selected model,
Codex stage, whether a patch was staged, and publish outcome. It does not log
the issue body or webhook secret. To inspect queue state without reading stored
payloads:

```sh
sudo node --input-type=module -e 'import {DatabaseSync} from "node:sqlite"; const db=new DatabaseSync("/var/lib/testplex-feedback/feedback-events.sqlite",{readOnly:true}); console.log(db.prepare("SELECT issue_number,event_name,action,state,attempts,received_at,last_error FROM feedback_events ORDER BY received_at DESC LIMIT 20").all()); db.close()'
```

To change the model:

```sh
sudoedit /etc/testplex-feedback/worker.env
sudo systemctl restart testplex-feedback-worker.service
```

Pending issue comments are retained in SQLite while the worker is stopped and
will resume after it starts.
- The draft publisher rechecks the human-discussion fingerprint before pushing.
  If Josh or a tester adds context while Codex works, that patch is rejected and
  the newer event is processed separately.
- Review the PR and CI, then deploy the same reviewed artifact to Hyper-V and
  Fly through the separate release process. The automation cannot access those
  deployments.
- To revoke plan-use access, disconnect the registered app in ChatGPT settings,
  stop `testplex-feedback-oauth-proxy.service`, and remove
  `/var/lib/testplex-siwc/credentials.json`.

## Current implementation state

The Oracle deployment was prepared on 2026-11-14. Node.js 22.23.5, GitHub CLI 2.104.0 and Codex CLI 0.169.0 are installed. The isolated service accounts, current PR service files and scripts are installed; the Caddy route is active and strips the `/testplex` prefix. Its route was verified to reach the receiver; unsigned requests receive HTTP 401.

The maintainer configured the private GitHub tokens, completed SIWC authorization and confirmed the GitHub webhook is configured. The validated SIWC credential is stored mode 0600 for its service account. All four services (OAuth proxy, webhook receiver, issue worker and draft publisher) are enabled and active. During startup, the worker and publisher exposed a shared SQLite permissions issue. The database is now group-writable (mode 0660), the webhook initializes it before the worker/publisher through systemd dependencies, and both consumers are running.

No signed GitHub delivery, real issue event, Codex processing turn or draft PR has yet been verified. Please check the webhook’s recent deliveries and send a safe test issue or comment before relying on automatic processing. PR #461 remains a draft and is not merged. No gameplay server was changed or deployed by this automation setup.
