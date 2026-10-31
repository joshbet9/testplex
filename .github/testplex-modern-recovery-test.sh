#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HELPER="$ROOT/testplex-modern-recovery.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
BIN="$TMP/bin"; mkdir -p "$BIN"
SERVER="$TMP/server"; STATE="$TMP/state"; BACKUP="$STATE/backups/transaction-test"
LOCK="$TMP/deploy.lock"; UNIT="$TMP/systemd/testplex-modern.service"
mkdir -p "$SERVER/plugins/ArcadeMechanicsModern" "$SERVER/logs" "$SERVER/mm_void" "$BACKUP" "$(dirname "$UNIT")"

cat > "$BIN/systemctl" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
cmd="$1"; shift || true
statefile="$TESTPLEX_STUB_ACTIVE"
[[ "${*: -1}" != *reborn* ]] || statefile="$TESTPLEX_STUB_REBORN_ACTIVE"
case "$cmd" in
  is-active) [[ -f "$statefile" ]] ;;
  is-enabled) [[ -f "$TESTPLEX_STUB_ENABLED" ]] && cat "$TESTPLEX_STUB_ENABLED" || printf 'enabled\n' ;;
  stop) rm -f "$statefile" ;;
  start)
    [[ "${TESTPLEX_FAIL_START:-false}" != true ]] || exit 17
    printf active > "$statefile"
    [[ "$statefile" != "$TESTPLEX_STUB_REBORN_ACTIVE" ]] || exit 0
    printf 'Paper server Done (3.0s)!\n' >> "$TESTPLEX_SERVER_DIR/logs/latest.log"
    printf 'status=READY\nmodule=arcade-lobby\nmanager=testplex-arcade\nworld=testplex_arcade_lobby_v1\nplane=100x100\nfloor_y=100\nsource_sha=%s\nnonce=123e4567-e89b-12d3-a456-426614174000\ninvocation_id=0123456789abcdef0123456789abcdef\nprocess_id=4242\n' \
      "$(cat "$TESTPLEX_SERVER_DIR/.testplex-modern-deployed-build")" > "$TESTPLEX_SERVER_DIR/plugins/ArcadeMechanicsModern/.testplex-arcade-readiness.properties"
    ;;
  enable) printf enabled > "$TESTPLEX_STUB_ENABLED" ;;
  disable)
    if [[ ! -e "$TESTPLEX_UNIT_FILE" ]]; then printf not-found > "$TESTPLEX_STUB_ENABLED"; else printf disabled > "$TESTPLEX_STUB_ENABLED"; fi ;;
  mask) printf masked > "$TESTPLEX_STUB_ENABLED" ;;
  show)
    case "$1" in
      --property=InvocationID) printf '0123456789abcdef0123456789abcdef\n' ;;
      --property=MainPID) printf '4242\n' ;;
      --property=ActiveEnterTimestampMonotonic) printf '1000000\n' ;;
      --property=WorkingDirectory) printf '%s\n' "$TESTPLEX_SERVER_DIR" ;;
      --property=ExecStart) printf '/opt/java/bin/java -jar paper.jar --nogui\n' ;;
      --property=LoadState) printf 'loaded\n' ;;
    esac
    ;;
  daemon-reload) : ;;
  *) echo "unexpected systemctl $cmd $*" >&2; exit 2 ;;
esac
STUB
cat > "$BIN/ss" <<'STUB'
#!/usr/bin/env bash
printf 'State Recv-Q Send-Q Local Address:Port Peer Address:Port\n'
if [[ -f "$TESTPLEX_STUB_ACTIVE" ]]; then printf 'LISTEN 0 128 0.0.0.0:25568 0.0.0.0:*\n'; fi
STUB
cat > "$BIN/timeout" <<'STUB'
#!/usr/bin/env bash
while [[ "$1" == --* ]]; do shift; done
shift
exec "$@"
STUB
cat > "$BIN/flock" <<'STUB'
#!/usr/bin/env bash
while [[ "$1" == -* ]]; do
  case "$1" in -w|-n) shift; [[ "$1" =~ ^[0-9]+$ ]] && shift ;; *) shift ;; esac
