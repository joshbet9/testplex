# Achievement Review and Implementation Progress

This checklist records the approval and implementation pass requested for the
30 game families. The detailed historical inventory remains in
`ACHIEVEMENT-AND-KIT-REVIEW.md`; this file records the live sequential work and
its verification state.

| Game | Current achievement count | Kits covered | Design approved | Implementation | Automated tests | Remaining verification |
| --- | ---: | --- | --- | --- | --- | --- |
| Snap | 20 | Existing kit achievement conditions retained | Yes | Existing catalogue and award paths retained; no code change required in this pass | Not run in this environment | Re-run automated tests; live Paper 26.3 gameplay |
| Sneaky Assassins | 14 | Escape Artist, Ranged Assassin, Briber, Revealer | Yes | Done: restored original entries, added approved detectors, renamed player-facing Assassin kit to Ranged Assassin | Added; execution blocked by unavailable Maven dependency download | Run tests/build; live smoke pursuit and final villager kill acceptance |
| Speed Builders | 14 | No kits | Yes | Done: restored source achievements and wired tie-break, lifetime, and approved build-feat detectors | Added; execution blocked by unavailable Maven dependency download | Run tests/build; live multi-builder round acceptance |
| Spleef | 14 | Snowballer, Brawler, Archer, Gust | Yes | Implemented: restored Demolitionist and 15-win Spleef King/Queen; added five approved challenges, with kit challenges checked against the final elimination sequence | Achievement catalogue and condition tests pass; full module suite has one unrelated Speed Builders failure | Live Paper 26.3 acceptance, especially Wind Charge and Smasher final-elimination attribution |
| Super Sploor | 15 | Existing kit coverage retained | Yes | Reviewed; user approved current achievements and no code changes were requested | Existing Super Sploor catalog tests pass in the full run | Continue to live test only if new issues are reported |
| Turf Wars | 15 | Marksman, Infiltrator, Shredder, Fortifier | Yes | Implemented: restored four source achievements; added Dead Centre (20 Marksman bow eliminations) and Keep Your Fort (30 intact own-turf Fortifier blocks) | Achievement catalogue and condition tests pass; full module suite has one unrelated Speed Builders failure | Live Paper 26.3 acceptance for Fortifier block ownership and source map frontline checks |

The remaining games are still tracked in the historical inventory and will be
reviewed in catalogue order. “Done” above means the implementation work is in
the working branch; it does not mean live gameplay has passed. The current full
module test run compiled the implementation and ran 1,050 tests: 1,049 passed,
with one unrelated-to-this-change Speed Builders debug-round test failure
(`GameDebugEventCatalogTest.speedBuildersPerfectBuildProgressDoesNotPersistInDebugRounds`).
