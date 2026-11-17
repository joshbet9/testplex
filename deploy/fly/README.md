# Testplex Modern on Fly.io

The Paper data root is `/server`. The `fly.toml` mount named
`testplex_data_restore` is required: it holds the default world and player
data, plugin configuration, statistics, achievements, profiles, and other
persistent server state across machine replacements and deployments.

The Fly image contains the seven Monster Maze source worlds from the same
pinned repository revision used by Hyper-V. At startup, `/server` receives
small symlinks to those image assets; the map files stay outside the persistent
volume while Paper continues using `/server` as its world container. This
preserves vanilla player data as well as plugin stats across deployments.

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
