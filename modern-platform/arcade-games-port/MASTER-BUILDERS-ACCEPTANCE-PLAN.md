# Master Builders — Paper 26.3 source-parity acceptance plan

**Readiness:** `READY_FOR_TEST` — available in the shared controlled Arcade queue. Live multiplayer and full judging acceptance remain outstanding.  
**Map source:** No legacy 1.8 ZIP archive exists for Master Builders; the modern port deliberately generates its own 4×4 plot arena.  
**Scope:** One shared Master Builders game ID with up to 16 builders; it is not a separate queue per plot.

The no-archive policy is explicit in `SourceMapCatalog.mapFoldersFor` and the build workflow. Do not invent or attribute an archived source map to this game.

## Lobby and plot assignment

- [ ] Verify the game panel and `/arcadeport play master-builders` use the shared Arcade lifecycle and generated Master Builders world.
- [ ] Enforce a maximum of 16 builders and reject invalid counts above 16 before allocating plots or mutating player state.
- [ ] Allocate 16 unique plots using the 4×4 plot grid. Check indices 0–15 are unique and 16 wraps only for the pure coordinate helper, never for a second participant assigned in the same round.
- [ ] Each player receives exactly one plot; no plot is shared, omitted or reassigned to a second online builder.
- [ ] Confirm plot borders, build volume, spawn and visitor entry points are disjoint and do not overlap the next plot or the lobby.
- [ ] A one-player practice run must be supported without requiring a second builder.

## Build phase

- [ ] Start the source three-minute build timer at the Live transition, not at join, map preparation or kit selection.
- [ ] Builders can place/break blocks only inside their own plot and the allowed vertical bounds. Visitors, spectators and other builders cannot alter a plot.
- [ ] Inventory access, block placement and palette/creative tools remain available only to the assigned builder during Build; block placement outside the plot and attempts to bypass the build boundary are rejected.
- [ ] At the deadline, all plots lock simultaneously. A late block break/place, inventory click or queued interaction cannot mutate a locked plot.
- [ ] Test all 16 plots at minimum and maximum coordinates and at lower/upper Y boundaries. Ensure plot index arithmetic does not wrap a player into another builder's region.

## Review and voting

- [ ] Transition from Build into the review phase on the same authoritative deadline for all participants.
- [ ] Each plot review uses the source eight-second viewing window; the next plot advances exactly once and the timer resets per plot.
- [ ] The builder is not allowed to vote for their own plot. A missing vote is handled as the source default/abstention and not accidentally cast for the next plot.
- [ ] Voting is limited to the available 1–6 vote items; values outside that range and duplicate interactions do not increment the score.
- [ ] Confirm every eligible judge sees each non-owned plot exactly once and all plots receive the same voting window.
- [ ] Disconnecting builders, judges or spectators cannot stall the review sequence, duplicate a plot, or cause a stale score to be assigned to a different builder.
- [ ] Final ranking is stable and deterministic for ties; confirm the winning builder and displayed results agree with the stored totals.

## Session cleanup

- [ ] Restore player snapshots and temporary game-mode/inventory changes after natural finish, operator stop, disconnect and immediate rematch.
- [ ] Reset all generated plots between matches, including blocks at the top and bottom build limits, without deleting lobby structures or adjacent arena blocks.
- [ ] Verify no plot ownership, votes, timers or review-state survives into the next match.

## Controlled-queue acceptance gate

Master Builders is promoted for controlled testing because the generated 16-plot arena, 16-builder capacity and timed build/review loop are present with deterministic plot-allocation tests. This is **not** live gameplay certification. Run a full 16-player session with a second observer, test every plot and vote value, then repeat disconnects, boundary checks, tie scoring and immediate rematch cleanup on the isolated Paper 26.3 server.
