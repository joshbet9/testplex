#!/usr/bin/env bash
set -euo pipefail

if [[ ${EUID} -ne 0 ]]; then
  echo 'Run this installer with sudo.' >&2
  exit 1
fi

SOURCE_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
INSTALL_DIR=/opt/testplex-codex-feedback
if [[ "$SOURCE_DIR" == "$INSTALL_DIR" ]]; then
  echo 'Run the installer from a source checkout outside /opt/testplex-codex-feedback.' >&2
  exit 1
fi

node_major="$(node -p 'Number(process.versions.node.split(".")[0])')"
if (( node_major < 22 )); then
  echo 'Node.js 22 or newer is required.' >&2
  exit 1
fi
node --input-type=module -e 'import { DatabaseSync } from "node:sqlite"; new DatabaseSync(":memory:").close()' >/dev/null
command -v git >/dev/null
command -v gh >/dev/null
command -v codex >/dev/null
command -v sudo >/dev/null
command -v openssl >/dev/null

ensure_group() { getent group "$1" >/dev/null || groupadd --system "$1"; }
ensure_user() {
  local username="$1" groupname="$2" homedir="$3"
  id "$username" >/dev/null 2>&1 || useradd --system --gid "$groupname" --home-dir "$homedir" --create-home --shell /usr/sbin/nologin "$username"
}
ensure_group testplex-feedback-db
ensure_group testplex-feedback-work
ensure_group testplex-feedback-publish
ensure_group testplex-feedback-siwc
ensure_group testplex-feedback-proxy
ensure_user testplex-feedback-webhook testplex-feedback-db /var/lib/testplex-feedback
ensure_user testplex-feedback-worker testplex-feedback-db /var/lib/testplex-feedback-worker
ensure_user testplex-feedback-publisher testplex-feedback-db /var/lib/testplex-feedback-publisher
ensure_user testplex-codex-runner testplex-feedback-work /var/lib/testplex-codex-runner
ensure_user testplex-feedback-siwc testplex-feedback-siwc /var/lib/testplex-siwc
usermod -a -G testplex-feedback-work,testplex-feedback-publish testplex-feedback-worker
usermod -a -G testplex-feedback-publish testplex-feedback-publisher
usermod -a -G testplex-feedback-proxy testplex-codex-runner
usermod -a -G testplex-feedback-proxy testplex-feedback-siwc

install -d -o root -g root -m 0755 "$INSTALL_DIR"
install -d -o testplex-feedback-webhook -g testplex-feedback-db -m 2770 /var/lib/testplex-feedback
install -d -o testplex-feedback-worker -g testplex-feedback-work -m 2770 /var/lib/testplex-feedback-shared/workspaces
install -d -o testplex-feedback-worker -g testplex-feedback-work -m 2770 /var/lib/testplex-feedback-shared/contexts
install -d -o testplex-feedback-worker -g testplex-feedback-publish -m 2770 /var/lib/testplex-feedback-publisher/inbox
install -d -o testplex-codex-runner -g testplex-feedback-work -m 0750 /var/lib/testplex-codex-runner/.codex
install -d -o testplex-feedback-siwc -g testplex-feedback-siwc -m 0700 /var/lib/testplex-siwc
install -d -o root -g testplex-feedback-proxy -m 0750 /var/lib/testplex-feedback-proxy
install -d -o root -g root -m 0750 /etc/testplex-feedback

