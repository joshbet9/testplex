# Plexball — Paper 26.3 test plan

Plexball is an experimental in-process mode hosted by `ArcadeMechanicsModern`. It is deliberately **not** added to the READY_FOR_TEST allow-list or the normal rotating lobby queue. It uses a separate generated world named `testplex_plexball_arena_v1` and returns participants to the shared Testplex Arcade lobby when the match ends.

## Start and kit selection

- `/arcadeport kit slugger`
- `/arcadeport kit technician`
- `/arcadeport kit curve-artist`
- `/arcadeport kit sprinter`
- `/arcadeport kit skyguard`
- `/stats` or `/stats plexball` — open the Plexball career page with achievements, milestone progress and lifetime statistics.
- `/arcadeport join plexball` — normal five-second queue countdown.
- `/arcadeport play plexball` — quick two-second queue countdown.
- `/arcadeport leave plexball` — leave the queue before a match starts.
- `/arcadeport start plexball` — operator-only immediate start for two or more queued players.
- `/arcadeport status` — shows the Plexball queue or current phase.
- `/arcadeport stop` — operator stop and return to lobby.

At least two and at most eight players are supported. Each player gets one initial batting turn. Everyone other than the current batter fields. The first round is capped at ten seconds per batter. Multiple home runs enter five-second sudden death, bounded by the same 180-second wall-clock match limit.

## Achievements

Achievements are one-time challenges, unlocked from completed match results and saved per player UUID.

| Achievement | Requirement |
| --- | --- |
| Sweet Spot | Land an uncaught hit of at least 75 metres. |
| Warning Track | Land an uncaught hit of at least 95 metres. |
| Out of the Park | Hit a fair 100-metre home run. |
| Perfect Contact | Record three top-grade contacts across your Plexball career. |
| Sticky Gloves | Make at least three successful catches in one match. |
| Golden Glove | Make at least five successful catches in one match. |
| All-Rounder | Make a catch and land an uncaught hit of at least 75 metres in one match. |
| Clutch Performer | Win a match after hitting at least two home runs in sudden death. |
| Champion | Win five Plexball matches in a row. |

Unlocks send a player notification and challenge-complete sound. They are local Testplex progression only; no production Mineplex currency, XP, title or cosmetic reward is granted.
Raised achievement requirements use versioned internal unlock keys, so an unlock earned under an earlier, easier requirement does not automatically satisfy the harder challenge. The 300-catch Wall of Leather milestone likewise has its own versioned key.

## Career milestones

Milestones are cumulative and persist between matches and server restarts. The /stats page shows individual progress bars and completion states.

| Milestone | Lifetime target |
| --- | ---: |
| Winning Legend | 250 wins |
| Wall of Leather | 300 successful catches |
| Home Run Hero | 100 home runs |
| 10 Kilometre Club | 10km total uncaught hit distance |

The progression file is plugins/ArcadeMechanicsModern/plexball-progression.yml. It is separate from kit preferences and production account/stat storage.

## Gameplay acceptance