done
shift # lock file; these single-process fixtures exercise lifecycle semantics, not kernel flock behavior.
exec "$@"
STUB
cat > "$BIN/mv" <<'STUB'
#!/usr/bin/env bash
if [[ "${TESTPLEX_FAIL_RESTORE_MOVE:-false}" == true && "$*" == *'.testplex-restore-'*'plugins'* ]]; then
  echo 'injected restore move failure' >&2; exit 19
fi
exec /usr/bin/mv "$@"
STUB
chmod +x "$BIN"/*

export PATH="$BIN:$PATH"
export TESTPLEX_SERVER_DIR="$SERVER" TESTPLEX_STATE_DIR="$STATE" TESTPLEX_LOCK_FILE="$LOCK"
export TESTPLEX_UNIT_FILE="$UNIT" TESTPLEX_SYSTEMCTL=systemctl TESTPLEX_SS=ss TESTPLEX_TIMEOUT=timeout
export TESTPLEX_STUB_ACTIVE="$TMP/service-active" TESTPLEX_STUB_ENABLED="$TMP/service-enabled" TESTPLEX_VERIFY_ATTEMPTS=1 TESTPLEX_VERIFY_SLEEP=0
export TESTPLEX_STUB_REBORN_ACTIVE="$TMP/reborn-active"
export TESTPLEX_PROC_ROOT="$TMP/proc"
mkdir -p "$TESTPLEX_PROC_ROOT/4242"
printf 'java -jar paper.jar\0--nogui\0' > "$TESTPLEX_PROC_ROOT/4242/cmdline"
printf enabled > "$TESTPLEX_STUB_ENABLED"
export TESTPLEX_LOCK_WAIT_SECONDS=1

old_sha=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
target_sha=bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
mkdir -p "$TMP/old/plugins/ArcadeMechanicsModern" "$TMP/old/mm_void"
printf 'old plugin payload\n' > "$TMP/old/plugins/ArcadeMechanicsModern/plugin.dat"
printf 'source_sha=%s\nnonce=123e4567-e89b-12d3-a456-426614174000\n' "$old_sha" \
  > "$TMP/old/plugins/ArcadeMechanicsModern/.testplex-modern-deploy-identity.properties"
printf 'old map payload\n' > "$TMP/old/mm_void/region.dat"
printf 'old paper\n' > "$TMP/old/paper.jar"
printf '%s\n' "$old_sha" > "$TMP/old/.testplex-modern-deployed-build"
printf 'old properties\n' > "$TMP/old/server.properties"
mkdir -p "$TMP/old-unit-dir"
printf 'old unit\n' > "$TMP/old-unit-dir/testplex-modern.service"
tar -cpf "$BACKUP/unit.tar" -C "$TMP/old-unit-dir" testplex-modern.service
printf 'new plugin payload\n' > "$SERVER/plugins/ArcadeMechanicsModern/plugin.dat"
printf 'new map payload\n' > "$SERVER/mm_void/region.dat"
printf 'new paper\n' > "$SERVER/paper.jar"
printf '%s\n' "$target_sha" > "$SERVER/.testplex-modern-deployed-build"
printf 'new properties\n' > "$SERVER/server.properties"
printf 'new unit\n' > "$UNIT"
printf 'new log\n' > "$SERVER/logs/latest.log"
tar -cpf "$BACKUP/server-artifacts.tar" -C "$TMP/old" plugins paper.jar .testplex-modern-deployed-build server.properties mm_void
sha256sum "$BACKUP/server-artifacts.tar" > "$BACKUP/server-artifacts.sha256"
sha256sum "$BACKUP/unit.tar" > "$BACKUP/unit.sha256"

write_txn() {
  local status="$1" complete="$2" replaced="$3"
  mkdir -p "$STATE"
  cat > "$STATE/current.properties" <<EOF
deployment_id=transaction-test
target_sha=$target_sha
backup_dir=$BACKUP
previous_sha=$old_sha
previous_state=deployed
previous_active=true
previous_enabled=enabled
previous_reborn_active=false
server_dir_existed=true
had_old_unit=true
had_old_paper=true
had_old_plugins=true
prior_files=paper.jar,.testplex-modern-deployed-build,server.properties
worlds=mm_void
backup_sha=$(awk '{print $1}' "$BACKUP/server-artifacts.sha256")
backup_complete=$complete
replacement_started=$replaced
status=$status
EOF
  cp "$STATE/current.properties" "$BACKUP/transaction.properties"
}
run_helper() { bash "$HELPER" "$@"; }
assert_eq() { [[ "$1" == "$2" ]] || { echo "ASSERT expected '$2', got '$1'" >&2; exit 1; }; }
assert_file_text() { [[ "$(cat "$1")" == "$2" ]] || { echo "ASSERT unexpected contents: $1" >&2; exit 1; }; }

# Fresh process recovery models the original SSH/deployment process disappearing.
write_txn REPLACING true true
# Independent recovery must recover the current journal even when the retrying
# workflow's build SHA differs from the failed deployment's target SHA.
run_helper recover
run_helper assert-deployable >/dev/null
assert_file_text "$SERVER/plugins/ArcadeMechanicsModern/plugin.dat" 'old plugin payload'
assert_file_text "$SERVER/mm_void/region.dat" 'old map payload'
assert_file_text "$SERVER/paper.jar" 'old paper'
assert_file_text "$SERVER/.testplex-modern-deployed-build" "$old_sha"
grep -Fxq "restored_artifact_sha=$old_sha" "$STATE/current.properties"
grep -Fxq "runtime_sha=$old_sha" "$STATE/current.properties"
grep -Fxq 'status=RECOVERY_VERIFIED' "$STATE/current.properties"
[[ -f "$BACKUP/failed-artifacts/attempt-001/plugins-failed-deployment/ArcadeMechanicsModern/plugin.dat" ]]
[[ -f "$BACKUP/failed-artifacts/attempt-001/maps/mm_void-failed-deployment/region.dat" ]]
echo 'PASS fresh-session interrupted-deployment recovery and separate artifact/runtime SHA'

# Regression: an early grep -q match closes tar's output pipe. With pipefail,
# tar's resulting SIGPIPE makes a valid archive look incomplete. Make its member
# listing larger than the pipe buffer so the broken-pipe behaviour is deterministic.
LARGE_OLD="$TMP/large-old"
mkdir -p "$LARGE_OLD"
cp -a "$TMP/old/." "$LARGE_OLD/"
mkdir -p "$LARGE_OLD/plugins/filler"
for i in $(seq -w 1 12000); do
  : > "$LARGE_OLD/plugins/filler/file-$i"
done
tar -cpf "$BACKUP/server-artifacts.tar" -C "$LARGE_OLD" plugins paper.jar .testplex-modern-deployed-build server.properties mm_void
sha256sum "$BACKUP/server-artifacts.tar" > "$BACKUP/server-artifacts.sha256"
run_helper --locked set-field backup_sha "$(awk '{print $1}' "$BACKUP/server-artifacts.sha256")"
if tar -tf "$BACKUP/server-artifacts.tar" 2>/dev/null | grep -Eq '^\\.?/?plugins/?$'; then
  echo 'FAIL regression fixture did not trigger tar SIGPIPE under pipefail' >&2
  exit 1
fi
run_helper verify-snapshot >/dev/null
echo 'PASS valid large snapshot is accepted without tar/grep pipefail false-negative'

# Restore the small fixture for the remaining lifecycle assertions.
tar -cpf "$BACKUP/server-artifacts.tar" -C "$TMP/old" plugins paper.jar .testplex-modern-deployed-build server.properties mm_void
sha256sum "$BACKUP/server-artifacts.tar" > "$BACKUP/server-artifacts.sha256"
run_helper --locked set-field backup_sha "$(awk '{print $1}' "$BACKUP/server-artifacts.sha256")"

# Repeated recovery verifies existing state without moving restored data again.
attempts_before="$(cat "$BACKUP/recovery-attempts")"
run_helper recover "$target_sha"
assert_file_text "$SERVER/plugins/ArcadeMechanicsModern/plugin.dat" 'old plugin payload'
assert_eq "$(cat "$BACKUP/recovery-attempts")" "$attempts_before"
echo 'PASS repeated recovery is idempotent'

# A process dying mid-restore leaves the verified tar intact; retry restores safely.
write_txn RESTORING true true
printf 'partially restored\n' > "$SERVER/plugins/ArcadeMechanicsModern/plugin.dat"
export TESTPLEX_FAIL_RESTORE_MOVE=true
if run_helper recover "$target_sha" >/dev/null 2>&1; then echo 'FAIL injected restore failure unexpectedly succeeded' >&2; exit 1; fi
unset TESTPLEX_FAIL_RESTORE_MOVE
grep -Fxq 'status=RESTORING' "$STATE/current.properties"
run_helper recover "$target_sha"
assert_file_text "$SERVER/plugins/ArcadeMechanicsModern/plugin.dat" 'old plugin payload'
grep -Fxq 'status=RECOVERY_VERIFIED' "$STATE/current.properties"
echo 'PASS interrupted restoration and failed restoration are retryable'

# An incomplete snapshot must not mutate artifacts or start the service.
rm -f "$TESTPLEX_STUB_ACTIVE"
write_txn REPLACING false true
before="$(sha256sum "$SERVER/plugins/ArcadeMechanicsModern/plugin.dat" | awk '{print $1}')"
if run_helper recover "$target_sha" >/dev/null 2>&1; then echo 'FAIL incomplete backup unexpectedly succeeded' >&2; exit 1; fi
after="$(sha256sum "$SERVER/plugins/ArcadeMechanicsModern/plugin.dat" | awk '{print $1}')"
assert_eq "$before" "$after"
[[ ! -f "$TESTPLEX_STUB_ACTIVE" ]]
if run_helper assert-deployable >/dev/null 2>&1; then echo 'FAIL unresolved deployment transaction allowed a new deployment' >&2; exit 1; fi
echo 'PASS incomplete backup refuses restoration and service start'

# A checksum-valid archive that omits a map declared in its durable transaction is incomplete.
tar -cpf "$BACKUP/server-artifacts.tar" -C "$TMP/old" plugins paper.jar .testplex-modern-deployed-build server.properties
sha256sum "$BACKUP/server-artifacts.tar" > "$BACKUP/server-artifacts.sha256"
write_txn REPLACING true true
before="$(sha256sum "$SERVER/plugins/ArcadeMechanicsModern/plugin.dat" | awk '{print $1}')"
if run_helper recover "$target_sha" >/dev/null 2>&1; then echo 'FAIL backup missing a declared map unexpectedly succeeded' >&2; exit 1; fi
after="$(sha256sum "$SERVER/plugins/ArcadeMechanicsModern/plugin.dat" | awk '{print $1}')"
assert_eq "$before" "$after"
echo 'PASS checksum-valid but incomplete map backup is rejected'

# Restore the valid snapshot fixture for the remaining runtime failures.
tar -cpf "$BACKUP/server-artifacts.tar" -C "$TMP/old" plugins paper.jar .testplex-modern-deployed-build server.properties mm_void
sha256sum "$BACKUP/server-artifacts.tar" > "$BACKUP/server-artifacts.sha256"

# A failure to start/verify the restored service never claims successful rollback.
write_txn REPLACING true true
rm -f "$TESTPLEX_STUB_ACTIVE"
export TESTPLEX_FAIL_START=true
if run_helper recover "$target_sha" >/dev/null 2>&1; then echo 'FAIL injected service start failure unexpectedly succeeded' >&2; exit 1; fi
unset TESTPLEX_FAIL_START
grep -Fxq 'status=RESTORED' "$STATE/current.properties"
grep -Fxq 'runtime_sha=unknown' "$STATE/current.properties"
if run_helper assert-deployable >/dev/null 2>&1; then echo 'FAIL unresolved recovery state allowed a new deployment' >&2; exit 1; fi
echo 'PASS unavailable restored service leaves recovery unresolved'

# A different unresolved deployment cannot be recovered by this job's identity.
write_txn REPLACING true true
if run_helper recover cccccccccccccccccccccccccccccccccccccccc >/dev/null 2>&1; then echo 'FAIL mismatched deployment identity accepted' >&2; exit 1; fi
echo 'PASS deployment identity fences independent recovery'

# A deployment marked complete is only accepted after a fresh process/port/Arcade-readiness check.
write_txn DEPLOYMENT_VERIFIED true true
printf 'successful target artifact\n' > "$SERVER/plugins/ArcadeMechanicsModern/plugin.dat"
printf '%s\n' "$target_sha" > "$SERVER/.testplex-modern-deployed-build"
printf 'deployed Paper artifact\n' > "$SERVER/paper.jar"
nonce=123e4567-e89b-12d3-a456-426614174000
printf 'source_sha=%s\nnonce=%s\n' "$target_sha" "$nonce" > "$SERVER/plugins/ArcadeMechanicsModern/.testplex-modern-deploy-identity.properties"
printf 'status=READY\nmodule=arcade-lobby\nmanager=testplex-arcade\nworld=testplex_arcade_lobby_v1\nplane=100x100\nfloor_y=100\nsource_sha=%s\nnonce=%s\ninvocation_id=0123456789abcdef0123456789abcdef\nprocess_id=4242\n' "$target_sha" "$nonce" > "$SERVER/plugins/ArcadeMechanicsModern/.testplex-arcade-readiness.properties"
printf active > "$TESTPLEX_STUB_ACTIVE"
printf 'Paper server Done (3.0s)!\n' >> "$SERVER/logs/latest.log"
output="$(run_helper verify-deployment "$target_sha")"
grep -q "Deployment already verified: target artifact SHA=$target_sha; runtime SHA=$target_sha; process identity=VERIFIED" <<< "$output"
output="$(run_helper recover "$target_sha")"
grep -q "Deployment already verified: target artifact SHA=$target_sha; runtime SHA=$target_sha; process identity=VERIFIED" <<< "$output"
grep -Fxq "runtime_sha=$target_sha" "$STATE/current.properties"
assert_file_text "$SERVER/plugins/ArcadeMechanicsModern/plugin.dat" 'successful target artifact'
echo 'PASS completed deployment requires fresh live runtime attestation after lost SSH response'

# A terminal journal record alone cannot make a stopped server look successfully deployed.
rm -f "$TESTPLEX_STUB_ACTIVE"
if run_helper verify-deployment "$target_sha" >/dev/null 2>&1; then
  echo 'FAIL a terminal deployment journal was accepted while Paper was stopped' >&2; exit 1
fi
if run_helper recover "$target_sha" >/dev/null 2>&1; then
  echo 'FAIL recovery claimed a completed deployment while Paper was stopped' >&2; exit 1
fi
assert_file_text "$SERVER/plugins/ArcadeMechanicsModern/plugin.dat" 'successful target artifact'
printf active > "$TESTPLEX_STUB_ACTIVE"
output="$(run_helper verify-deployment "$target_sha")"
grep -q "runtime SHA=$target_sha; process identity=VERIFIED" <<< "$output"
echo 'PASS a deployment journal is not treated as live health when its service is down'

# A PREPARING transaction can recover a Modern service stopped by locked preflight.
write_txn PREPARING true false
sed -i 's/^previous_active=.*/previous_active=true/' "$STATE/current.properties"
cp "$STATE/current.properties" "$BACKUP/transaction.properties"
rm -f "$TESTPLEX_STUB_ACTIVE"
run_helper recover "$target_sha" >/dev/null
[[ -f "$TESTPLEX_STUB_ACTIVE" ]]
grep -Fxq "runtime_sha=$old_sha" "$STATE/current.properties"
echo 'PASS previously active Modern service restored after preflight stop'

