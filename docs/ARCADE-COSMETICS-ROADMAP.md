# Arcade Cosmetics — Game-by-Game Proposal

**Status:** Design proposal / implementation roadmap  
**Scope:** The Arcade Cosmetics menu, per-game cosmetic concepts, and scripted win-effect ideas for the Mineplex Reborn test instance. This document records intent; it does not claim that the proposed items or win effects have been implemented.

## 1. Menu structure

Add a **jukebox named Arcade Cosmetics** to the existing cosmetics menu. Selecting it opens the Arcade Cosmetics hub.

The hub should provide a button for each supported game in the rotation. Each game button opens that game's own cosmetic submenu, with its available/unlocked choices and the equipped choice clearly indicated. Keep the layout data-driven so adding a game or cosmetic does not require rebuilding the entire menu structure.

Initial game list for this proposal:

- Bacon Brawl
- Bomb Lobbers
- Death Tag
- Dragon Escape
- Dragons
- Evolution
- Gladiators
- Micro Battles
- Monster Maze
- One In The Quiver
- Paintball
- Runner
- Sheep Quest
- Snake
- Spleef
- Turf Wars

The list is the planned scope recorded here; verify it against the actual rotation configuration when implementation begins. If a game is not part of a particular rotation, avoid presenting a misleading entry for unavailable content, while retaining the player's saved cosmetic selection.

## 2. Per-game concepts

For every game, the **cosmetic** is the player-selectable appearance or effect used during normal play. The **win effect** is a short, staged gameplay-style sequence using the existing map and mechanics to celebrate the winner. These are separate features and should be tracked and tested separately.

### Bacon Brawl
- **Cosmetics:** Accessories for pigs and sheep to wear.
- **Win effect:** Torpedo/body-slam into four players, sending them flying off the edge.
- **Implementation notes:** Reuse the game's existing pig/sheep entities and collision/knockback mechanics. Accessories must remain cosmetic and must not alter hitboxes or combat behaviour.

### Bomb Lobbers
- **Cosmetics:** TNT explosion/impact effects.
- **Win effect:** Land three direct lob hits on the map's turtles, with suitable sound effects.
- **Implementation notes:** Confirm which turtle targets/map entities are available in each supported map. Keep cosmetic sound/particles distinct from the actual damage/explosion rules.

### Death Tag
- **Cosmetics:** Expanded Runner Trail options.
- **Win effect:** Knock three teammates off a shared landmark or gathering spot, using locations such as Pirate Harbour or the flag area on Halfmoon.
- **Implementation notes:** Detect/map the landmark where supported; do not assume the same coordinates on every map. Respect the normal game team and movement rules.

### Dragon Escape
- **Cosmetics:**
  - Custom death chat lines sent by a player when they die, such as “chat am i cooked?” or “grok is this true?”
  - A cosmetic beacon-colour choice for the leading player. The shared leader beacon changes to that player's selected colour for the whole lobby and updates when the leader changes.
- **Win effect:** Have two players parkour through the finish section, with the winner leaping ahead to finish first from third place.
- **Implementation notes:** Keep the chat lines selectable and within normal chat/safety limits. The beacon represents the current leader, so the colour must follow leader changes rather than remain attached to the previous leader.

### Dragons
- **Cosmetics:** Pyrotechnic ability particle effects and player-selected flare burst effects visible to the whole lobby.
- **Win effect:** Use a Pyro platform, then have the winner get hit by a dragon while everyone else falls.
- **Implementation notes:** Reuse the existing Pyro/platform and dragon interactions where possible. Flare effects are visual-only; the flare itself briefly excludes the user and nearby target area from every dragon's player and sparkler target selection. The staged sequence must not interfere with a live round or leave temporary entities/blocks behind.

### Evolution
- **Cosmetics:** Alternative evolving particle effects and dragon-egg appearance/effect choices.
- **Win effect:** Fly to the top of a building as a chicken, then evolve at the top.
- **Implementation notes:** Use a suitable existing map building and the game's native evolution flow. Avoid giving equipped choices any gameplay advantage.

### Gladiators
- **Cosmetics:** Custom kill and death messages.
- **Win effect:** Ironic rod/bow spam at an iconic final-fight location.
- **Implementation notes:** Identify supported landmarks per map and reuse the game's rod/bow actions. Keep any staged attacks harmless to the real lobby and ensure effects end cleanly.

### Micro Battles
- **Cosmetics:**
  - A choice of glass colours. Each player's choice changes the starting glass for that player on their side.
  - A matching colour particle effect when that player is within three blocks of the border.
- **Win effect:** Three players fall from the pillar, with the winner being the last to fall.
- **Implementation notes:** The glass colour is per player/side, not a global setting. Choose colours that remain readable against team and map colours; border particles must not obscure important gameplay cues.

### Monster Maze
- **Existing cosmetics:** The mob-set cosmetic collection is already implemented. Each set changes the beacon colour and Safe Pad colour.
- **Menu behaviour to refine:** In the Monster Maze cosmetics submenu, let players toggle each unlocked mob set on or off. Only enabled/unlocked sets should cycle in-game, so players can limit the rotation to sets they actually want. Add a final **Custom Pack** that lets players configure the beacon colour and Safe Pad colour for each mob set.
- **Win effect:** After Repulsor, pilot up to the top Safe Pad with three seconds remaining, accompanied by the Safe Pad sound.
- **Implementation notes:** Preserve existing sets and saved unlocks. Custom Pack options must not overwrite the standard sets. Reuse the real Repulsor, Pilot, Safe Pad and sound mechanics; restore temporary state after the celebration.

