#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo 'Usage: testplex-feedback-cleanup-workspace <workspace>' >&2
  exit 64
fi

workspace="$1"
workspace_root="$(realpath -e -- /var/lib/testplex-feedback-shared/workspaces)"
workspace_parent="$(realpath -e -- "$(dirname -- "$workspace")")"
[[ "$workspace_parent" == "$workspace_root" ]] || {
  echo 'workspace is outside the feedback work root' >&2
  exit 65
}
[[ "$(basename -- "$workspace")" =~ ^[0-9]+-[a-f0-9]{32}$ ]] || {
  echo 'workspace name is invalid' >&2
  exit 65
}
[[ ! -L "$workspace" ]] || {
  echo 'workspace must not be a symbolic link' >&2
  exit 65
}

if [[ -e "$workspace" ]]; then
  exec /usr/bin/rm -rf -- "$workspace"
fi