# A running legacy process has no independent build attestation; its SHA stays unverified.
write_txn PREPARING true false
rm -f "$SERVER/plugins/ArcadeMechanicsModern/.testplex-modern-deploy-identity.properties"
printf active > "$TESTPLEX_STUB_ACTIVE"
output="$(run_helper recover "$target_sha")"
grep -q 'runtime SHA=UNVERIFIED' <<< "$output"
grep -Fxq 'runtime_sha=UNVERIFIED' "$STATE/current.properties"
! grep -Fxq "runtime_sha=$old_sha" "$STATE/current.properties"
echo 'PASS legacy runtime without identity attestation is explicitly unverified'

# The unresolved transaction gate refuses a new journal while both services are active.
write_txn RESTORING true true
printf active > "$TESTPLEX_STUB_ACTIVE"
printf active > "$TESTPLEX_STUB_REBORN_ACTIVE"
before_modern="$(cat "$TESTPLEX_STUB_ACTIVE")"; before_reborn="$(cat "$TESTPLEX_STUB_REBORN_ACTIVE")"
if run_helper --locked begin next-deployment "$target_sha" "$STATE/backups/next" "$old_sha" deployed true enabled true true true true true paper.jar,plugins mm_void >/dev/null 2>&1; then
  echo 'FAIL unresolved transaction allowed a new deployment journal' >&2; exit 1
