# Testplex Modern on Fly.io

The Paper server data root is `/server`. The `fly.toml` mount named
`testplex_data_restore` is required: it holds worlds, plugin configuration,
player statistics, achievements, profiles, and other server state across
machine replacements and deployments.

The Fly entrypoint checks `/proc/mounts` before writing anything and exits if
the volume is missing. Keep that check and the `[[mounts]]` declaration in
`fly.toml` together. A deployment without the mount must fail closed instead
of starting with a fresh empty data directory.

On the first start from the recovered snapshot, the entrypoint migrates the
legacy `ArcadeMechanicsModern`, `TestplexModernArcade`, and `FakePlayerPlugin`
data directories into Paper's `plugins/` data directories. It backs up any
existing destination directories under `/server/recovery/` and uses a marker
to ensure this migration runs only once.

There is one production machine in `iad`, so attach exactly one volume to that
machine. After deployment, verify `fly volumes list -a monstermaze` shows the
volume attached and `fly machine list -a monstermaze` shows the server healthy.
Fly's scheduled snapshots are enabled with five-day retention; they supplement
the live volume and do not replace it.

The current volume was recovered from the newest available snapshot. Do not
delete the older `testplex_data` volume or its snapshots while validating this
setup.
