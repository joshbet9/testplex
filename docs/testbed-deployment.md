# Isolated Mineplex Reborn testbed deployment

The build workflow publishes a versioned set of compiled plugin JARs and, when explicitly enabled, deploys them to a **dedicated Mineplex Reborn test server on your Hyper-V host** using the same Tailscale + SSH route as the existing Monster Maze deployment. It never deploys to the production server, and a deploy is rejected unless the destination contains a test-only marker.

## What the workflow does

1. Builds the Maven reactor using Java 8.
2. Checks that `Plugins/out/Arcade.jar` exists and runs source smoke checks for Snake's magma hazards/roulette and Monster Maze's safe-pad bounds.
3. Uploads every JAR produced in `Plugins/out/` as `testplex-plugin-set-<commit-sha>` for 14 days.
4. Optionally, the workflow connects to Hyper-V through Tailscale and SSH, copies the compiled plugin set to a temporary location, backs up JARs it will replace, stops only the dedicated Reborn test-server systemd service, installs the files, starts the service, and verifies that Arcade enabled successfully. If startup verification fails, it attempts to restore the previous JARs and restart the service.

To add the standalone-themed maps later, package each converted Mineplex-compatible map as a `.zip` under the respective `../../update/maps/MonsterMazeSpeed/` or `../../update/maps/MonsterMazeModern/` folder, including the native `ORANGE` center marker, any supported `E=` mob type tag, and `B1`/`B2`/`B3` palette tags. Existing original Monster Maze map zips remain the fallback until those new folders have entries. The current integration uses the source-compatible Reborn map pool; it does not automatically convert standalone world folders into Mineplex map metadata.

The artifact is **not a complete server distribution**. A test instance still needs its own server/runtime files, maps, configuration, Java 8, Redis/MySQL and a compatible API service.

## One-time Hyper-V test-server setup

The deploy job deliberately reuses the established `MonsterMaze` deployment route: Ubuntu-hosted GitHub Actions runner, Tailscale, SSH with a pinned known-hosts file, `sudo systemctl`, timestamped backups and rollback. It **must not target** the standalone Spigot/Paper servers at `/home/monstermaze/servers/1.8` or `/home/monstermaze/servers/1.21`; those cannot load the native Mineplex Reborn `Arcade.jar`.

The target must be an already-installed, working **Mineplex Reborn** test instance on the Hyper-V host. This workflow deploys plugin JARs only: it does not provision the server runtime, maps, configuration, Redis/MySQL, or the compatible API service.

1. Prepare a separate test-server root, recommended path `/home/monstermaze/servers/testplex-reborn`. It must have the normal Reborn server layout, including `plugins/` and `logs/latest.log`, and be managed by its own running systemd unit, recommended name `testplex-reborn.service`. Verify by hand that this unit starts and stops only that test instance.
2. In the test-server root, create `.testplex-sandbox` containing exactly:

   ```text
   TESTPLEX ISOLATED TEST SERVER - NOT PRODUCTION
   ```

   The deployment rejects the request if the marker is missing or differs.
3. Add/configure the service so it starts the Reborn server from that root, runs under the intended server account, and writes logs to `logs/latest.log`. Install the fixed deployment script as a root-owned executable at `/usr/local/sbin/deploy-testplex-hyperv.sh` (review `.github/scripts/deploy-testplex-hyperv.sh` first). Grant the SSH account permission to run only that fixed script through passwordless sudo; do not grant passwordless sudo to arbitrary `bash`. For the `monstermaze` SSH account, a sudoers drop-in can contain `monstermaze ALL=(root) NOPASSWD: /usr/local/sbin/deploy-testplex-hyperv.sh`. Set the drop-in to mode `0440` and validate it with `visudo -cf /etc/sudoers.d/testplex-deploy`. The script itself validates the server-root marker and limits accepted paths/service names.
4. In **the `testplex` repository's own Settings → Environments → `hyperv`**, configure these secrets. GitHub secrets are scoped to a repository/environment, so the values from `MonsterMaze` are not automatically inherited by `testplex`:

   | Secret | Purpose |
   | --- | --- |
   | `TS_OAUTH_CLIENT_ID` | Tailscale GitHub Actions connection |
   | `TS_OAUTH_SECRET` | Tailscale GitHub Actions connection |
   | `HYPERV_SSH_HOST` | Hyper-V host reachable over Tailscale |
   | `HYPERV_SSH_USER` | SSH deployment account |
   | `HYPERV_SSH_PORT` | SSH port; optional, defaults to `22` |
   | `HYPERV_SSH_PRIVATE_KEY` | Private key matching the server's authorised key |
   | `HYPERV_SSH_KNOWN_HOSTS` | Pinned SSH host-key entries |