fi
assert_eq "$(cat "$TESTPLEX_STUB_ACTIVE")" "$before_modern"
assert_eq "$(cat "$TESTPLEX_STUB_REBORN_ACTIVE")" "$before_reborn"
echo 'PASS unresolved transaction gate preserves both service states'

# Prior inactive Modern service and each enablement kind are restored without starting it.
for enablement in enabled disabled masked static indirect; do
  write_txn PREPARING true false
  rm -f "$TESTPLEX_STUB_ACTIVE" "$TESTPLEX_STUB_REBORN_ACTIVE"
  printf '%s' "$enablement" > "$TESTPLEX_STUB_ENABLED"
  sed -i "s/^previous_active=.*/previous_active=false/; s/^previous_enabled=.*/previous_enabled=$enablement/" "$STATE/current.properties"
  cp "$STATE/current.properties" "$BACKUP/transaction.properties"
  run_helper recover "$target_sha" >/dev/null
  [[ ! -f "$TESTPLEX_STUB_ACTIVE" ]] || { echo "FAIL inactive Modern service restarted for $enablement" >&2; exit 1; }
  assert_eq "$(cat "$TESTPLEX_STUB_ENABLED")" "$enablement"
done
echo 'PASS prior inactive service and enabled/disabled/masked/static/indirect restoration'

