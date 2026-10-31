# Draw My Thing — Paper 26.3 source-parity acceptance plan

**Readiness:** `READY_FOR_TEST` — available in the shared controlled Arcade queue. Full live multiplayer and map-by-map acceptance remain outstanding.  
**Map source:** `update/maps/Draw My Thing/Draw_Stadium.zip` — one original archive.  
**Scope:** One shared Draw My Thing game ID, one drawer at a time, with the classic short-turn cadence for small lobbies and a five-minute total drawing budget for large lobbies.

The original map is enforced by `DrawMyThingMapArchiveTest`, which compares the exact source ZIP filename with the resource packaged in `ArcadeMechanicsModern.jar`.

## Original map inventory

1. `Draw_Stadium.zip`

The map selector must use the original Draw Stadium world and its marker-defined drawing/canvas area. It must not silently substitute a flat generated board when the source archive is selected.

## Turn schedule and lobby size

- [ ] Verify the shared lobby accepts the source map with the correct drawer spawn and canvas region loaded from the original archive.
- [ ] Every participant draws exactly once in a round, in a shuffled order, with the appropriate drawer/guesser state and no self-guessing.
- [ ] For lobbies of up to ten players, keep the original 30-second drawing turn and three-second transition between drawers.
- [ ] For more than ten players, divide the five-minute game budget across all players after reserving the three-second intermissions. Check 11, 12 and 16 players against the deterministic helper values and verify the complete round fits the five-minute budget.
- [ ] Confirm the timer starts when a drawer's turn begins, stops on correct completion or timeout, and cannot be reset by a reconnect, drawer death or GUI reopen.
- [ ] Check the first and final turn boundaries at 1, 10, 11, 12 and 16 players; no player is skipped or assigned a second turn.

## Drawing, guessing and hint flow

- [ ] Only the active drawer can make drawing changes. Guesser input is accepted only through the game's supported guess UI/command and never edits the canvas.
- [ ] Drawing changes remain inside the source canvas volume; clicks, block updates and interaction attempts outside the allowed drawing area do not affect the arena.
- [ ] Timed hints appear at their intended intervals and reveal only the configured hint information. The hidden answer must not leak through scoreboard, action bar, inventory names, block data or spectator messages.
- [ ] Correct guesses are credited once, immediately removed from the active guesser pool and cannot be replayed for duplicate rewards.
- [ ] Incorrect guesses obey the attempt/feedback limits; spamming, empty strings, case variation and repeated guesses must not bypass the rules.
- [ ] When all guessers have answered correctly, the current turn ends cleanly. If the timer expires, the next drawer begins without carrying over the previous canvas or answer.
- [ ] The drawer cannot submit their own answer as a guess or earn guess rewards from their own drawing.

## Lifecycle and cleanup

- [ ] Test drawer disconnect, guesser disconnect, reconnect and server-side player replacement during a turn. Drawer replacement follows the chosen source-compatible abort/skip policy and never strands the round.
- [ ] Stop, natural game completion and immediate rematch restore inventories, modes, effects, scoreboards, player locations and the lobby canvas state.
- [ ] Reset only the drawing volume and temporary display state; preserve the original source map outside the allowed canvas.
- [ ] Verify the shared lobby remains usable while the game is selected, queued, started, ended or stopped.

## Controlled-queue acceptance gate

Draw My Thing is promoted for controlled testing because the original Draw Stadium archive is bundled, the five-minute large-lobby scaling is implemented and covered by deterministic tests, and the shared turn lifecycle is present. This is **not** a claim of live gameplay certification. Complete the 1/10/11/12/16-player schedule checks, test all draw/guess/hint paths with multiple clients, and exercise disconnect/cleanup behaviour on the isolated Paper 26.3 server.