5. In the same `hyperv` environment's variables, set:

   | Variable | Recommended value |
   | --- | --- |
   | `TESTPLEX_SERVER_DIR` | `/home/monstermaze/servers/testplex-reborn` |
   | `TESTPLEX_SYSTEMD_SERVICE` | `testplex-reborn.service` |

   The script additionally restricts the path to the `/home/monstermaze/servers/testplex[-name]` namespace, requires the exact marker, and accepts only `testplex-*.service` unit names. It explicitly refuses the existing Monster Maze standalone services.

6. To deploy the already-verified build without spending Actions minutes on another Maven build, open **Actions → Deploy Testplex Reborn to Hyper-V → Run workflow**, select `feat/testbed-bootstrap` in the branch dropdown, set `deploy_to_test_server` to `true`, and leave `source_run_id` at its default `40301889733`. The manual-only entrypoint exists on the default branch so the dispatch button is visible, but its job is guarded to run only when you explicitly select `feat/testbed-bootstrap` and opt in. It verifies the selected source run succeeded on this branch, locates its unexpired `testplex-plugin-set-*` artifact, and installs that exact artifact. The artifact retention is 14 days from upload. To deploy a different successful build, enter that run's ID in `source_run_id`.
7. Automatic deployment is off by default. Only after a manual deployment succeeds and runtime testing is acceptable, set the **repository variable** `TESTPLEX_AUTO_DEPLOY` to the literal `true` to deploy future trusted pushes to `feat/testbed-bootstrap`. Pull-request runs never deploy.

## Safety and troubleshooting

- The workflow refuses to deploy if the sandbox marker, `plugins/` directory, running systemd service, or `Arcade.jar` artifact is missing.
- Only plugin JARs present in the selected build artifact are replaced. Previous versions are backed up under `.testplex-backups/<timestamp>-<sha>/` within the test root.
- After restart, the job looks for the `Enabling Arcade` startup log marker. If the service doesn't become ready, it attempts to restore the previous JARs and restart the server, then emits recent unit logs.
- Keep a Hyper-V VM snapshot and a known-good backup until actual in-game acceptance testing is complete. Rollback is best-effort; it cannot recover a failed host, broken systemd unit, missing server dependencies, or unavailable database/API services.
- Self-hosted runners and the Tailscale/SSH route execute code and operate on a server. Only trusted maintainers should be allowed to dispatch a deployment or enable auto-deployment.

## Optional Monster Maze safe-pad settings

In the test server's existing Arcade plugin config, merge this YAML into the root configuration (don't replace unrelated settings). Both enhancement switches are off by default:

```yaml
monstermaze:
  safe-pad-enhancements:
    enabled: false
    surface-colour-enabled: false
    surface-colour-data: 5
```

- Set `enabled: true` to run one objective-beacon-only check about 2–3 seconds after the initial pad is built during recruit, then re-check the active and preview objective beacons once per second while live. The code repairs the central beacon only; it does not rebuild surrounding clay or the pad base.
- Set `surface-colour-enabled: true` to select the starting stained-clay surface colour. `surface-colour-data` accepts Minecraft 1.8 dye data values 0–15; default 5 keeps the existing green surface.
- These options are separate from diagnostics. They do not enable or change diagnostic logging. Restart the test server after editing config to ensure the running Arcade plugin loads the new values.
- Use the optional behaviour only on the isolated test server and verify old-pad decay and objective transitions using the in-game test plan.

## Local validation without GitHub Actions

On a Windows development machine with **JDK 8** and Maven on `PATH`, run this from the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .github/scripts/validate-monster-maze.ps1
```

The script builds the full Maven reactor, verifies that `Plugins/out/Arcade.jar` contains the native Speed, Modern and PB classes, and checks the key port contracts. For a source-only pass without Maven, use `-SkipBuild`; source checks do not replace the Java build. No server is stopped, deployed to, or restarted by this local script.

## Native Monster Maze test variants

The new games are part of the Reborn Arcade plugin, not a standalone JAR. For manual tests, use `/game set MonsterMaze`, `/game set MonsterMazeSpeed`, or `/game set MonsterMazeModern`, then `/game start` when the game is not already running. On a Reborn test server, `/game set` updates the active test game list. For a persistent rotating server group, add `MonsterMazeSpeed` and `MonsterMazeModern` to that group's configured games list while retaining `MonsterMaze`. Variants use their own map folder when it contains maps and otherwise fall back to the original Reborn Monster Maze map pool. Kit/stat infrastructure is native in both cases. Do not install the standalone `MonsterMazeStandalone.jar`.

To reproduce a layout, merge this into the Arcade plugin's config without replacing other settings:

```yaml
monstermaze:
  pattern: 0 # 0=random; 1, 2, or 3=force that original maze pattern
  safe-pad-enhancements:
    enabled: false
    surface-colour-enabled: false
    surface-colour-data: 5
```

The personal-best port writes per-player records through Mineplex account custom data, separated by game display/mode, pattern and kit. The isolated test server therefore needs a functioning Mineplex account/custom-data database. No Monster Maze competitive backend/API credentials are required for this PB feature.
