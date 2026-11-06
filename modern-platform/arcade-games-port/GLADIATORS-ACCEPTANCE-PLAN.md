# Gladiators — shared Paper 26.3 acceptance plan

## Test objective

Gladiators joins the shared acceptance queue with its classic **Normal** ruleset and all four voted variants. Normal is a real fifth option, not an assumption that one of the four variants is the default.

Start a quick practice session:

```text
/arcadeport play gladiators
```

For a tournament with multiple players:

```text
/arcadeport join gladiators
/arcadeport start gladiators
```

## Ruleset vote — all five must be visible

- [ ] The lobby vote menu shows **Normal**, **Rodless**, **Bowless**, **Modern** and **Pot** as five distinct choices.
- [ ] Normal is the default when the queue has no saved votes, when no one votes in the live vote, and for solo practice.
- [ ] Normal gives the tier sword, bow, the correct tier arrow count and a fishing rod.
- [ ] Rodless gives the bow and arrows but no fishing rod.
- [ ] Bowless gives the fishing rod but no bow.
- [ ] Modern gives the sword, stone axe and shield, with neither bow nor fishing rod.
- [ ] Pot gives the sword and healing potions, with neither bow nor fishing rod.
- [ ] Existing lobby votes carry into the match vote; changing a vote updates the visible count.
- [ ] Equal/no-vote outcomes resolve deterministically to Normal rather than silently selecting Rodless.
- [ ] Classic-tool achievements are available in Normal, Rodless and Bowless, and not in Modern or Pot.

## Tournament and combat lifecycle

- [ ] Original Gladiators source map loads in its own isolated world and uses the source arena markers/bounds.
- [ ] A single random seed fixes every winners', losers', and conditional grand-final-reset slot when the tournament starts.
- [x] Automated bracket simulations cover every player count from 2 through 16, including 3, 5, 8, and 16; 12 different duel completion orders per count preserve the same fixed slot dependencies, and 100 seeded simulations for 8 and 16 players avoid an avoidable round-two rematch.
- [ ] The 16-player bracket has winners' rounds of 8/4/2/1 and losers' rounds of 4/4/3/2/1 matches; the lower semifinal has four entrants after the winners' finalist drops in.
- [ ] Reordering independent match completions never changes feeder relationships or designated opponents.
- [ ] Each participant enters at most one duel at a time; independent ready matches start concurrently when distinct arenas are available.
- [ ] Byes and unavailable participants resolve without a duel or tournament deadlock.
- [ ] Each duel has the full five-second start lock; players cannot damage, shoot or interfere with another pair.
- [ ] First defeat advances through the predetermined lower-bracket feeder; second defeat eliminates the player.
- [ ] A lower-bracket duel starts as soon as its feeder results are known and an arena is free, without waiting on unrelated duels.
- [x] Automated tests cover the winners' champion ending the tournament immediately and both finalists entering the reset final after a lower-bracket grand-final win; either player can win the reset without giving the winner a third loss.
- [ ] Unresolved duels receive the intended poison escalation after 60 seconds, without poisoning completed duels.
- [ ] Environmental damage remains active inside a duel; eliminated and out-of-pair players cannot be damaged.
- [ ] A disconnect, quit, death or server-side stop resolves the affected pair and does not strand either bracket.
- [ ] Solo practice does not create a tournament win or advance a bracket.

## Mirrored world and spectator checks

- [ ] The losers' bracket receives a separate world extracted from the same selected source-map archive, preserving terrain, arena markers and configuration.
- [ ] Winners' fighters remain in the original map and losers' fighters remain in the mirror; arena assignments do not overlap within either world.
- [ ] A spectator can switch between winners' and losers' matches, return to the match list, and cannot attack or interact with fighters.
- [ ] The spectator match list shows bracket, round, both fighters, current state, and completed results.
- [ ] Waiting fighters can spectate and are returned to their assigned arena when their next duel begins.
- [ ] The lower-bracket champion is moved to the original map for the grand final.
- [ ] Ending, stopping, or restarting removes only the temporary mirror instance; interrupted-session mirror worlds are cleaned at startup.

## Hotbar, stats and presentation

- [ ] The in-duel Hotbar Editor opens from the editor item and `/arcadeport hotbar`, swaps slots and preserves the editor item.
- [ ] Saved hotbar layouts are restored in a later duel and empty/legacy layouts do not remove the editor.
- [ ] Kill-message cosmetics save and display the chosen style.
- [ ] Achievement and milestone progress is player-scoped; the no-bow/no-rod challenge and half-heart win are awarded only when their conditions are met.
- [ ] The Silky Seconds lower-bracket milestone records correctly.
- [ ] Tournament status, bracket queues, duel result messages, spectator state and post-round lobby scoreboard remain readable.
- [ ] Ending or restarting the tournament restores inventories, effects, locations and arena/world state.

## Draw My Thing regression checks

- [ ] The game has a 320-second total timer; round time divides the available drawing time among queued players and never exceeds 40 seconds.
- [ ] With 8 or fewer players, each drawing turn is 40 seconds; above 8 players, the 320-second drawing budget is divided across all turns with no intermission.
- [ ] The drawer can select every palette colour from the inventory and the source map's GREEN swatch area without moving or losing palette items; swatches preserve their actual map material and colour.
- [ ] Hint, correct-guess, painting, fill, colour-selection, and clear-board sounds play for the intended audience, including the 1.8 splash sound for bucket fill, and do not spam unrelated players.

## Exit criteria

Keep Gladiators enabled in the shared queue while fixing any defects found. Do not mark it gameplay-accepted until the five rulesets, bracket simulations, both world instances, spectator switching, disconnect cases, environmental damage and saved hotbar behaviour have been exercised on Paper 26.3. Automated bracket checks are not live gameplay acceptance.