# Modern and Reborn activity are independent journal fields.
write_txn PREPARING true false
sed -i 's/^previous_active=.*/previous_active=false/; s/^previous_reborn_active=.*/previous_reborn_active=true/' "$STATE/current.properties"
cp "$STATE/current.properties" "$BACKUP/transaction.properties"
rm -f "$TESTPLEX_STUB_ACTIVE" "$TESTPLEX_STUB_REBORN_ACTIVE"
run_helper recover "$target_sha" >/dev/null
[[ ! -f "$TESTPLEX_STUB_ACTIVE" && -f "$TESTPLEX_STUB_REBORN_ACTIVE" ]]
echo 'PASS prior Reborn activity is restored independently of Modern state'

# A deployment without a verified prior SHA is refused before any service change.
printf active > "$TESTPLEX_STUB_ACTIVE"; printf active > "$TESTPLEX_STUB_REBORN_ACTIVE"
before_modern="$(cat "$TESTPLEX_STUB_ACTIVE")"; before_reborn="$(cat "$TESTPLEX_STUB_REBORN_ACTIVE")"
if run_helper --locked begin unknown-sha "$target_sha" "$STATE/backups/unknown" unknown deployed true enabled true true true true true paper.jar mm_void >/dev/null 2>&1; then
  echo 'FAIL unknown previous SHA accepted' >&2; exit 1
