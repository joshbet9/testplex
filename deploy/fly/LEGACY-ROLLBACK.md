# Legacy Fly deployment rollback record

Captured before repurposing the `monstermaze` Fly app for Testplex Modern.

## Legacy images and source

- Fly app: `monstermaze` (`monstermaze.fly.dev`)
- Legacy image tag: `registry.fly.io/monstermaze:v1.3.9`
- Legacy immutable image digest: `sha256:5d9fcf6e06a54349882747cefd2a16e41a899d489681f6f0ca241fcdef4d44f1`
- Source repository: <https://github.com/joshbet9/MonsterMaze>
- Source revision from the image labels: `8d6a113a6ba8b598e60c5c26b422af8563f8e0bd`
- The two stopped Machines share the image above; do not remove either until the replacement has been accepted and the image tag is confirmed retrievable.

## Legacy Machines

| Process | Machine ID | State | Region | VM size | Public port |
|---|---|---|---|---|---:|
| `mm18` | `84503ef24605e8` | stopped | `iad` | `performance-2x`, 4 GB | 25565 |
| `mm21` | `85d3e1b44dd7e8` | stopped | `iad` | `performance-2x`, 4 GB | 25566 |

Neither legacy Machine had an attached Fly Volume at capture time. The original public Fly dedicated IPv4 is `37.16.31.125` and remains app-scoped.

## Legacy Fly configuration

```toml
app = "monstermaze"
primary_region = "iad"

[build]
  dockerfile = "Dockerfile"

[processes]
  mm18 = "1.8"
  mm21 = "1.21"

[[services]]
  protocol = "tcp"
  internal_port = 25565
  auto_stop_machines = true
  auto_start_machines = false
  min_machines_running = 0
  processes = ["mm18"]
  [[services.ports]]
    port = 25565

[[services]]
  protocol = "tcp"
  internal_port = 25566
  auto_stop_machines = true
  auto_start_machines = false
  min_machines_running = 0
  processes = ["mm21"]
  [[services.ports]]
    port = 25566

[[vm]]
  size = "performance-2x"
  processes = ["mm18"]

[[vm]]
  size = "performance-2x"
  processes = ["mm21"]
```

The legacy image and source revision above remain the rollback artifacts. The old `mm18` and `mm21` Machines were removed after the Testplex replacement passed startup readiness checks; recreate a legacy Machine from the recorded image/config if rollback is required. The app-scoped dedicated IPv4 remains allocated.
