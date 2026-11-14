#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo 'Usage: testplex-feedback-clone-repo <repository> <branch> <workspace>' >&2
  exit 64
fi

repository="$1"
branch="$2"
workspace="$3"
[[ "$repository" == "joshbet9/testplex" ]] || { echo 'repository is not allowed' >&2; exit 65; }
[[ "$branch" == "stable" ]] || { echo 'branch is not allowed' >&2; exit 65; }

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
[[ ! -e "$workspace" && ! -L "$workspace" ]] || {
  echo 'workspace already exists' >&2
  exit 66
}

exec /usr/bin/git clone --depth 1 --branch "$branch" \
  "https://github.com/$repository.git" "$workspace"
