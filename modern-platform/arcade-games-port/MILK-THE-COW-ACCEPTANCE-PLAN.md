# Milk the Cow controlled-acceptance plan

Milk the Cow is now READY_FOR_TEST in the shared controlled queue. It uses the original BobsFarm archive and a mechanics-first Paper 26.3 runtime. The archive test checks that the packaged WorldConfig retains the three animal marker groups used by the runtime (WHITE chickens, PINK pigs and PURPLE villagers). This source-asset gate does not replace a two-human play test.

## Required gameplay pass

1. Start with two and then several human players. Verify the automatic Farmer/Cow split, role balance, team colours, HUD and initial spawn placement.
2. As a Farmer, fill and drink milk buckets to reach 15 points. Verify milk-bucket fill protection, scoring, health feedback and the Pinger condition while tied with a player who is drinking.
3. As a Cow, verify disguise visibility, hunger/sprint charge, body charge, Cow Bomb, Body Slam, knockback and the Farmer double jump.
4. Eliminate the final Cow and verify a remaining Farmer is promoted. Eliminate all Farmers and verify the surviving Cow team receives the win. Check respawn and death score penalties.
5. Exercise Pure Skill, Pinger, Milk High Club, Perfect Game, Slaughterhouse, Stop Stop He's Already Dead and Got the Chops using their real event triggers.
6. Confirm wins, role wins, kills, assists and buckets digested persist; disconnect/reconnect, repeated rounds and teardown must restore the lobby without leaving herd/disguise entities.

Milk the Cow is available for controlled testing after the packaged BobsFarm archive passes CI. The two-human and multi-human round must still be exercised on Paper 26.3 before declaring live gameplay acceptance.

## Achievement verification

- [ ] Preserve all seven existing achievements and their current detectors: Pure Skill, Pinger, Milk High Club, Perfect Game, Slaughterhouse, Stop Stop He's Already Dead and Got the Chops.
- [ ] The Last Drop is awarded immediately for the winning milk drink when the Farmer has at most one heart before drinking.
- [ ] Whole Herd requires a winning Farmer to have obtained milk from three distinct Cow players.
- [ ] Udderly Explosive requires one Cow Bomb to hit three distinct Farmers and directly eliminate at least one.
- [ ] Seismic Stampede requires one Body Slam to hit three distinct Farmers and directly eliminate at least one.
- [ ] The Full Moo requires the same Cow to hit one Farmer with Cow Bomb, Body Slam and Cow Charge, then receive elimination credit against that Farmer.
- [ ] Stampede! requires one uninterrupted sprint-charge sequence to hit five distinct Farmers and earn three credited eliminations.
- [ ] Spilt Milk requires three separate Cow Charge events to empty milk buckets while the Farmer is one point below the win target, followed by losing the naturally completed game.
- [ ] The catalogue contains exactly 14 distinct achievement IDs; existing milestone thresholds remain unchanged.
- [ ] Bot progression and debug-round achievements are not persisted.

Automated tests cover the new achievement conditions and negative cases, catalog count and IDs, and thresholds. Paper 26.3 acceptance remains required for real event ordering, charge-sequence boundaries, multi-target ability behavior, score flow and persistence.
