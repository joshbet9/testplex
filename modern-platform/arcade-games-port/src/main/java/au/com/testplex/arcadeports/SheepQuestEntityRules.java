package au.com.testplex.arcadeports;

import org.bukkit.entity.Sheep;

final class SheepQuestEntityRules {
    private SheepQuestEntityRules() { }

    static void hideNameTag(Sheep sheep) {
        sheep.setCustomName(null);
        sheep.setCustomNameVisible(false);
    }
}
