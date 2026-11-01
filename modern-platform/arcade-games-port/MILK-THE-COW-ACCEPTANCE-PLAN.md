# Milk the Cow controlled-acceptance plan

Milk the Cow has the original BobsFarm archive and a mechanics-first Paper 26.3 runtime. The new archive test checks that the packaged WorldConfig retains the three animal marker groups used by the runtime (WHITE chickens, PINK pigs and PURPLE villagers). This is a source-asset gate, not a replacement for a two-human play test.

## Required gameplay pass

1. Start with two and then several human players. Verify the automatic Farmer/Cow split, role balance, team colours, HUD and initial spawn placement.
2. As a Farmer, fill and drink milk buckets to reach 15 points. Verify milk-bucket fill protection, scoring, health feedback and the Pinger condition while tied with a player who is drinking.
3. As a Cow, verify disguise visibility, hunger/sprint charge, body charge, Cow Bomb, Body Slam, knockback and the Farmer double jump.
4. Eliminate the final Cow and verify a remaining Farmer is promoted. Eliminate all Farmers and verify the surviving Cow team receives the win. Check respawn and death score penalties.
5. Exercise Pure Skill, Pinger, Milk High Club, Perfect Game, Slaughterhouse, Stop Stop He's Already Dead and Got the Chops using their real event triggers.
6. Confirm wins, role wins, kills, assists and buckets digested persist; disconnect/reconnect, repeated rounds and teardown must restore the lobby without leaving herd/disguise entities.

Milk the Cow remains a candidate for the controlled acceptance queue only after the original BobsFarm world loads and the two-human round is accepted on Paper 26.3.
