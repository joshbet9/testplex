# Camera Shy — Modern 26.3 test plan

## Join and controls
- `/arcadeport play camera-shy` queues the player and opens the short join window.
- `/arcadeport join camera-shy` queues without starting the quick-start timer.
- `/arcadeport lobby camera-shy` selects the game in the shared lobby and refreshes its kit selector.
- `/arcadeport start camera-shy` starts the current lobby participants.
- `/arcadeport kit camera-shy-smoke-artist|camera-shy-decoy|camera-shy-runner|camera-shy-spotter|camera-shy-understudy` selects a persistent kit; the lobby kit GUI offers the same choices.
- Hold right-click with the vanilla spyglass to aim; press F (swap hands) to take a photograph.
- `/arcadeport leave` leaves the waiting queue; `/arcadeport status` reports queue and match state.

## Gameplay acceptance
- A photograph requires a living opponent, a clear ray-traced line of sight, a target within 24 blocks, and a ready camera.
- One successful photograph adds exactly one exposure; the fifth exposure eliminates the target.
- Exposure is permanent for the match. A photographed player receives 1.25 seconds of exposure protection.
- Each photographer has a 1.5 second shutter cooldown (1.0 second during Final Snapshot).
- A flash and nearby shutter cue reveal the photographer's approximate location.
- Photographed players enter spectator mode; game end restores human players' saved inventory, location, mode, effects and movement settings.
- The match runs for three minutes. The last remaining human wins; a solo human test session does not end immediately just because the fixture has one human and AFK targets.
- Timeout winner selection is deterministic: lowest exposure count, then most photographs, then most eliminations.

## Kit acceptance
- Smoke Artist: smoke sphere blocks camera rays; encounter escape achievement counts three distinct escapes.
- Decoy: fake shutter/flash fools three distinct human opponents.
- Runner: Speed II for three seconds; shutter is disabled during the burst; a successful escape within the follow-up window unlocks Catch Me If You Can.
- Spotter: only reveals opponents within 18 blocks and clear line of sight; marks last 15 seconds and must lead to three distinct photographs.
- Understudy: deploys a player-skin stand-in; photographing the stand-in awards Method Actor and does not add exposure.

## Match mechanics
- Group Photo pickup grants one charged shutter that can photograph a second target in the same narrow camera cone. The charge is retained if a valid second target is not present.
- Director's Spotlight periodically marks one living human player.
- Final Snapshot begins with 30 seconds remaining and shortens shutter cooldown.
- Smoke, fake flashes, decoys and pickup entities are removed at match end.

## Progression acceptance
- All 20 IDs in `CameraShyProgressCatalog` have a unique name and condition.
- The three lifetime counters are wins (250), eliminations (500) and successful photographs (1,000).
- Match-scoped achievements reset each round; lifetime milestones and consecutive-win tracking persist in local Testplex config.
- AFK Test Player objects never receive human progression records.

## Player progression and stats
- `/stats` or `/stats camera-shy` shows the viewer's Camera Shy lifetime record.
- `/stats camera-shy <1-4>` pages through all 20 achievements; every ID appears exactly once.
- `/stats <online-player> camera-shy [page]` can inspect another online test participant's record.
- Each lifetime milestone displays current progress toward 250 wins, 500 eliminations or 1,000 photographs and marks completed thresholds.

## Regression checks
- Starting Camera Shy while another Arcade game is running is rejected.
- Starting another Arcade game while Camera Shy is live is rejected.
- Operator stop and plugin shutdown restore player state and clean up the test-player roster.
- `mvn -pl modern-platform/arcade-games-port -am test` and the modern platform tests pass.