### One In The Quiver
- **Cosmetics:** A death-location effect triggered according to the killer's current streak. Higher streaks create a larger/more elaborate effect.
- **Win effect:** An archery bullseye.
- **Implementation notes:** Use bounded effect tiers so high streaks cannot cause excessive particles, lag or sound spam. Trigger the effect at the victim's death location and do not change the actual kill outcome.

### Paintball
- **Cosmetics:** **Needs further investigation.** Candidate directions are pellet impact effects or gun/weapon skins; determine which hooks/assets the native game supports before committing to a design.
- **Win effect:** A “Clutch or Kick” moment: kill eight players with one shotgun shot.
- **Research questions:** Can the game reliably expose pellet impacts independently? Can cosmetic weapon appearances be represented safely in the 1.8-era item/rendering system? Which existing shotgun/map interactions can be staged deterministically for a win effect?

### Runner
- **Cosmetics:** Replace the red Runner trails with player-selectable custom colours, provided they do not conflict with red map elements or critical route cues.
- **Win effect:** On Euphoria or Mini Euphoria, the winner runs a red “W” shape, which then breaks away.
- **Implementation notes:** Maintain strong map readability. Validate every trail colour against the maps and restore/remove temporary blocks/particles after use.

### Sheep Quest
- **Cosmetics:** The sheep-capture effect depends on the player who captures the sheep.
- **Win effect:** A game-winning capture with 0.1 seconds remaining on the clock.
- **Implementation notes:** Resolve the effect using the capturer's equipped cosmetic. The win sequence should use the real capture/scoring mechanic and avoid changing the authoritative match result.

### Snake
- **Cosmetics:** Snake skins paired with a distinct visual effect for each ability use.
- **Win effect:** The winner rides around in a zigzag pattern while the song plays.
- **Implementation notes:** Keep skin and ability-effect choices selectable per player. Ability visuals should communicate the ability being used and should not make other snakes or hazards harder to see. The win sequence should clean up its movement, sound and temporary effects when finished.

### Spleef
- **Cosmetics:** Effects when blocks are broken.
- **Win effect:** Axe-spleef a dense group of players in a small area, then leap back up.
- **Implementation notes:** Use existing block-break and axe-spleef mechanics. Any staged blocks or player movement must be confined to the celebration and leave the map in its expected state.

### Turf Wars
- **Cosmetics:** Impact effects when an arrow hits the floor.
- **Win effect:** A 360 no-scope shot off a mid-bounce.
- **Implementation notes:** Reuse arrow-impact and movement mechanics. Cosmetic impact effects must not affect projectile collision, team visibility or the result of the real match.

## 3. Shared design and engineering requirements

1. **Per-game selection:** Players can choose cosmetics independently for each game. Saved choices should survive reconnects and restarts using the existing cosmetics persistence approach.
2. **Unlocked/equipped state:** Clearly distinguish locked, unlocked, enabled and equipped choices. Monster Maze additionally needs the per-set enable/disable list so unwanted sets do not participate in cycling.
3. **No gameplay advantage:** Cosmetics may change visuals, sounds or presentation only. They must not change movement, hitboxes, damage, cooldowns, collision, scoring, visibility of gameplay-critical information or matchmaking.
4. **Use existing assets and mechanics:** Win effects should favour actual game entities, abilities, sounds and recognisable locations from supported maps. Avoid assuming that every map shares the same coordinates or landmarks.
5. **Isolated celebrations:** Win effects should not run on top of an active competitive round. Use a controlled winner/participant sequence, bounded duration, and reliable cleanup of temporary entities, sounds, effects, blocks and movement.
6. **Map-aware fallbacks:** Where a landmark or structure is absent, skip that location-specific beat or use an explicitly supported alternate. Never teleport a sequence to arbitrary coordinates just because a map differs.
7. **Performance:** Bound particle counts, entity counts, sounds and duration. Test effects with several players and on the test VM.
8. **Accessibility/readability:** Avoid blinding screens, excessive noise, or cosmetic colours that mask teams, hazards, trails, borders, arrows or other important game information.
9. **Separate validation:** For each game, test (a) menu navigation and persistence, (b) normal-play cosmetic trigger, and (c) win-effect sequence/cleanup independently.
10. **Development order:** First confirm the latest native Monster Maze and Snake changes can be loaded and tested on the Reborn server. Then implement the shared Arcade Cosmetics hub/menu, followed by individual game cosmetics and win effects. Paintball remains research-only until its hooks are understood.

## 4. Initial acceptance checklist

- [ ] Arcade Cosmetics jukebox appears in the existing cosmetics menu.
- [ ] Opening it shows the planned games that are actually available in the current rotation.
- [ ] Each game button opens the correct game's cosmetic submenu.
- [ ] Choices persist across relog/restart and remain independent by game.
- [ ] Cosmetics trigger only at their intended gameplay event and do not alter the game result.
- [ ] Win effects use the intended existing map/ability where supported and clean up after completion.
- [ ] Monster Maze unlocked-set toggles limit the cycling list; Custom Pack saves separate beacon/Safe Pad colours per mob set.
- [ ] Snake ability effects are distinct, readable and visible to other players as intended.
- [ ] Paintball cosmetic hooks are investigated and documented before implementation.
- [ ] Every game has a recorded pass/fail result and notes from a real 1.8.x in-game test.

## 5. Implementation status

This document is a product/design plan only. The Snake and Monster Maze implementation status must be verified against the deployed Arcade build and actual in-game behaviour; a successful workflow deployment proves artifact installation and startup checks, not that every game or cosmetic has passed manual testing.