fi
assert_eq "$(cat "$TESTPLEX_STUB_ACTIVE")" "$before_modern"
assert_eq "$(cat "$TESTPLEX_STUB_REBORN_ACTIVE")" "$before_reborn"
echo 'PASS unknown previous SHA is rejected before mutation'

# First-install rollback restores exact prior absence and uses its distinct terminal outcome.
FIRST_SERVER="$TMP/first-server"; FIRST_STATE="$TMP/first-state"; FIRST_UNIT="$TMP/first-systemd/testplex-modern.service"
mkdir -p "$FIRST_SERVER/plugins" "$FIRST_SERVER/logs" "$FIRST_STATE/backups/first" "$(dirname "$FIRST_UNIT")"
FIRST_BACKUP="$FIRST_STATE/backups/first"
tar -cpf "$FIRST_BACKUP/server-artifacts.tar" --files-from /dev/null
sha256sum "$FIRST_BACKUP/server-artifacts.tar" > "$FIRST_BACKUP/server-artifacts.sha256"
printf 'first-install artifact\n' > "$FIRST_SERVER/paper.jar"
printf '%s\n' "$target_sha" > "$FIRST_SERVER/.testplex-modern-deployed-build"
TESTPLEX_SERVER_DIR="$FIRST_SERVER" TESTPLEX_STATE_DIR="$FIRST_STATE" TESTPLEX_UNIT_FILE="$FIRST_UNIT" \
TESTPLEX_STUB_ACTIVE="$TMP/first-active" TESTPLEX_STUB_ENABLED="$TMP/first-enabled" \
  bash "$HELPER" --locked begin first-install "$target_sha" "$FIRST_BACKUP" none none false not-found false false false false false '' ''
