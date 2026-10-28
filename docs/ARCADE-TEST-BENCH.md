# Arcade Test Bench

The isolated Testplex Reborn server exposes an operator-only `/arcadetest` command. It is intended to be opened from the ordinary Arcade lobby; it does not replace the Game Panel or the normal Cosmetics menu.

## Win-effect tester

Run `/arcadetest` and choose **Win Effect Tester**, or use:

- `/arcadetest win list` — list the registered `Arcade …` win-effect gadgets.
- `/arcadetest win <effect name>` — replay a selected effect. Quotes are not needed for names containing spaces.
- `/arcadetest stop` — end the active test early and restore the tester.

Playback uses the registered Mineplex `WinEffectGadget` implementation, not a second animation. The command only starts from the lobby. Playback is isolated to a separate `arcadetest` world so the flat test arena, themed builds and temporary entities do not overwrite the lobby or game maps. When playback finishes, the selected gadget's cleanup runs, the temporary arena is reset, and the tester's saved location, inventory, armour, game mode, food, health, experience, flight state and potion effects are restored.

Only one win-effect test can run at a time. If the player leaves or the plugin shuts down, the test's cleanup path runs.

## Summoned per-game bots

Open **Game Bot Spawner** from `/arcadetest`, or use:

- `/arcadetest bot <game>` — summon three stationary dummies.
- `/arcadetest bot <game> <count> [moving]` — summon 1–8 stationary or slowly moving dummies.
- `/arcadetest clear` — remove the invoking operator's dummies.
- `/arcadetest stop` — stop the current win-effect playback if one is running.

Use a game key such as `baconbrawl`, `bomblobbers`, `deathtag`, `dragonescape`, `dragons`, `evolution`, `gladiators`, `microbattles`, `monstermaze`, `oneinthequiver`, `paintball`, `runner`, `sheepquest`, `snake`, `spleef`, or `turfwars`. The menu displays the supported games. Dummies spawn near the operator and carry a representative held item for that game. Append `moving` for slow circular motion.

The spawner is bounded to eight dummies per operator and 32 across the server. Dummies are cleaned up when the owner runs `/arcadetest clear`, disconnects, or the Arcade plugin shuts down.

## Important limitation

The summoned actors are temporary player-skinned ArmorStand disguises, **not real Bukkit Players**. They help inspect visible models and held items, provide target practice, and exercise some entity/projectile interactions. They do not join Mineplex teams and cannot trigger every mechanic that explicitly requires a real online Player, player account, kit registration, or player-specific cosmetic event. Use a second real client for those cases. These are test tools, not evidence that every cosmetic is correct or that a game has passed manual testing.

Both features are available only to operators when the exact `.testplex-sandbox` marker is present. They are not enabled on production servers.
