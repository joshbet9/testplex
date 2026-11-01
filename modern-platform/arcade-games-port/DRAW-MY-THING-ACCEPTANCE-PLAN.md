# Draw My Thing — Paper 26.3 controlled acceptance plan

Draw My Thing is promoted to the shared controlled queue with the original Draw My Thing Stadium archive. The current implementation uses native Paper 26.3 wool painting and does not require a resource pack.

## Queue and drawing turns

- [ ] Start from the shared Game Panel and verify the queue, minimum/maximum participation and return to lobby.
- [ ] Confirm each queued player receives exactly one shuffled drawing turn.
- [ ] Verify the original Stadium archive loads with the intended drawer position, guesser positions and play bounds.
- [ ] For lobbies of 10 or fewer, verify the 30-second drawing window and three-second inter-turn gap.
- [ ] For lobbies above 10, verify turn durations scale against the five-minute game budget after reserving the inter-turn gaps; test 11, 12 and 16 players.
- [ ] Confirm hint reveal timing scales with the active drawing window and does not reveal the answer prematurely.

## Drawing and guessing

- [ ] Verify the drawing tool paints the expected wool colours and cannot modify blocks outside the drawing area.
- [ ] Verify each player has at most ten guesses per turn and the action-bar counter updates after each accepted guess.
- [ ] Confirm correct guesses are credited once, with the expected scoring/order and no duplicate off-hand interaction.
- [ ] Verify wrong guesses, blank guesses, repeated guesses and late guesses are handled consistently.
- [ ] Confirm a drawing turn closes at the deadline and no further painting or guessing is accepted.

## Disconnects and cleanup

- [ ] Disconnect the active drawer before anyone guesses correctly; verify the documented round-ending behaviour.
- [ ] Disconnect the active drawer after a correct guess; verify that the round continues and the remaining turn sequence is valid.
- [ ] Disconnect all remaining queued drawers; verify the final intermission is skipped when no online drawer remains.
- [ ] Exercise guesser disconnect/rejoin, operator stop, normal finish and server shutdown.
- [ ] Confirm the drawing world, temporary blocks, timers, scoreboard, inventory and player state are restored for the next lobby session.

## Acceptance record

Record build SHA, player count, turn duration, drawing/guess events, hint timing, disconnect timing and restoration results. Deterministic timing tests and source-archive packaging are prerequisites, not a substitute for a multi-client session.
