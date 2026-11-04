# Wizards shared Arcade module

Wizards is a resource-pack-independent controller compiled into `ArcadeMechanicsModern.jar`. It has no separate Bukkit plugin or deployment artifact. The shared Arcade manager owns match startup, player snapshots, match completion and cleanup. Active Wizards implementation sources live under `modern-platform/arcade-games-port`.

Use the shared commands:

- `/arcadeport join wizards` to queue.
- `/arcadeport kit wizards <mage|sorcerer|mystic|witchdoctor>` to choose a kit.
- `/arcadeport start wizards` to start the Arcade queue.
- `/arcadeport leave wizards` to leave and restore player state.

The implementation still needs the listed source parity and live multiplayer acceptance work. Sharing the Arcade lifecycle does not claim that parity is complete.