| Test | Expected |
| --- | --- |
| Join with 2–8 players | Queue starts and every participant enters the separate Plexball arena. |
| Queue leave | A player can remove themselves while queued; countdown resets if the queue falls below two players. |
| Batter turn | Only the selected batter can hit; movement is locked but look direction remains available. |
| Single swing | The first click on the tee consumes the at-bat, including a miss or foul; repeated clicking cannot retry until timing improves. |
| Fielder turn | Every non-batter can move throughout the fair triangle. |
| Fielding hotbar | Fielder can switch between slot 0 (glove) and slot 1 (kit ability); other slots are blocked. |
| Standings | Distance and successful catches are broadcast after each completed batting attempt. |
| Player HUD | Every participant sees a live sidebar with phase, match clock, current batter, personal best, catches, kit/ability and the top three distances. |
| Progression menu | `/stats plexball` opens a read-only inventory page listing all nine achievements, four long-term career goals and the player's saved totals. |
| Achievement unlocks | Each achievement unlocks once, is saved per UUID, and notifies the player with a message and challenge-complete sound. |
| Milestone unlocks | Wins (250), successful catches (300), home runs (100) and uncaught hit distance (10km) progress cumulatively and persist between restarts. |
| Crosshair | Clicking the tee Interaction entity only counts if the server-side eye ray intersects the smaller ball sphere. |
| Timing | Each at-bat gets a newly phased beat; near-perfect timing grades progressively rather than binary hit/miss. |
| Direction | Swing yaw must be inside the fair cone; off-cone direction is a foul. |
| Ball physics | Server simulates position, gravity, spin and distance; no client projectile velocity or reported distance is accepted. |
| Catch | A fielder gets one catch attempt per ball; catch is based on the server's swept ball segment and buffered fielder position. |
| Catch tiebreak | A valid catch voids the batter's hit and increments the catcher's count. |
| Kits | Slugger, Technician, Curve Artist, Sprinter and Skyguard have distinct batting/fielding identities. |
| Rotation | After batting, the previous batter joins the fielders for the next player. |
| Home run | A fair 100m hit is recorded as a home run. |
| Sudden death | Multiple home-run players take five-second turns; a finalist must hit another home run to survive the round. |
| Match cap | The game ends at 180 seconds of wall-clock time, even if server TPS falls below 20. |
| Cleanup | Ball, tee Interaction and player inventories are cleaned; online participants return to the shared Arcade lobby. |
| Repeated rounds | Field generation marker prevents rebuilding the entire field on every match. |

## Latency and fairness matrix

Test with at least two clients and compare a low-latency client against one with controlled network delay. Record the Paper ping estimate, separately measured network RTT, swing timing grade, catch timing, actual path intersection, and result. Paper's `Player#getPing()` is a coarse estimate and is **not guaranteed to be network RTT**; it is used only as a bounded compensation hint and never as an anti-cheat signal.

| Paper ping estimate | Expected compensation |
| ---: | --- |
| 0–49ms | 0 ticks |
| 50–149ms | 1 tick |
| 150–249ms | 2 ticks |
| 250–349ms | 3 ticks |
| 350–449ms | 4 ticks |
| 450ms or higher | Capped at 5 ticks (250ms maximum rewind) |

The server uses Paper's ping estimate only to choose a bounded rewind, then validates against server-recorded player and ball history; client-provided timestamps and client-reported ball/catch results are never trusted. Player positions and ball positions are kept in short server-side histories, and catches use a swept-segment test to avoid missing fast balls between ticks.

A high-ping client must not receive a catch merely because its packet arrived later: the historical ball path and historical player position must still intersect the catch window. Conversely, ordinary network delay should not make a valid pre-deadline input lose just because its packet arrived later. Compensation is intentionally bounded; latency above the cap can still affect play. This is **latency tolerant, not ping independent**: the separate measured-RTT matrix must pass before the game is promoted.

## Anti-cheat checks

- Repeated fixed-delay swing macros must not consistently obtain perfect timing because each at-bat receives a server-generated random phase and cycle.
- Repeated clicks cannot produce more than one swing; the first click on the tee Interaction consumes the at-bat even when it misses or is foul.
- The larger tee Interaction entity is only an interaction target; the server ray must hit the actual ball sphere.
- Catch spamming cannot create repeated windows; each fielder has one catch action per ball.
- Client timestamps, reported trajectory, reported hit power and reported distance are not inputs to the result.
- Movement outside the triangle or above the permitted field height is rejected; normal movement is otherwise left to Paper.
- Ability use is one per fielder per ball and is checked by the game module.

## Known experimental limits

- The current version uses right-click to swing at the tee Interaction entity and right-click the glove to attempt a catch. The match HUD uses a persistent scoreboard, role-specific action-bar prompts and phase/result titles. It does not provide a custom client-side crosshair overlay or a full swing animation.
- Ball rendering is an ItemDisplay moved by the server-side simulation. Its visual smoothness may vary by client FPS/network, but the server path and score do not.
- The current catch rewind is capped at five ticks (approximately 250ms one-way estimate). Do not advertise Plexball as completely ping independent; validate the RTT matrix and adjust only with measured evidence.
- Achievements and career milestones are implemented in a local Testplex profile (`plexball-progression.yml`). They provide unlock notifications and a stats menu, but do not award production Mineplex currency, XP, titles or cosmetics. Ranked leaderboards, spectator controls and public matchmaking are still out of scope.
