# Fly.io Testplex tester — release preparation

**Status: preflight only. No Fly app, Machine, volume, or deployed server is created by this PR.**

The existing Hyper-V deployment remains the staged source of truth. The Fly
release must use a previously successful `testplex-modern-paper-26.3` GitHub
Actions artifact; it must not rebuild plugin JARs during promotion.

## Confirmed tester decisions and app state

- The tester server uses **ephemeral storage**. Do not attach a volume. World,
  player, and stats data may reset when the Machine is replaced or the server
  is redeployed.
- Josh prefers a separate Fly app if it has no additional cost. If reusing
  `monstermaze` reduces cost, use the existing app; if costs are unchanged,
  prefer a separate app. Any replacement of an existing process must have a
  recorded, reversible image/configuration rollback.
- The supplied Fly CLI output showed app `monstermaze` in IAD, with two stopped
  4 GB performance-2x Machines (`mm18` on port 25565 and `mm21` on port
  25566), no Fly volumes, and a dedicated public IPv4 currently listed at
  $2/month. The app is the standalone Monster Maze server.
- The Hyper-V Testplex server listens on port 25568. A Fly candidate should use
  direct TCP 25568 unless the selected service/process layout explicitly
  preserves that port another way.
- Current price reference for IAD: a continuously started `performance-2x`
  4 GB Machine is about **$66/month**; stopped Machines still bill roughly
  $0.15/GB/month for root filesystem storage, and a dedicated IPv4 is $2/month.
  Network egress is additional. Reusing the existing stopped `mm21` process
  and its app-bound dedicated IP can avoid a second $2/month IP charge, but it
  does **not** remove compute charges while Testplex is running. Verify the
  account's current bill and machine rootfs size before promotion.
- The candidate service template keeps Fly Proxy autostop enabled with
  zero minimum running Machines, but **disables proxy autostart**. A raw TCP
  connection or status ping to Fly's public address must never start Testplex.
  The Oracle wake gateway is the only wake path: it validates a Minecraft
  login username and UUID, then explicitly starts the Machine through Fly's
  Machines API. Once the machine is running, the gateway tells the player to
  connect to the direct Fly service. Keep one `mm21` Machine provisioned;
  autostop can stop it between sessions without removing its configuration.
  The stopped Machine still incurs root-filesystem storage charges. Verify
  that status pings leave it stopped and an authorized gateway login starts it.

The app cost comparison and current machine/image state still need to be
rechecked immediately before promotion. The live Machine's per-machine
`autostart` setting must also remain false; a future `fly deploy` must not
restore proxy autostart from stale release metadata. This PR makes no Fly API changes.

## Implemented now

Use the manually dispatched **Prepare Testplex Fly tester release (no deployment)**
workflow with the run ID of a completed, successful integration-branch build.
It verifies the source run's repository, branch, workflow and conclusion,
downloads the exact two JARs and 20-map Wizards archive, validates their
contents and produces a checksum manifest. It never requests a Fly token or
calls `fly deploy`.

`deploy/fly/fly.toml.example` documents TCP 25568, intentionally omits a
volume, and uses Fly Proxy autostart/autostop to reduce idle compute. It remains
a template; no deployment workflow consumes it.

## Remaining blockers before a deployable Fly release

1. **Choose the cost-safe target.** Recheck Fly pricing for a new app/IP and
   running Machine. Compare that with replacing the stopped `mm21` process in
   `monstermaze`, while preserving `mm18`. Do not alter either process until
   the exact prior image and app config have been recorded and rollback has
   been verified.
2. **Package every runtime dependency.** Paper 26.3 needs Java 25, a verified
   Paper build/checksum, `TestplexModernArcade.jar`,
   `ArcadeMechanicsModern.jar`, required PacketEvents and FakePlayerPlugin
   versions, 20 Wizards map archives, and seven pinned physical Monster Maze
   worlds (`mm_void`, `mm_volcano`, `mm_colombia`, `mm_sandycoast`,
   `mm_siberian`, `mm_swampland`, `mm_tesorohundido`). The preflight
   artifact currently omits the Monster Maze physical worlds and Paper runtime.
   Determine a safe, efficient image/staging strategy and verify all other
   source-map assets. Do not depend on an unpinned live download at startup.
3. **Make reset semantics explicit.** No volume is planned. Package world
   assets read-only in the release image and initialize mutable server data in
   the ephemeral filesystem. Confirm expected reset behavior after redeploy
   and restart, and ensure server shutdown does not corrupt the image assets.
4. **Check backend dependencies.** Inventory the Hyper-V runtime's database,
   Redis, network access, secrets and permissions. Use separate tester
   credentials and connectivity rather than production secrets.
5. **Require manual release approval.** Use a protected GitHub Actions
   environment and a least-privilege, app-scoped Fly deployment token stored
   only in repository/environment secrets. Promotion must verify the candidate
   artifact SHA256 manifest, pin Paper and the image, and refuse an unapproved
   app/process target.
6. **Verify a real server and rollback.** Require external TCP connection on
   25568, signed-in Minecraft join, all 32 selections, documented data-reset
   behavior, clean shutdown, resource usage, and a proven rollback before
   publishing an invite.

The tester should initially use `online-mode=true`, a whitelist for controlled
acceptance, no exposed RCON, and a public rather than localhost-only bind.
Opening access beyond approved testers requires a separate decision.

## What this PR does not do

- It does not create a Fly app or volume.
- It does not modify or deploy `monstermaze` or any other Fly app.
- It does not touch the Hyper-V server.
- It does not yet include a complete deployable image or live promotion action.
- It does not claim map, backend, or public multiplayer acceptance.

Relevant documentation: [Fly app configuration](https://fly.io/docs/reference/configuration/),
[Fly CLI automation and tokens](https://fly.io/docs/flyctl/integrating/), and
[Fly resource pricing](https://fly.io/docs/about/pricing/).