find "$INSTALL_DIR" -maxdepth 1 -type f -name '*.mjs' -delete
find "$SOURCE_DIR" -maxdepth 1 -type f -name '*.mjs' -exec install -o root -g root -m 0644 '{}' "$INSTALL_DIR/" \;
install -o root -g root -m 0644 "$SOURCE_DIR"/systemd/*.service /etc/systemd/system/

install -o root -g root -m 0755 /dev/stdin /usr/local/sbin/testplex-feedback-run-codex <<'RUNNER'
#!/usr/bin/env bash
set -euo pipefail
if [[ $# -ne 2 ]]; then exit 64; fi
workspace="$(realpath -e -- "$1")"
context="$(realpath -e -- "$2")"
case "$workspace" in /var/lib/testplex-feedback-shared/workspaces/*) ;; *) echo 'workspace outside feedback root' >&2; exit 65 ;; esac
case "$context" in /var/lib/testplex-feedback-shared/contexts/*.md) ;; *) echo 'context outside feedback root' >&2; exit 65 ;; esac
[[ -d "$workspace/.git" && -f "$context" ]] || { echo 'workspace/context missing' >&2; exit 66; }
codex_bin="$(command -v codex)"
access_token="$(cat /var/lib/testplex-feedback-proxy/proxy.key)"
exec /usr/bin/env -i \
  PATH=/usr/local/bin:/usr/bin:/bin \
  HOME=/var/lib/testplex-codex-runner \
  CODEX_HOME=/var/lib/testplex-codex-runner/.codex \
  CODEX_BIN="$codex_bin" \
  CODEX_MODEL=gpt-6.1-sol \
  SIWC_PROXY_URL=http://127.0.0.1:8768/v1 \
  ACCESS_TOKEN="$access_token" \
  LANG=C.UTF-8 LC_ALL=C.UTF-8 \
  /usr/bin/node /opt/testplex-codex-feedback/run-codex-turn.mjs "$workspace" "$context"
RUNNER
chown root:root /usr/local/sbin/testplex-feedback-run-codex
chmod 0755 /usr/local/sbin/testplex-feedback-run-codex

printf '%s\n' 'testplex-feedback-worker ALL=(testplex-codex-runner) NOPASSWD: /usr/local/sbin/testplex-feedback-run-codex' > /etc/sudoers.d/testplex-feedback-worker
chmod 0440 /etc/sudoers.d/testplex-feedback-worker
visudo -cf /etc/sudoers.d/testplex-feedback-worker >/dev/null

write_template() {
  local file="$1"
  if [[ ! -e "$file" ]]; then
    install -o root -g root -m 0600 /dev/null "$file"
    cat > "$file"
  fi
}
write_template /etc/testplex-feedback/webhook.env <<'ENV'
GITHUB_WEBHOOK_SECRET=REPLACE_WITH_RANDOM_64_HEX_CHARACTERS
GITHUB_REPOSITORY=joshbet9/testplex
WEBHOOK_HOST=127.0.0.1
WEBHOOK_PORT=8787
FEEDBACK_DATABASE=/var/lib/testplex-feedback/feedback-events.sqlite
ENV
write_template /etc/testplex-feedback/worker.env <<'ENV'
GITHUB_READ_TOKEN=REPLACE_WITH_FINE_GRAINED_READ_ONLY_TOKEN
GITHUB_REPOSITORY=joshbet9/testplex
GITHUB_DEFAULT_BRANCH=stable
FEEDBACK_DATABASE=/var/lib/testplex-feedback/feedback-events.sqlite
FEEDBACK_WORK_ROOT=/var/lib/testplex-feedback-shared
FEEDBACK_PUBLISHER_INBOX=/var/lib/testplex-feedback-publisher/inbox
CODEX_RUNNER_USER=testplex-codex-runner
ENV
write_template /etc/testplex-feedback/publisher.env <<'ENV'
GH_TOKEN=REPLACE_WITH_FINE_GRAINED_PUBLISH_TOKEN
GITHUB_DEFAULT_BRANCH=stable
FEEDBACK_DATABASE=/var/lib/testplex-feedback/feedback-events.sqlite
FEEDBACK_PUBLISHER_INBOX=/var/lib/testplex-feedback-publisher/inbox
ENV
write_template /etc/testplex-feedback/oauth-proxy.env <<'ENV'
SIWC_PROXY_HOST=127.0.0.1
SIWC_PROXY_PORT=8768
SIWC_HOST_ID_FILE=/var/lib/testplex-siwc/host-id
SIWC_CREDENTIALS_FILE=/var/lib/testplex-siwc/credentials.json
SIWC_PROXY_SHARED_KEY_FILE=/var/lib/testplex-feedback-proxy/proxy.key
ENV

if [[ ! -e /var/lib/testplex-siwc/host-id ]]; then
  runuser -u testplex-feedback-siwc -- env SIWC_HOST_ID_FILE=/var/lib/testplex-siwc/host-id node "$INSTALL_DIR/host-id.mjs" >/dev/null
fi
if [[ ! -e /var/lib/testplex-feedback-proxy/proxy.key ]]; then
  proxy_key="$(openssl rand -hex 32)"
  printf '%s\n' "$proxy_key" | install -o root -g testplex-feedback-proxy -m 0440 /dev/stdin /var/lib/testplex-feedback-proxy/proxy.key
fi
chown -R testplex-feedback-siwc:testplex-feedback-siwc /var/lib/testplex-siwc
chmod 0700 /var/lib/testplex-siwc
chmod 0600 /etc/testplex-feedback/*.env
systemctl daemon-reload

cat <<'NEXT'
Service files and isolated accounts are installed. Services were deliberately not enabled or started.

Next:
  1. Configure /etc/testplex-feedback/*.env with a random webhook secret, a read-only
     fine-grained GitHub token for the worker, and a publisher token limited to issue
     comments/labels, contents and pull requests. Keep the publisher token out of Codex.
  2. Add the Caddy route from caddy-webhook-snippet.txt to the existing HTTPS site and
     configure the GitHub Issues + Issue comments webhook to its public URL.
  3. On Josh's Windows PC, authorize using the host ID in /var/lib/testplex-siwc/host-id;
     transfer the protected credentials file to /var/lib/testplex-siwc/credentials.json,
     then set its owner to testplex-feedback-siwc and mode 0600.
  4. Enable and start the four units only after those steps are complete.
NEXT
