# Snake — Paper 26.3 source-parity port

This module ports the existing Mineplex Reborn 1.8.9 Snake game into the isolated Java 25 / Paper 26.3 test platform. The original implementation in `Plugins/Nautilus.Game.Arcade/.../snake/Snake.java` and its three archived maps under `update/maps/Snake` are the source of truth.

## Source features carried into the modern port

- The original Snake Pit, Snake Volcanic and Snake WinterBabylon map archives are packaged in `SnakeModern.jar`. `/snake maps` lists them and `/snake map <name>` changes the selected archive while the game is idle. Paper 26.3 legacy-world storage is prepared before the map is loaded.
- A player rides the leading sheep entity, with sheep segments following the travelled path. The player is not made invisible; the 1.8 game uses a normal third-person player model mounted on the sheep head. Segment colour is assigned per player.
- The snake grows by one segment each second. Green slime food spawns once per second in the source radial distribution; eating a slime adds two segments and updates the local Slime-upgrade/stat path.
- Source movement speed follows the original length-dependent curve, including scaling the base multiplier for Speed and the 126-segment speed plateau while the tail itself continues growing. Participant colours follow the original 1.8 dye-data order. Reverse pauses normal movement for two seconds, reverses the segment order and rebuilds the path so the tail does not collapse.
- Speed, Reverse and Invulnerable kits retain their source items (5 feathers, 3 cookies and 2 nether stars respectively), two-second abilities and one-second reuse cooldown.
- The physical Magma Cube item boxes retain the 10-second first spawn, 15-second subsequent interval, four-cube cap, 48-point golden-angle spawn search, five-block clearance, native hop/gravity, 0.12 steering every four ticks, 2.1-block contact radius, 12-step hotbar roll, and per-player 1/3/5/10/15/20/30-second steering-debuff escalation. An occupied roulette does not consume another cube.
- Tail collisions use the source 1.2-block radius and first-three-segment self-collision grace. Invulnerability and Reverse suppress collision checks. Lava is fatal; ordinary damage, combustion and vanilla entity targeting are cancelled.
- The original idle elimination is retained: the head must make more than two blocks of progress within each two-second window. Reverse refreshes the idle position just as the original ReverseUpdate does.
- Eliminated players receive the Tracking Compass. The nearest live player is tracked; left-click spectates the nearest survivor with the original three-second cooldown, and right-click opens the survivor selection menu. Compass items cannot be dropped.
- Source achievement/stat thresholds are retained locally for the isolated test server: Cannibal at six kills, ChooChoo at a tail length of 60, and SlimySheep after 20 slimes. Round chat stats include kills, tail length, slimes eaten and kit. Lifetime totals are stored in `stats.yml`.
- The combined Snake cosmetic line keeps the legacy palettes (Elemental, Neon, Sunset) and ability-specific Speed / Super / Reversal colours and visual styles. Snake-type segment sets (Anaconda, Cobra, Neon Viper, Reef Serpent) are selectable alongside those options in one menu line.

## Commands

- `/gamepanel` → Snake → Start / Test.
- `/snake join`, `/snake start`, `/snake kit <speed|reverse|invulnerable>`, `/snake status`.
- `/snake maps`, `/snake map <name>`, `/snake stats`, `/snake leave`.
- `/snake stop` is operator-only.

## Acceptance boundary

The automated tests cover deterministic source rules and map metadata parsing. A successful Java 25 / Paper 26.3 build does not establish client-visible or multiplayer parity. The three original maps, mounted movement/camera presentation, steering and tail collision, kit timings, food and item-box interaction, spectator tracking, achievements, and cleanup still require the in-game acceptance matrix on the isolated 26.3 server. Snake remains marked mechanics-in-progress until those checks pass.
