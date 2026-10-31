package au.com.testplex.wizards;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum WizardsSpell {
    ANVIL_DROP("anvil-drop", "Anvil Drop", Category.ATTACK, Material.ANVIL, 3, 40, 15, -3, -4, 10, true),
    FIREBALL("fireball", "Fireball", Category.ATTACK, Material.FIRE_CHARGE, 3, 30, 15, -3, -2, 10, true),
    FLASH("flash", "Flash", Category.SUPPORT, Material.ENDER_PEARL, 3, 20, 50, 0, -5, 3, true),
    FROST_BARRIER("frost-barrier", "Frost Barrier", Category.MISC, Material.PACKED_ICE, 3, 10, 20, 0, -5, 3, true),
    GUST("gust", "Gust", Category.MISC, Material.FEATHER, 3, 15, 20, 0, 0, 5, true),
    HEAL("heal", "Heal", Category.SUPPORT, Material.GOLDEN_APPLE, 5, 50, 30, 0, -1, 5, true),
    ICE_PRISON("ice-prison", "Ice Prison", Category.MISC, Material.PRISMARINE_CRYSTALS, 3, 25, 20, 2, 0, 3, true),
    ICE_SHARDS("ice-shards", "Ice Shards", Category.ATTACK, Material.POINTED_DRIPSTONE, 5, 30, 20, 0, -2, 3, true),
    IMPLODE("implode", "Implode", Category.MISC, Material.GLOWSTONE_DUST, 3, 50, 30, -2, -3, 3, true),
    LIGHTNING_STRIKE("lightning-strike", "Lightning Strike", Category.ATTACK, Material.LIGHTNING_ROD, 3, 50, 20, 0, 0, 10, true),
    MANA_BOLT("mana-bolt", "Mana Bolt", Category.ATTACK, Material.AMETHYST_SHARD, 3, 15, 5, -2, 0, 15, true),
    NAPALM("napalm", "Napalm", Category.ATTACK, Material.BLAZE_POWDER, 5, 60, 60, 5, -10, 1, true),
    RAINBOW_BEAM("rainbow-beam", "Rainbow Beam", Category.ATTACK, Material.PRISMARINE_SHARD, 5, 5, 8, 3, 1, 10, true),
    RAINBOW_ROAD("rainbow-road", "Rainbow Road", Category.MISC, Material.PRISMARINE, 3, 10, 20, 0, 0, 3, true),
    RUMBLE("rumble", "Rumble", Category.ATTACK, Material.TNT, 5, 30, 15, 0, -1, 10, true),
    SPECTRAL_ARROW("spectral-arrow", "Spectral Arrow", Category.ATTACK, Material.SPECTRAL_ARROW, 3, 40, 15, -5, -2, 3, true),
    SPEED_BOOST("speed-boost", "Speed Boost", Category.SUPPORT, Material.SUGAR, 2, 20, 40, 0, 0, 3, true),
    SUMMON_WOLVES("summon-wolves", "Summon Wolves", Category.ATTACK, Material.BONE, 3, 80, 160, -10, 0, 8, true),
    TRAP_RUNE("trap-rune", "Trap Rune", Category.MISC, Material.TRIPWIRE_HOOK, 3, 25, 30, 0, -5, 3, true),
    WEB_SHOT("web-shot", "Web Shot", Category.ATTACK, Material.COBWEB, 3, 40, 20, -5, 0, 3, true),
    WIZARDS_COMPASS("wizards-compass", "Wizard's Compass", Category.SUPPORT, Material.COMPASS, 1, 5, 4, 0, 0, 1, true);

    public enum Category { ATTACK, SUPPORT, MISC }

    private final String id;
    private final String displayName;
    private final Category category;
    private final Material icon;
    private final int maximumLevel;
    private final int baseManaCost;
    private final int baseCooldown;
    private final int manaPerLevel;
    private final int cooldownPerLevel;
    private final int lootWeight;
    private final boolean implemented;

    WizardsSpell(String id, String displayName, Category category, Material icon, int maximumLevel,
                 int baseManaCost, int baseCooldown, int manaPerLevel, int cooldownPerLevel,
                 int lootWeight, boolean implemented) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.icon = icon;
        this.maximumLevel = maximumLevel;
        this.baseManaCost = baseManaCost;
        this.baseCooldown = baseCooldown;
        this.manaPerLevel = manaPerLevel;
        this.cooldownPerLevel = cooldownPerLevel;
        this.lootWeight = lootWeight;
        this.implemented = implemented;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public Category category() { return category; }
    public Material icon() { return icon; }
    public int maximumLevel() { return maximumLevel; }
    public int baseManaCost() { return baseManaCost; }
    public int baseCooldown() { return baseCooldown; }
    public int lootWeight() { return lootWeight; }
    public boolean implemented() { return implemented; }

    public int manaCost(int level) {
        return manaCost(level, 1.0D);
    }

    public int manaCost(int level, double modifier) {
        int raw = baseManaCost + manaPerLevel * Math.max(0, level - 1);
        return Math.max(0, (int) Math.round(raw * Math.max(0.0D, modifier)));
    }

    public int cooldownSeconds(int level, double modifier) {
        int raw = baseCooldown + cooldownPerLevel * Math.max(0, level - 1);
        double scale = (this == HEAL || this == RAINBOW_ROAD) ? 1.0D : Math.max(0.0D, modifier);
        return Math.max(0, (int) Math.round(raw * scale));
    }

    public static WizardsSpell fromId(String id) {
        if (id == null) return null;
        String normalized = id.toLowerCase(Locale.ROOT).replace("'", "").replace('_', '-').replace(' ', '-');
        return Arrays.stream(values()).filter(spell -> spell.id.equals(normalized)).findFirst().orElse(null);
    }

    public static List<WizardsSpell> implementedSpells() {
        return Arrays.stream(values()).filter(WizardsSpell::implemented).toList();
    }
}
