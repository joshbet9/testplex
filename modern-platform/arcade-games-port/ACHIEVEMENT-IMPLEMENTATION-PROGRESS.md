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
| Spleef | 7 | Pending review | No | Not started | Not run | Awaiting design review and approval |
| Super Sploor | 15 | Pending review | No | Not started | Not run | Awaiting design review and approval |
| Turf Wars | 9 | Pending review | No | Not started | Not run | Awaiting design review and approval |

The remaining games are still tracked in the historical inventory and will be
reviewed in catalogue order. “Done” above means the implementation work is in
the working branch; it does not mean tests or live gameplay have passed.
