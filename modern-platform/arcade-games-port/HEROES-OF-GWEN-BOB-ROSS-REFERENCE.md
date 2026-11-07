# Heroes of GWEN: Bob Ross source reference

Bob Ross is intentionally unavailable in the modern Heroes of GWEN lobby picker and runtime until his kit is ported. Do not re-enable his selection by adding only a catalogue entry: the current runtime has no Bob Ross loadout or ability consumers.

Keep this source kit as the implementation reference for a future update:

- Hero definition and Mage role: `Plugins/Nautilus.Game.Arcade/src/nautilus/game/arcade/game/games/moba/kit/bob/HeroBob.java`
- Paint: `Plugins/Nautilus.Game.Arcade/src/nautilus/game/arcade/game/games/moba/kit/bob/SkillPaint.java`
- Happy Trees: `Plugins/Nautilus.Game.Arcade/src/nautilus/game/arcade/game/games/moba/kit/bob/SkillHappyTrees.java`
- Beat The Devil Out Of It: `Plugins/Nautilus.Game.Arcade/src/nautilus/game/arcade/game/games/moba/kit/bob/SkillBeatTheDevil.java`
- Build Painting: `Plugins/Nautilus.Game.Arcade/src/nautilus/game/arcade/game/games/moba/kit/bob/SkillBuildPainting.java`

When the abilities and their cleanup/lifecycle behaviour are implemented and tested, review whether Bob Ross should return as a playable hero and add a dedicated ability achievement at that time.
