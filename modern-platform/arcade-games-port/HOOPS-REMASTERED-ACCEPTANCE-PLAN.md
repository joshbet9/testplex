# Hoops Remastered — Paper 26.3 acceptance plan

## Match format
- 2–16 players (up to 8v8), balanced RED / BLUE teams, one isolated court per session.
- Match ends when one team reaches 21 points, or when five minutes elapse. First to 21 wins immediately (no win-by-two). If the five-minute buzzer finds the score tied, the match enters sudden-death overtime and the next basket wins.
- 24-second shot clock. Two points inside the three-point arc and three points beyond it.

## Controls and gameplay
- Double-tap jump to use Hops, a directional double-jump with an eight-second recharge.
- Left-click with possession to pass toward a teammate in the player's forward passing lane; an unmarked pass travels forward and remains catchable.
- Right-click with possession to shoot toward the crosshair. The server owns the ball trajectory and validates a descending rim crossing; aiming assistance is limited to a small rim tolerance.
- A grounded, aimed close-range attempt becomes a layup. Jump toward the rim and shoot while rising from close range to attempt a two-point dunk.
- Left-click an opposing carrier in close range and while facing them to attempt a deterministic steal (with a per-player cooldown).
- Right-click near an airborne opposing shot while facing the ball to attempt a timed block (with a per-player cooldown). Loose balls can be collected; missed shots and blocks remain recoverable.
- Assists are credited when a teammate scores within eight seconds of a completed pass.

## Progress and presentation
- Native Paper boss bar: live score, regulation clock and shot clock; overtime changes its title and colour and uses the possession clock for its progress.
- Sidebar: RED / BLUE scores, regulation or overtime state, shot clock, possession and no-win-by-two rule.
- Floating in-world TextDisplay scoreboard above centre court, updated with score and both clocks.
- Team-coloured name tags and tab-list entries through the match scoreboard teams.
- Action bar: current player points, assists, rebounds, blocks, steals and field-goal percentage.
- Final chat results: winning score, winning-team MVP and a top-five box-score summary.
- Local lifetime totals: games, wins, losses, points, assists, rebounds, blocks, steals, shots attempted/made and PPG/APG/RPG.
- Court presentation uses only vanilla blocks, Paper's built-in BossBar/TextDisplay APIs, built-in particles and sounds. No resource pack or extra runtime plugin is required.
- Hoops Remastered achievements use the existing progress catalogue; career milestones remain in its existing wins/points/assists/rebounds/steals/blocks catalogue.

## Automated validation
- HoopsRemasteredRulesTest covers lobby sizes, two/three-point classification, crosshair tolerance, layup/dunk eligibility, first-to-21 without win-by-two, five-minute leader resolution, tied-score sudden-death overtime, block cooldown, clutch/buzzer windows, assist expiry, box-score awards, comeback threshold, spawn assignment and clock formatting.
- The lobby accepts 2–16 participants only; team assignment remains 1–8 players per side.
- Maven compile/test must pass on Paper 26.3 / Java 25.

## Live acceptance still required
- Confirm the enclosed court, seating, lighting, centre display scoreboard and hoop geometry look correct from both ends.
- Verify bossbar, sidebar, floating scoreboard, team-coloured names and tab-list entries stay in sync for every player.
- Test crosshair shooting at several angles, layups, rising-jump dunks, both hoops, two/three-point line boundary, missed-shot rebound and blocked shot.
- Test passes under pressure, teammate catches, missed pass recovery, facing-dependent steals, timed/facing-dependent blocks, shot-clock turnover, inventory/drop protection and disconnect while holding the ball.
- Run 2, 3, 6, 8, 10, 12, 14 and 16-player matches; confirm equal team assignment, eight unique spawn lanes per side, and no team with a player advantage greater than one.
- Specifically test a full 8v8 match for readable player spacing, pass target selection, interceptions, rebound races, steals and blocks.
- Verify score resets, time expiration, a tied five-minute transition to sudden death, first-to-21 termination, next-basket overtime termination, player snapshot restoration, scoreboard cleanup, and back-to-back matches.
- Compare shot feel and court scale with the legacy source before promoting the game to normal rotation.

Hoops Remastered uses only the target server's built-in Paper API and vanilla Minecraft assets. It requires no resource pack, PacketEvents, ProtocolLib or other extra runtime plugin.

Hoops Remastered remains disabled from normal rotation until the live checklist is accepted.
