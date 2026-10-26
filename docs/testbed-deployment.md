# Isolated Mineplex Reborn testbed deployment

The build workflow can publish a versioned set of compiled plugin JARs and, when explicitly enabled, deploy them to a **dedicated Windows test server**. It never deploys to the production server, and a deploy is rejected unless the destination contains a test-only marker.

## What the workflow does

1. Builds the Maven reactor using Java 8.
2. Checks that `Plugins/out/Arcade.jar` exists and runs source smoke checks for Snake's magma hazards/roulette and Monster Maze's safe-pad bounds.
3. Uploads every JAR produced in `Plugins/out/` as `testplex-plugin-set-<commit-sha>` for 14 days.
4. Optionally, a Windows self-hosted runner copies those JARs into the test server's `plugins` directory, backs up any JARs it will replace, stops the matching test server process, installs the files, starts the server, and checks that the process returns. If startup verification fails, it attempts to restore the previous JARs and start the server again.

To add the standalone-themed maps later, package each converted Mineplex-compatible map as a `.zip` under the respective `../../update/maps/MonsterMazeSpeed/` or `../../update/maps/MonsterMazeModern/` folder, including the native `ORANGE` center marker, any supported `E=` mob type tag, and `B1`/`B2`/`B3` palette tags. Existing original Monster Maze map zips remain the fallback until those new folders have entries. The current integration uses the source-compatible Reborn map pool; it does not automatically convert standalone world folders into Mineplex map metadata.

The artifact is **not a complete server distribution**. A test instance still needs its own server/runtime files, maps, configuration, Java 8, Redis/MySQL and a compatible API service.

## One-time Windows test-server setup

Use a separate Hyper-V VM or other isolated Windows host. Do not point this at a production server.

1. Prepare a working Reborn server instance and verify it can be started and stopped safely by hand. Ensure the expected plugin folder is named `plugins`.
2. In the root server folder, create a file named `.testplex-sandbox` containing exactly this line:

   ```text
   TESTPLEX ISOLATED TEST SERVER - NOT PRODUCTION
   ```

3. Create two **server-specific PowerShell scripts** on that isolated host:
   - A stop script that requests a graceful shutdown of this test server only and exits with an error if shutdown fails.
   - A start script that launches this test server only and exits with an error if startup could not be initiated.

   The deployment workflow does not guess the server's launcher, RCON arrangement or Windows service name. Point the variables below at the scripts you've verified. The deploy script also waits for the Java process matching `TESTPLEX_PROCESS_MATCH` to disappear and return, respectively.
4. Register a GitHub Actions **self-hosted Windows x64 runner** on the test host (or on a dedicated Windows deployment host that can access the test server files). Give it the custom label `testplex`. Run it under an account with access to the test-server folder and the stop/start mechanism.
5. In the repository's **Settings → Secrets and variables → Actions → Variables**, configure these repository variables:

   | Variable | Value |
   | --- | --- |
   | `TESTPLEX_SERVER_DIR` | Full path to the isolated server root |
   | `TESTPLEX_STOP_SCRIPT` | Full path to the verified stop PowerShell script |
   | `TESTPLEX_START_SCRIPT` | Full path to the verified start PowerShell script |
   | `TESTPLEX_PROCESS_MATCH` | A unique, case-insensitive substring in that server's Java command line, at least 8 characters long |

   The deploy workflow uses the runner labels `self-hosted`, `windows`, `x64`, and `testplex`. Do not put credentials in the repository or in these path variables.
6. Initially, leave automatic deployment disabled. Open **Actions → Testplex Java 8 build → Run workflow**, select branch `feat/testbed-bootstrap`, and set `deploy_to_test_server` to true to exercise the guarded deployment. The runner must already be online and the variables above must be set.
7. After a successful isolated test, set the repository variable `TESTPLEX_AUTO_DEPLOY` to the literal value `true` to deploy automatically when commits are pushed to `feat/testbed-bootstrap`. Pull-request builds never run on the self-hosted deployment runner.

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

## Safety and troubleshooting

- The destination marker and existing `plugins` directory are mandatory. If either check fails, no deployment occurs.
- The deploy job only accepts the versioned JAR artifact created by the successful build in the same workflow run.
- Replaced JARs are backed up under `.testplex-backups/<timestamp>/` inside the test server root. Retain a known-good backup until the new version has been tested.
- If the server process does not stop or return within three minutes, deployment fails. Startup failure triggers a best-effort rollback, but the workflow cannot guarantee recovery from a broken server launcher or host-level failure; keep VM snapshots and a tested manual recovery path.
- Do not enable auto-deploy until the runner and paths have been verified on the isolated VM. Self-hosted runners execute repository code, so only trusted project members should be able to push to the deployment branch or enable the deployment variable.


## Native Monster Maze test variants

The new games are part of the Reborn Arcade plugin, not a standalone JAR. In the dedicated test server's server-group configuration, add the enum values `MonsterMazeSpeed` and `MonsterMazeModern` to its configured games list while retaining `MonsterMaze`. These entries share the existing Reborn Monster Maze map pool and kit/stat systems. Do not install the standalone `MonsterMazeStandalone.jar`.

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
