# Snake shared Arcade module

Snake's game controller and rules are compiled from this source folder into `ArcadeMechanicsModern.jar`. It is an in-process `ArcadeGameModule`; it has no Bukkit plugin descriptor, independent command lifecycle, or separate deployment artifact. The shared Arcade lobby owns its queue, launch, shutdown and readiness. The original implementation in `Plugins/Nautilus.Game.Arcade/.../snake/Snake.java` and its three archived maps under `update/maps/Snake` remain the source of truth.

## Ported features

- Snake Pit, Snake Volcanic and Snake WinterBabylon are bundled in the Arcade JAR. `/snake maps` lists them and `/snake map <name>` selects one while idle.
- The nine second frozen preparation period, sheep disguise, length based speed curve, food growth, tail collision, idle elimination, Reverse, Speed and Invulnerable kits, spectator compass, score tracking and original achievements remain implemented.
- The Magma Cube item box and its item roll are retained as intentional modern features.
- Participant heads are distributed around the source spawn ring to avoid adjacent initial heads colliding at the start. Startup logs identify the selected map and parsed `TEAM_SPAWNS` count.

## Commands

- `/gamepanel` â†’ Snake â†’ Start / Test launches through the shared Arcade queue.
- `/snake join`, `/snake start`, `/snake kit`, `/snake status`, `/snake maps`, `/snake map <name>`, `/snake stats`, `/snake leave` and operator-only `/snake stop` remain available.

## Acceptance

Source-rule and map-metadata checks remain with the Snake sources. A successful build does not establish client-visible or multiplayer parity. All three maps, mounted movement and camera presentation, steering, collision, kits, food and item box interaction, spectator tracking, achievements and cleanup still need in-game acceptance on Paper 26.3.
