#!/usr/bin/env bash
set -Eeuo pipefail

# Durable, standalone rollback for a modern Testplex deployment transaction.
SERVER_DIR="${TESTPLEX_SERVER_DIR:-/home/monstermaze/servers/testplex-modern}"
STATE_DIR="${TESTPLEX_STATE_DIR:-/var/lib/testplex-modern-deploy}"
LOCK_FILE="${TESTPLEX_LOCK_FILE:-/var/lock/testplex-modern-deploy.lock}"
UNIT_FILE="${TESTPLEX_UNIT_FILE:-/etc/systemd/system/testplex-modern.service}"
SERVICE="${TESTPLEX_SERVICE:-testplex-modern.service}"
REBORN_SERVICE="${TESTPLEX_REBORN_SERVICE:-testplex-reborn.service}"
SYSTEMCTL="${TESTPLEX_SYSTEMCTL:-systemctl}"
SS="${TESTPLEX_SS:-ss}"
TIMEOUT="${TESTPLEX_TIMEOUT:-timeout}"
PROC_ROOT="${TESTPLEX_PROC_ROOT:-/proc}"
MANIFEST="$STATE_DIR/current.properties"
WORLD_NAMES=(mm_void mm_volcano mm_colombia mm_sandycoast mm_siberian mm_swampland mm_tesorohundido)

fail() { printf 'RECOVERY_FAILED: %s\n' "$*" >&2; exit 1; }
value() { [[ -f "$MANIFEST" ]] || return 1; sed -n "s/^$1=//p" "$MANIFEST" | head -n 1; }
write_manifest() {
  local key="$1" replacement="$2" found=false tmp backup_dir
  [[ "$key" =~ ^[a-z_]+$ ]] || fail "invalid manifest field $key"
  tmp="$(mktemp "$STATE_DIR/.current.XXXXXX")"
  while IFS= read -r line || [[ -n "$line" ]]; do
    if [[ "$line" == "$key="* ]]; then printf '%s=%s\n' "$key" "$replacement" >> "$tmp"; found=true
    else printf '%s\n' "$line" >> "$tmp"; fi
  done < "$MANIFEST"
  [[ "$found" == true ]] || printf '%s=%s\n' "$key" "$replacement" >> "$tmp"
  chmod 0600 "$tmp"
  mv -f -- "$tmp" "$MANIFEST"
  backup_dir="$(value backup_dir || true)"
  if [[ -n "$backup_dir" && -d "$backup_dir" ]]; then
    cp "$MANIFEST" "$backup_dir/transaction.properties.tmp"
    mv -f -- "$backup_dir/transaction.properties.tmp" "$backup_dir/transaction.properties"
  fi
  sync
}
set_status() { write_manifest status "$1"; }

