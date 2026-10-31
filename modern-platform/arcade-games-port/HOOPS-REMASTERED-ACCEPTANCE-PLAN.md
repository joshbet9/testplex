# Hoops Remastered — Paper 26.3 acceptance plan

## Match format
- 2–10 players, balanced RED / BLUE teams, one isolated court per session.
- Five-minute regulation; first to 21 with a two-point lead; a tied regulation score enters next-basket-wins overtime.
- 24-second shot clock. Two points inside the three-point arc and three points beyond it.

## Controls and gameplay
- Left-click with possession to pass toward a teammate in the player's forward passing lane; an unmarked pass travels forward and remains catchable.
- Right-click with possession to shoot toward the crosshair. The player must aim at the rim; the server owns the ball trajectory and validates a descending rim crossing.
- Left-click an opposing carrier in close range to attempt a deterministic steal (with a per-player cooldown).
- Right-click near an airborne opposing shot to swat it. Loose balls can be collected; recovering a missed shot records a rebound.
- Assists are credited when a teammate scores within eight seconds of a completed pass.

## Progress and presentation
- Live scoreboard: team score, regulation/overtime clock, shot clock and possession.
- Action bar: current player points, assists, rebounds, blocks and steals.
- Local lifetime totals: games, wins, losses, points, assists, rebounds, blocks, steals, shots attempted/made and PPG/APG/RPG.
- Hoops Remastered achievements use the existing progress catalogue; career milestones remain in its existing wins/points/assists/rebounds/steals/blocks catalogue.

## Automated validation
- HoopsRemasteredRulesTest covers lobby sizes, two/three-point classification, win-by-two, sudden-death overtime, clutch/buzzer windows, assist expiry, box-score awards, comeback threshold, spawn assignment and clock formatting.
- Maven compile/test must pass on Paper 26.3 / Java 25.

## Live acceptance still required
- Confirm the generated court and hoop geometry look correct from both ends.
- Test short/long shots, both hoops, two/three-point line boundary, missed-shot rebound and blocked shot.
- Test passes under pressure, steals from front/side, pass interceptions, shot-clock turnover and disconnect while holding the ball.
- Run 2, 3, 6 and 10-player matches; confirm equal team assignment and no team with a player advantage greater than one.
- Verify score resets, time expiration, tied overtime, win-by-two regulation, player snapshot restoration, scoreboard cleanup, and back-to-back matches.
- Compare shot feel and court scale with the legacy source before promoting the game to normal rotation.

Hoops Remastered remains disabled from normal rotation until the live checklist is accepted.
