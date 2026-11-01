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
- [ ] The first tournament stage scales correctly to the player count; concurrent duels start in separate arenas.
- [ ] Each duel has the full five-second start lock; players cannot damage, shoot or interfere with another pair.
- [ ] The winners' bracket advances only after every duel in its stage resolves.
- [ ] First loss moves a player into the lower bracket; second loss eliminates them.
- [ ] Lower-bracket pairings start at stage boundaries and can run alongside the next winners' stage.
- [ ] The grand final resets correctly when the lower-bracket finalist takes the first final.
- [ ] Unresolved duels receive the intended poison escalation after 60 seconds, without poisoning completed duels.
- [ ] Environmental damage remains active inside a duel; eliminated and out-of-pair players cannot be damaged.
- [ ] A disconnect, quit, death or server-side stop resolves the affected pair and does not strand either bracket.
- [ ] Solo practice does not create a tournament win or advance a bracket.

## Hotbar, stats and presentation

- [ ] The in-duel Hotbar Editor opens from the editor item and `/arcadeport hotbar`, swaps slots and preserves the editor item.
- [ ] Saved hotbar layouts are restored in a later duel and empty/legacy layouts do not remove the editor.
- [ ] Kill-message cosmetics save and display the chosen style.
- [ ] Achievement and milestone progress is player-scoped; the no-bow/no-rod challenge and half-heart win are awarded only when their conditions are met.
- [ ] The Silky Seconds lower-bracket milestone records correctly.
- [ ] Tournament status, bracket queues, duel result messages, spectator state and post-round lobby scoreboard remain readable.
- [ ] Ending or restarting the tournament restores inventories, effects, locations and arena/world state.

## Exit criteria

Keep Gladiators enabled in the shared queue while fixing any defects found. Do not mark it gameplay-accepted until the five rulesets, full tournament progression, disconnect cases, environmental damage and saved hotbar behaviour have been exercised on Paper 26.3.