TESTPLEX_SERVER_DIR="$FIRST_SERVER" TESTPLEX_STATE_DIR="$FIRST_STATE" TESTPLEX_UNIT_FILE="$FIRST_UNIT" \
TESTPLEX_STUB_ACTIVE="$TMP/first-active" TESTPLEX_STUB_ENABLED="$TMP/first-enabled" \
  bash "$HELPER" --locked set-field backup_sha "$(awk '{print $1}' "$FIRST_BACKUP/server-artifacts.sha256")"
TESTPLEX_SERVER_DIR="$FIRST_SERVER" TESTPLEX_STATE_DIR="$FIRST_STATE" TESTPLEX_UNIT_FILE="$FIRST_UNIT" \
TESTPLEX_STUB_ACTIVE="$TMP/first-active" TESTPLEX_STUB_ENABLED="$TMP/first-enabled" \
  bash "$HELPER" --locked set-field backup_complete true
TESTPLEX_SERVER_DIR="$FIRST_SERVER" TESTPLEX_STATE_DIR="$FIRST_STATE" TESTPLEX_UNIT_FILE="$FIRST_UNIT" \
TESTPLEX_STUB_ACTIVE="$TMP/first-active" TESTPLEX_STUB_ENABLED="$TMP/first-enabled" \
  bash "$HELPER" --locked set-phase REPLACING
output="$(TESTPLEX_SERVER_DIR="$FIRST_SERVER" TESTPLEX_STATE_DIR="$FIRST_STATE" TESTPLEX_UNIT_FILE="$FIRST_UNIT" \
TESTPLEX_STUB_ACTIVE="$TMP/first-active" TESTPLEX_STUB_ENABLED="$TMP/first-enabled" bash "$HELPER" recover "$target_sha")"
grep -q '^no-prior-deployment-restored:' <<< "$output"
[[ ! -e "$FIRST_SERVER" && ! -e "$FIRST_UNIT" ]]
[[ -f "$FIRST_BACKUP/failed-artifacts/attempt-001/no-prior-server-directory/paper.jar" ]]
grep -Fxq 'status=NO_PRIOR_DEPLOYMENT_RESTORED' "$FIRST_STATE/current.properties"
echo 'PASS first-install recovery verifies prior absence and retains failed artifacts'

echo 'All independent recovery tests passed.'
