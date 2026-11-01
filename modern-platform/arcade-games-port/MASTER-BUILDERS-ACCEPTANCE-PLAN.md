# Master Builders — Paper 26.3 controlled acceptance plan

Master Builders is promoted to the shared controlled queue. The modern port creates 16 isolated builder plots rather than loading a legacy map archive. The review phase is part of the same match lifecycle.

## Queue and plot assignment

- [ ] Launch through the shared Game Panel and verify the expected supported player range and 16-player upper limit.
- [ ] Confirm every builder receives a unique plot, teleports to that plot at the start of the creative phase and cannot access another player's plot.
- [ ] Verify plot boundaries, floor/air space, player spawn orientation and separation between adjacent builds.
- [ ] Run a second match and confirm all 16 plot regions are cleared before reuse.

## Creative phase

- [ ] Verify the three-minute build timer and countdown presentation.
- [ ] Confirm block placement and breaking are allowed only within the assigned plot and only during the creative phase.
- [ ] Verify placement and breaking stop at the exact build deadline, including delayed and off-hand interactions.
- [ ] Test representative blocks, block states, containers and multi-block structures; confirm no build can escape its plot or alter the lobby.

## Review and voting

- [ ] Verify the review phase begins only after building ends and visits the expected eligible plots.
- [ ] Confirm ratings are accepted only during the review phase and are limited to the supported 1–6 range.
- [ ] Verify each player can rate a plot only once; off-hand duplicate vote events must not count twice.
- [ ] Test empty plots, incomplete reviews, player disconnects and a match with only the minimum supported number of participants.
- [ ] Confirm score aggregation, ties, winner selection and final result presentation.

## Lifecycle and recovery

- [ ] Exercise normal finish, leave, disconnect/rejoin, operator stop and server shutdown.
- [ ] Verify all temporary plot blocks are cleared through the complete allowed build height (Y=62 through 119) before the next match.
- [ ] Confirm inventories, game modes, location, effects, movement and scoreboards are restored and a new match can start cleanly.

## Acceptance record

Record build SHA, player count, themes, plot assignments, phase durations, vote results, disconnect cases and cleanup results. Plot/timing unit tests are prerequisites, not a substitute for a real multi-client build-and-review session.
