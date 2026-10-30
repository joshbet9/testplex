# WizardsModern — Paper 26.3

A separate, resource-pack-independent port of the original Mineplex Wizards Arcade survival game. It is built as its own plugin and does not use the legacy 1.8 NMS implementation.

## Resource-pack-free presentation

All spell books, wands, inventory menus and feedback use ordinary Paper/Minecraft materials, item names, lore, boss bars and vanilla particles. There are no custom fonts, custom-model-data dependencies, downloaded assets, forced server packs or resource-pack prompts. The icons are intentionally readable vanilla item choices rather than attempts to reproduce missing texture-pack artwork.

## First implementation milestone

- Standalone /wizards queue/start/leave/kit/status/stop test controls.
- Four source kits: Mage, Sorcerer, Mystic and Witch Doctor.
- Free-for-all, last-wizard-standing round flow in a generated isolated test arena.
- Loot chests with food, armour, wands and spellbooks; spellbooks level known spells and convert duplicates at max level into mana.
- Mana regeneration, Soul Star boosts, wand slots, per-spell cooldowns and an inventory spell selector.
- Ten initial cast implementations: Mana Bolt, Fireball, Flash, Frost Barrier, Gust, Heal, Ice Prison, Lightning Strike, Speed Boost and Wizard's Compass.
- All 21 historical spell definitions are catalogued. The other eleven are deliberately marked as not yet implemented and are not put into live loot until their effects are ported.

The repository currently has no Wizards map archive. This module therefore creates a clearly-labelled generated arena for mechanics testing; it does not claim the original map layout or source map chest placements.

## Test commands

- /wizards join — queue for the next round.
- /wizards kit <mage|sorcerer|mystic|witchdoctor> — choose a kit before starting.
- /wizards start — start the queued players (solo start is enabled in the default test config).
- /wizards status — inspect queue and round state.
- /wizards leave — leave the queue or active round and restore the saved player state.
- /wizards stop — operator reset.

Right-click a wand to open the spell selector. Left-click a spell in the selector to bind it to that wand; right-click a spell to quickcast it. Left-click with a bound wand to cast. Spellbooks found in chests are consumed to learn or level the spell.

## Status and outstanding parity

This is a mechanics-in-progress test port, not READY_FOR_TEST and not a 100% parity claim. The initial milestone still needs live multi-client acceptance and further source comparison. Outstanding work includes the remaining eleven spell effects (Anvil Drop, Ice Shards, Implode, Napalm, Rainbow Beam, Rainbow Road, Rumble, Spectral Arrow, Summon Wolves, Trap Rune and Web Shot), source-accurate endgame meteor/lightning pressure, complete loot weighting, map parity, damage/armour balancing, and repeated disconnect/reconnect/round-cleanup validation.