validate_transaction() {
  [[ -f "$MANIFEST" ]] || fail "no durable deployment transaction exists"
  local deployment_id backup_dir target_sha previous_sha replacement_started previous_state previous_active previous_reborn_active previous_enabled
  deployment_id="$(value deployment_id || true)"; backup_dir="$(value backup_dir || true)"
  target_sha="$(value target_sha || true)"; previous_sha="$(value previous_sha || true)"
  replacement_started="$(value replacement_started || true)"
  previous_state="$(value previous_state || true)"
  previous_active="$(value previous_active || true)"
  previous_reborn_active="$(value previous_reborn_active || true)"
  previous_enabled="$(value previous_enabled || true)"
  [[ "$deployment_id" =~ ^[A-Za-z0-9._-]{1,100}$ ]] || fail "invalid deployment identity"
  [[ "$target_sha" =~ ^[0-9a-f]{40}$ ]] || fail "invalid target build SHA"
  [[ "$previous_state" == deployed || "$previous_state" == none ]] || fail "invalid previous deployment state"
  if [[ "$previous_state" == deployed ]]; then
    [[ "$previous_sha" =~ ^[0-9a-f]{40}$ ]] || fail "deployed previous state has no verified build SHA"
  else
    [[ "$previous_sha" == none ]] || fail "absent previous state has an unexpected build SHA"
  fi
  [[ "$previous_active" == true || "$previous_active" == false ]] || fail "invalid previous modern service state"
  [[ "$previous_reborn_active" == true || "$previous_reborn_active" == false ]] || fail "invalid previous Reborn service state"
  case "$previous_enabled" in enabled|disabled|masked|static|indirect|not-found) ;; *) fail "unsupported previous systemd enablement state" ;; esac
  case "$backup_dir" in "$STATE_DIR"/backups/*) ;; *) fail "backup path is outside the durable deployment state directory" ;; esac
  [[ "$replacement_started" == true || "$replacement_started" == false ]] || fail "invalid replacement_started state"
}
assert_backup_complete() {
  local backup_dir backup_sha world declared_worlds previous_marker file prior_files archive_listing
  backup_dir="$(value backup_dir)"; backup_sha="$(value backup_sha || true)"
  [[ "$(value backup_complete || true)" == true ]] || fail "backup is incomplete; refusing to restore or restart"
  [[ "$backup_sha" =~ ^[0-9a-f]{64}$ && -s "$backup_dir/server-artifacts.tar" && -s "$backup_dir/server-artifacts.sha256" ]] ||
    fail "server artifact snapshot identity is incomplete"
  [[ "$(awk '{print $1}' "$backup_dir/server-artifacts.sha256")" == "$backup_sha" ]] || fail "backup manifest SHA differs from transaction identity"
  (cd "$backup_dir" && sha256sum -c server-artifacts.sha256 >/dev/null) || fail "server artifact snapshot checksum failed"

  # Build one complete member index and test that regular file below. Never pipe
  # tar -tf into grep -q: with pipefail, grep closes the pipe on its first match,
  # tar receives SIGPIPE, and a valid archive is falsely reported as incomplete.
  # Reusing this index also avoids rescanning a large world/plugin archive for
  # each individual file and directory assertion.
  archive_listing="$backup_dir/server-artifacts.members"
  tar -tf "$backup_dir/server-artifacts.tar" > "$archive_listing" ||
    fail "server artifact snapshot cannot be read"
  chmod 0600 "$archive_listing"

  prior_files="$(value prior_files || true)"
  if [[ "$(value previous_state)" == deployed ]]; then
    if [[ "$(value had_old_plugins)" == true ]]; then
      grep -Eq '^\.?/?plugins/?$' "$archive_listing" || fail "snapshot omits the previous plugins directory"
    fi
    grep -Eq '^(\./)?\.testplex-modern-deployed-build$' "$archive_listing" ||
      fail "snapshot omits the previous deployed-build identity"
    previous_marker="$(tar -xOf "$backup_dir/server-artifacts.tar" .testplex-modern-deployed-build 2>/dev/null ||
      tar -xOf "$backup_dir/server-artifacts.tar" ./.testplex-modern-deployed-build 2>/dev/null || true)"
    [[ "$previous_marker" == "$(value previous_sha)" ]] ||
      fail "snapshot deployed-build identity differs from the previous deployment identity"
  else
    [[ -z "$prior_files" && "$(value had_old_plugins)" == false && "$(value had_old_paper)" == false &&
       -z "$(value worlds || true)" && "$(value server_dir_existed)" == false ]] ||
      fail "no-prior-deployment journal describes existing deployment artifacts"
    [[ ! -s "$archive_listing" ]] || fail "no-prior-deployment snapshot is not empty"
  fi
  IFS=',' read -r -a files_to_verify <<< "$prior_files"
  for file in "${files_to_verify[@]}"; do
    [[ "$file" =~ ^(paper\.jar|server\.properties|eula\.txt|\.testplex-modern-deployed-build|\.testplex-paper-build|\.testplex-mm-source-maps|\.testplex-plugin-sha256sums|\.testplex-modern-sandbox)$ ]] ||
      fail "transaction names an unsupported server artifact"
    grep -Fxq "$file" "$archive_listing" || grep -Fxq "./$file" "$archive_listing" ||
      fail "snapshot omits previously installed artifact $file"
  done
  declared_worlds="$(value worlds || true)"
  if [[ -n "$declared_worlds" ]]; then
    IFS=',' read -r -a worlds_to_verify <<< "$declared_worlds"
    for world in "${worlds_to_verify[@]}"; do
      [[ "$world" =~ ^(mm_void|mm_volcano|mm_colombia|mm_sandycoast|mm_siberian|mm_swampland|mm_tesorohundido)$ ]] ||
        fail "transaction names an unsupported map world"
      grep -Eq "^\\.?/?${world}/?$" "$archive_listing" ||
        fail "snapshot omits previously installed map world $world"
    done
  fi
  if [[ "$(value had_old_paper || true)" == true ]]; then
    grep -Eq '^\.?/?paper\.jar$' "$archive_listing" || fail "snapshot omits the previous Paper artifact"
  fi
  if [[ "$(value had_old_unit || true)" == true ]]; then
    [[ -s "$backup_dir/unit.tar" && -s "$backup_dir/unit.sha256" ]] || fail "previous service unit backup is incomplete"
    (cd "$backup_dir" && sha256sum -c unit.sha256 >/dev/null) || fail "previous service unit checksum failed"
  fi
}

systemctl_bounded() { "$TIMEOUT" --foreground --signal=TERM 90s "$SYSTEMCTL" "$@"; }
systemctl_query() { "$TIMEOUT" --foreground --signal=TERM 15s "$SYSTEMCTL" "$@"; }
service_active() { systemctl_query is-active --quiet "$SERVICE" >/dev/null 2>&1; }
port_listening() { "$TIMEOUT" --foreground --signal=TERM 15s "$SS" -ltn | awk '$4 ~ /:25568$/ {found=1} END {exit !found}'; }
runtime_record_matches() {
  local sha="$1" invocation="$2" pid="$3" readiness="$SERVER_DIR/plugins/ArcadeMechanicsModern/.testplex-arcade-readiness.properties" nonce
  nonce="$(sed -n 's/^nonce=//p' "$SERVER_DIR/plugins/ArcadeMechanicsModern/.testplex-modern-deploy-identity.properties" 2>/dev/null | head -n 1 || true)"
  [[ -n "$nonce" && -s "$readiness" ]] || return 1
  grep -Fxq 'status=READY' "$readiness" && grep -Fxq 'module=arcade-lobby' "$readiness" &&
    grep -Fxq 'manager=testplex-arcade' "$readiness" &&
    grep -Fxq 'world=testplex_arcade_lobby_v1' "$readiness" &&
    grep -Fxq 'plane=100x100' "$readiness" && grep -Fxq 'floor_y=100' "$readiness" &&
    grep -Fxq "source_sha=$sha" "$readiness" &&
    grep -Fxq "nonce=$nonce" "$readiness" && grep -Fxq "invocation_id=$invocation" "$readiness" &&
    grep -Fxq "process_id=$pid" "$readiness"
}
runtime_identity_matches() {
  local sha="$1" invocation="$2" pid="$3" identity="$SERVER_DIR/plugins/ArcadeMechanicsModern/.testplex-modern-deploy-identity.properties"
  [[ -e "$identity" ]] || return 0
  runtime_record_matches "$sha" "$invocation" "$pid"
}
running_process_matches_service() {
  local pid="$1" cwd command_line working_directory exec_start
  [[ -r "$PROC_ROOT/$pid/cmdline" ]] || return 1
  command_line="$(tr '\0' ' ' < "$PROC_ROOT/$pid/cmdline")"
  working_directory="$(systemctl_query show --property=WorkingDirectory --value "$SERVICE" 2>/dev/null || true)"
  exec_start="$(systemctl_query show --property=ExecStart --value "$SERVICE" 2>/dev/null || true)"
  cwd="$(readlink -f "$SERVER_DIR" 2>/dev/null || true)"
  [[ -n "$cwd" && "$working_directory" == "$cwd" && "$exec_start" == *paper.jar* &&
     "$command_line" == *-jar*paper.jar* ]]
}
verify_running_runtime() {
  local expected_sha="$1" baseline_log_identity="$2" invocation pid active_enter log_identity runtime_sha attempt
  for attempt in $(seq 1 "${TESTPLEX_VERIFY_ATTEMPTS:-60}"); do
    if service_active && port_listening && [[ -s "$SERVER_DIR/logs/latest.log" ]] && grep -Fq 'Done (' "$SERVER_DIR/logs/latest.log"; then
      log_identity="$(stat -c '%i:%s:%y' "$SERVER_DIR/logs/latest.log" 2>/dev/null || printf unavailable)"
      invocation="$(systemctl_query show --property=InvocationID --value "$SERVICE" 2>/dev/null || true)"
      pid="$(systemctl_query show --property=MainPID --value "$SERVICE" 2>/dev/null || true)"
      active_enter="$(systemctl_query show --property=ActiveEnterTimestampMonotonic --value "$SERVICE" 2>/dev/null || true)"
      if [[ "$log_identity" != unavailable && ( "$baseline_log_identity" == ACTIVE || "$log_identity" != "$baseline_log_identity" ) ]] &&
         [[ "$invocation" =~ ^[0-9a-fA-F]{32}$ && "$pid" =~ ^[1-9][0-9]*$ && -n "$active_enter" ]] &&
         [[ "$(cat "$SERVER_DIR/.testplex-modern-deployed-build" 2>/dev/null || true)" == "$expected_sha" ]] &&
         running_process_matches_service "$pid" && runtime_identity_matches "$expected_sha" "$invocation" "$pid"; then
        runtime_sha=UNVERIFIED
        [[ -e "$SERVER_DIR/plugins/ArcadeMechanicsModern/.testplex-modern-deploy-identity.properties" ]] && runtime_sha="$expected_sha"
        printf '%s\n%s\n%s\n' "$runtime_sha" "$invocation" "$pid"; return 0
      fi
    fi
    sleep "${TESTPLEX_VERIFY_SLEEP:-2}"
  done
  return 1
}
preserve_path() {
  local source="$1" destination="$2"
  if [[ -e "$source" || -L "$source" ]]; then mkdir -p "$(dirname "$destination")"; mv -- "$source" "$destination"; fi
}
restore_systemd_enablement() {
  local expected="$(value previous_enabled)" actual
  case "$expected" in
    enabled) systemctl_bounded enable "$SERVICE" || fail "could not restore enabled state" ;;
    disabled) systemctl_bounded disable "$SERVICE" >/dev/null 2>&1 || fail "could not restore disabled state" ;;
    masked) systemctl_bounded mask "$SERVICE" >/dev/null 2>&1 || fail "could not restore masked state" ;;
    not-found)
      systemctl_bounded disable "$SERVICE" >/dev/null 2>&1 || true
      [[ ! -e "$UNIT_FILE" && ! -L "$UNIT_FILE" ]] || fail "unit exists although its prior state was not-found"
      ;;
    static|indirect) : ;; # These states are determined by the restored unit; never enable them blindly.
    *) fail "unsupported previous systemd enablement state $expected" ;;
  esac
  actual="$(systemctl_query is-enabled "$SERVICE" 2>/dev/null || true)"
  [[ "$actual" == "$expected" ]] || fail "systemd enablement is $actual after restoration; expected $expected"
}
restore_service_active_state() {
  local service="$1" expected="$2" label="$3"
  if [[ "$expected" == true ]]; then
    systemctl_bounded start "$service" || fail "could not restore previously active $label service"
    systemctl_query is-active --quiet "$service" || fail "$label service is not active after restoration"
  else
    if systemctl_query is-active --quiet "$service"; then
      systemctl_bounded stop "$service" || fail "could not restore previously inactive $label service"
    fi
    ! systemctl_query is-active --quiet "$service" || fail "$label service remained active after restoration"
  fi
}
next_attempt_dir() {
  local backup_dir count
  backup_dir="$(value backup_dir)"; count="$(cat "$backup_dir/recovery-attempts" 2>/dev/null || printf 0)"
  [[ "$count" =~ ^[0-9]+$ ]] || count=0
  count=$((count + 1)); printf '%s\n' "$count" > "$backup_dir/recovery-attempts.tmp"
  mv -f -- "$backup_dir/recovery-attempts.tmp" "$backup_dir/recovery-attempts"
  printf '%s/failed-artifacts/attempt-%03d\n' "$backup_dir" "$count"
}
restore_artifacts() {
  local backup_dir deployment_id attempt_dir stage world file had_unit
  backup_dir="$(value backup_dir)"; deployment_id="$(value deployment_id)"
  attempt_dir="$(next_attempt_dir)"; stage="$SERVER_DIR/.testplex-restore-$deployment_id"
  mkdir -p "$attempt_dir"
  set_status RESTORING
  systemctl_bounded stop "$SERVICE" || fail "could not stop modern service before artifact restoration"
  if [[ "$(value previous_state)" == none ]]; then
    preserve_path "$SERVER_DIR" "$attempt_dir/no-prior-server-directory"
  else
    if [[ -e "$stage" ]]; then preserve_path "$stage" "$attempt_dir/incomplete-restore-stage"; fi
    mkdir -p "$stage"
    tar -xf "$backup_dir/server-artifacts.tar" -C "$stage" || fail "could not extract the verified artifact snapshot"
    if [[ "$(value had_old_plugins)" == true ]]; then
      [[ -d "$stage/plugins" ]] || fail "snapshot did not extract the previous plugins directory"
    fi
    if [[ "$(value had_old_paper)" == true ]]; then [[ -s "$stage/paper.jar" ]] || fail "previous Paper artifact is missing"; fi
    preserve_path "$SERVER_DIR/plugins" "$attempt_dir/plugins-failed-deployment"
    if [[ "$(value had_old_plugins)" == true ]]; then mv -- "$stage/plugins" "$SERVER_DIR/plugins"; fi
    for file in paper.jar server.properties eula.txt .testplex-modern-deployed-build .testplex-paper-build \
      .testplex-mm-source-maps .testplex-plugin-sha256sums .testplex-modern-sandbox; do
      preserve_path "$SERVER_DIR/$file" "$attempt_dir/current-$file"
      if [[ -e "$stage/$file" || -L "$stage/$file" ]]; then mv -- "$stage/$file" "$SERVER_DIR/$file"; fi
    done
    for world in "${WORLD_NAMES[@]}"; do
      preserve_path "$SERVER_DIR/$world" "$attempt_dir/maps/$world-failed-deployment"
      if [[ -d "$stage/$world" ]]; then mv -- "$stage/$world" "$SERVER_DIR/$world"; fi
    done
  fi
  had_unit="$(value had_old_unit || true)"
  if [[ "$had_unit" == true ]]; then
    local unit_stage="$stage/previous-unit"
    mkdir -p "$unit_stage"
    tar -xf "$backup_dir/unit.tar" -C "$unit_stage" || fail "could not extract the previous systemd unit"
    [[ -e "$unit_stage/testplex-modern.service" || -L "$unit_stage/testplex-modern.service" ]] || fail "previous systemd unit is missing from its backup"
    preserve_path "$UNIT_FILE" "$attempt_dir/testplex-modern-failed-deployment.service"
    mv -f -- "$unit_stage/testplex-modern.service" "$UNIT_FILE"
  else
    preserve_path "$UNIT_FILE" "$attempt_dir/testplex-modern-failed-deployment.service"
  fi
  systemctl_bounded daemon-reload || fail "systemd could not reload the restored unit"
  restore_systemd_enablement
  sync
  if [[ "$(value previous_state)" == none ]]; then
    write_manifest restored_artifact_sha none
    set_status RESTORED
    printf 'Artifacts restored to their prior absent state; failed deployment retained at %s.\n' "$attempt_dir"
    return 0
  fi
  write_manifest restored_artifact_sha "$(value previous_sha)"
  set_status RESTORED
  printf 'Artifacts restored from backup %s; restored artifact SHA=%s. Failed deployment files retained at %s.\n' \
    "$backup_dir" "$(value previous_sha)" "$attempt_dir"
}
verify_restored_runtime() {
  local previous_sha baseline_log_identity runtime_info runtime_sha invocation pid prior_active
  previous_sha="$(value previous_sha)"
  prior_active="$(value previous_active)"
  if [[ "$(value previous_state)" == none ]]; then
    ! service_active || fail "modern service is active after restoring the prior absent state"
    [[ ! -e "$SERVER_DIR" && ! -L "$SERVER_DIR" ]] || fail "server directory remains after restoring prior absence"
    [[ ! -e "$UNIT_FILE" && ! -L "$UNIT_FILE" ]] || fail "modern service unit remains after restoring prior absence"
    [[ "$(systemctl_query is-enabled "$SERVICE" 2>/dev/null || true)" == not-found ]] ||
      fail "modern service is enabled or masked after restoring prior absence"
    ! port_listening || fail "port 25568 is listening after restoring prior absence"
    restore_service_active_state "$REBORN_SERVICE" "$(value previous_reborn_active)" Reborn
    write_manifest restored_artifact_sha none
    write_manifest runtime_sha UNVERIFIED
    write_manifest runtime_invocation_id none
    write_manifest runtime_pid none
    set_status NO_PRIOR_DEPLOYMENT_RESTORED
    printf 'no-prior-deployment-restored: restored artifact SHA=none; runtime SHA=UNVERIFIED; modern service absent and inactive.\n'
    return 0
  fi
  [[ "$previous_sha" =~ ^[0-9a-f]{40}$ ]] || fail "previous artifact SHA is not verifiable"
  [[ "$(cat "$SERVER_DIR/.testplex-modern-deployed-build" 2>/dev/null || true)" == "$previous_sha" ]] || fail "restored artifact marker differs from backup identity"
  if [[ "$(value had_old_unit)" == true || "$(systemctl_query show --property=LoadState --value "$SERVICE" 2>/dev/null || true)" != not-found ]]; then
    :
  else
    fail "restored modern service unit is unavailable"
  fi
  write_manifest runtime_sha unknown
  write_manifest runtime_invocation_id unknown
  write_manifest runtime_pid unknown
  if [[ "$prior_active" == false ]]; then
    service_active && systemctl_bounded stop "$SERVICE"
    service_active && fail "modern service should be inactive after restoration"
    ! port_listening || fail "port 25568 is listening although the modern service was previously inactive"
    write_manifest restored_artifact_sha "$previous_sha"
    write_manifest runtime_sha UNVERIFIED
    write_manifest runtime_invocation_id none
    write_manifest runtime_pid none
    restore_systemd_enablement
    restore_service_active_state "$REBORN_SERVICE" "$(value previous_reborn_active)" Reborn
    set_status RECOVERY_VERIFIED
    printf 'Rollback verified: restored artifact SHA=%s; runtime SHA=UNVERIFIED (service was previously inactive); process identity=NOT_RUNNING.\n' "$previous_sha"
    return 0
  fi
  if ! service_active; then
    baseline_log_identity="$(stat -c '%i:%s:%y' "$SERVER_DIR/logs/latest.log" 2>/dev/null || printf missing)"
    systemctl_bounded start "$SERVICE" || fail "systemd could not start restored modern service"
  else baseline_log_identity=ACTIVE; fi
  if ! runtime_info="$(verify_running_runtime "$previous_sha" "$baseline_log_identity")"; then
    write_manifest runtime_sha unknown; set_status RESTORED
    fail "systemd, Paper readiness, port, or restored process identity could not be verified"
  fi
  runtime_sha="$(printf '%s\n' "$runtime_info" | sed -n '1p')"
  invocation="$(printf '%s\n' "$runtime_info" | sed -n '2p')"; pid="$(printf '%s\n' "$runtime_info" | sed -n '3p')"
  if [[ "$runtime_sha" != UNVERIFIED && "$runtime_sha" != "$previous_sha" ]]; then
    fail "running runtime SHA differs from restored artifact SHA"
  fi
  write_manifest runtime_sha "$runtime_sha"; write_manifest runtime_invocation_id "$invocation"
  write_manifest runtime_pid "$pid"; write_manifest runtime_verified_at "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  restore_systemd_enablement
  restore_service_active_state "$REBORN_SERVICE" "$(value previous_reborn_active)" Reborn
  set_status RECOVERY_VERIFIED
  printf 'Rollback verified: restored artifact SHA=%s; runtime SHA=%s; process identity=VERIFIED InvocationID=%s MainPID=%s; prior_active=%s.\n' \
    "$previous_sha" "$runtime_sha" "$invocation" "$pid" "$prior_active"
}
verify_already_recovered() {
  local previous_sha runtime_sha invocation pid prior_active
  if [[ "$(value status)" == NO_PRIOR_DEPLOYMENT_RESTORED ]]; then
    verify_restored_runtime
    return 0
  fi
  previous_sha="$(value restored_artifact_sha || value previous_sha || true)"; runtime_sha="$(value runtime_sha || true)"
  invocation="$(value runtime_invocation_id || true)"; pid="$(value runtime_pid || true)"; prior_active="$(value previous_active || true)"
  [[ "$previous_sha" =~ ^[0-9a-f]{40}$ && ( "$runtime_sha" == "$previous_sha" || "$runtime_sha" == UNVERIFIED ) ]] ||
    fail "recovery status lacks a verified restored runtime identity"
  [[ "$(cat "$SERVER_DIR/.testplex-modern-deployed-build" 2>/dev/null || true)" == "$previous_sha" ]] || fail "live artifact marker changed after recovery"
  if [[ "$prior_active" == true ]]; then
    [[ "$invocation" =~ ^[0-9a-fA-F]{32}$ && "$pid" =~ ^[1-9][0-9]*$ ]] || fail "active recovery lacks a verified process identity"
    service_active && port_listening && grep -Fq 'Done (' "$SERVER_DIR/logs/latest.log" || fail "service, Paper, or port is unavailable after recovery"
    local current_pid="$(systemctl_query show --property=MainPID --value "$SERVICE" 2>/dev/null || true)"
    local current_invocation="$(systemctl_query show --property=InvocationID --value "$SERVICE" 2>/dev/null || true)"
    [[ "$current_pid" == "$pid" && "$current_invocation" == "$invocation" ]] || fail "runtime process identity changed after recovery"
    running_process_matches_service "$current_pid" || fail "Paper process no longer matches the restored service"
    if [[ "$runtime_sha" != UNVERIFIED ]]; then runtime_record_matches "$runtime_sha" "$current_invocation" "$current_pid" || fail "runtime SHA attestation no longer matches"; fi
  else
    [[ "$runtime_sha" == UNVERIFIED && "$invocation" == none && "$pid" == none ]] || fail "inactive prior service has inconsistent runtime fields"
    ! service_active && ! port_listening || fail "service or port is active after restoring prior inactive state"
  fi
  [[ "$(systemctl_query is-enabled "$SERVICE" 2>/dev/null || true)" == "$(value previous_enabled)" ]] || fail "systemd enablement changed after recovery"
  printf 'Rollback already verified: restored artifact SHA=%s; verified runtime SHA=%s; prior_active=%s.\n' "$previous_sha" "$runtime_sha" "$prior_active"
}

run_locked() {
  local command status replacement_started
  command="$1"
  case "$command" in
    assert-deployable)
      [[ -f "$MANIFEST" ]] || { echo "No unresolved modern deployment transaction."; return 0; }
      status="$(value status || true)"
      case "$status" in DEPLOYMENT_VERIFIED|RECOVERY_VERIFIED|NO_PRIOR_DEPLOYMENT_RESTORED) echo "Previous transaction $status." ;; *) fail "unresolved transaction ($status); independent recovery is required" ;; esac
      ;;
    begin)
      local deployment_id="$2" target_sha="$3" backup_dir="$4" previous_sha="$5" previous_state="$6"
      local previous_active="$7" previous_enabled="$8" previous_reborn_active="$9"
      local server_dir_existed="${10}" had_old_unit="${11}" had_old_paper="${12}" had_old_plugins="${13}"
      local prior_files="${14}" worlds="${15}"
      [[ "$deployment_id" =~ ^[A-Za-z0-9._-]{1,100}$ && "$target_sha" =~ ^[0-9a-f]{40}$ ]] || fail "invalid deployment identity or target SHA"
      [[ "$previous_state" == none && "$previous_sha" == none && "$server_dir_existed" == false && "$had_old_unit" == false &&
         "$had_old_paper" == false && "$had_old_plugins" == false && "$previous_active" == false &&
         "$previous_enabled" == not-found && -z "$prior_files" && -z "$worlds" ]] ||
        [[ "$previous_state" == deployed && "$previous_sha" =~ ^[0-9a-f]{40}$ && "$server_dir_existed" == true ]] ||
        fail "previous state is not a verified deployed baseline or verified absence"
      [[ "$previous_active" == true || "$previous_active" == false ]] || fail "invalid previous modern service state"
      [[ "$previous_reborn_active" == true || "$previous_reborn_active" == false ]] || fail "invalid previous Reborn service state"
      case "$previous_enabled" in enabled|disabled|masked|static|indirect|not-found) ;; *) fail "unsupported previous enablement state" ;; esac
      case "$backup_dir" in "$STATE_DIR"/backups/*) ;; *) fail "invalid backup path" ;; esac
      if [[ -f "$MANIFEST" ]]; then
        local existing_status="$(value status || true)"
        case "$existing_status" in DEPLOYMENT_VERIFIED|RECOVERY_VERIFIED|NO_PRIOR_DEPLOYMENT_RESTORED) ;; *) fail "unresolved transaction blocks journal creation" ;; esac
      fi
      mkdir -p "$STATE_DIR" "$backup_dir"
      cat > "$STATE_DIR/current.properties.tmp" <<EOF
deployment_id=$deployment_id
target_sha=$target_sha
backup_dir=$backup_dir
previous_sha=$previous_sha
previous_state=$previous_state
previous_active=$previous_active
previous_enabled=$previous_enabled
previous_reborn_active=$previous_reborn_active
server_dir_existed=$server_dir_existed
had_old_unit=$had_old_unit
had_old_paper=$had_old_paper
had_old_plugins=$had_old_plugins
prior_files=$prior_files
worlds=$worlds
backup_complete=false
replacement_started=false
status=PREPARING
EOF
      chmod 0600 "$STATE_DIR/current.properties.tmp"; mv -f -- "$STATE_DIR/current.properties.tmp" "$MANIFEST"
      cp "$MANIFEST" "$backup_dir/transaction.properties"
      sync
      ;;
    set-field) validate_transaction; write_manifest "$2" "$3" ;;
    set-phase)
      validate_transaction
      case "$2" in PREPARED|REPLACING|DEPLOYMENT_VERIFIED|RESTORE_FAILED) ;; *) fail "unsupported transaction phase $2" ;; esac
      if [[ "$2" == REPLACING ]]; then write_manifest replacement_started true; fi
      set_status "$2"
      ;;
    verify-snapshot)
      validate_transaction
      assert_backup_complete
      printf 'Snapshot verified: artifact SHA=%s; transaction=%s.\\n' "$(value backup_sha)" "$(value deployment_id)"
      ;;
    recover)
      local expected_target="${2:-}"
      if [[ ! -f "$MANIFEST" ]]; then echo "No durable transaction is present; no rollback was claimed."; return 0; fi
      if [[ -n "$expected_target" && "$(value target_sha || true)" != "$expected_target" ]]; then
        status="$(value status || true)"
      case "$status" in DEPLOYMENT_VERIFIED|RECOVERY_VERIFIED|NO_PRIOR_DEPLOYMENT_RESTORED)
          echo "No unresolved transaction matches target $expected_target; current transaction is terminal ($status)."
          return 0
          ;;
          *) fail "another unresolved deployment transaction blocks recovery" ;;
        esac
      fi
      validate_transaction; status="$(value status || true)"
      case "$status" in
        RECOVERY_VERIFIED) verify_already_recovered; return 0 ;;
        NO_PRIOR_DEPLOYMENT_RESTORED) verify_already_recovered; return 0 ;;
        DEPLOYMENT_VERIFIED)
          printf 'Deployment already verified: target artifact SHA=%s; refusing rollback of a successful deployment.\n' "$(value target_sha)"
          return 0
          ;;
      esac
      replacement_started="$(value replacement_started || true)"
      if [[ "$replacement_started" != true ]]; then
        if [[ "$(value previous_state)" == deployed ]]; then
          [[ "$(cat "$SERVER_DIR/.testplex-modern-deployed-build" 2>/dev/null || true)" == "$(value previous_sha)" ]] ||
            fail "pre-replacement artifacts differ from the verified previous deployment"
          write_manifest restored_artifact_sha "$(value previous_sha)"
        fi
        set_status RESTORED
        verify_restored_runtime
        return 0
      fi
      assert_backup_complete
      restore_artifacts
      verify_restored_runtime
      ;;
    *) fail "unsupported command $command" ;;
  esac
}
main() {
  local locked=false
  if [[ "${1:-}" == --locked ]]; then locked=true; shift; fi
  [[ $# -gt 0 ]] || fail "command is required"
  mkdir -p "$STATE_DIR" "$(dirname "$LOCK_FILE")"
  if [[ "$locked" != true ]]; then
    if [[ "$1" == recover ]]; then
      local wait_seconds="${TESTPLEX_LOCK_WAIT_SECONDS:-30}"
      [[ "$wait_seconds" =~ ^[0-9]+$ ]] || fail "invalid recovery lock wait"
      exec flock -w "$wait_seconds" "$LOCK_FILE" env TESTPLEX_RECOVERY_LOCK_HELD=1 "$0" --locked "$@"
    fi
    exec flock -n "$LOCK_FILE" env TESTPLEX_RECOVERY_LOCK_HELD=1 "$0" --locked "$@"
  fi
  run_locked "$@"
}
main "$@"
