package au.com.testplex.arcadeports;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Discovers the game's primitive rule constants for the profile editor. */
final class GameDebugVariableCatalog {
    enum Kind { INTEGER, LONG, FLOAT, DOUBLE, BOOLEAN }

    record Variable(String key, String label, Kind kind, double defaultValue,
                    double minimum, double maximum, double step) { }

    private static final Set<String> LIVE_BOOLEAN_RULES = Set.of(
            "BOMB_LOBBERS_POWERUP_SPAWNING_ENABLED",
            "BACON_BRAWL_HITS_DAMAGE_HEALTH",
            "DRAGONS_SPAWNING_ENABLED",
            "DRAGONS_AUTONOMOUS_MOVEMENT_ENABLED",
            "MICRO_BATTLES_CRUMBLE_ENABLED",
            "QUIVER_LEAPER_DOUBLE_JUMP_ENABLED",
            "SPLEEF_HUNGER_DRAIN_ENABLED",
            "SPEED_BUILDERS_DOUBLE_JUMP_ENABLED",
            "SPEED_BUILDERS_PERFECT_BUILD_TIME_TIEBREAK_ENABLED");
    private static final Set<String> LIVE_SUPER_SPLOOR_BOOLEAN_RULES = Set.of(
            "SUPER_SPLOOR_SQUID_MODE_ENABLED", "SUPER_SPLOOR_INK_REGEN_ENABLED",
            "SUPER_SPLOOR_SECONDARIES_ENABLED", "SUPER_SPLOOR_SPECIALS_ENABLED");
    private static final Set<String> LIVE_SNEAKY_NUMERIC_RULES = Set.of(
            "SNEAKY_ASSASSINS_POWERUP_KILL_WINDOW_MILLIS", "SNEAKY_ASSASSINS_NEXT_POWERUP_DELAY_MILLIS",
            "SNEAKY_ASSASSINS_MAX_ACTIVE_POWERUPS", "SNEAKY_ASSASSINS_MASTER_ASSASSIN_POWERUPS_REQUIRED",
            "SNEAKY_ASSASSINS_REVEAL_RADIUS_BLOCKS", "SNEAKY_ASSASSINS_SMOKE_RADIUS_BLOCKS",
            "SNEAKY_ASSASSINS_GLOBAL_REVEAL_CYCLE_MILLIS", "SNEAKY_ASSASSINS_REVEALER_DURATION_MILLIS",
            "SNEAKY_ASSASSINS_SMOKE_CLOAK_DURATION_MILLIS", "SNEAKY_ASSASSINS_BRIBE_COOLDOWN_MILLIS",
            "SNEAKY_ASSASSINS_NPC_CULL_INTERVAL_MILLIS", "SNEAKY_ASSASSINS_NPC_PATROL_ANCHOR_DISTANCE_BLOCKS",
            "SNEAKY_ASSASSINS_NPC_PATROL_ORDER_CHANCE_DENOMINATOR", "SNEAKY_ASSASSINS_NPC_STUCK_RECOVERY_MILLIS",
            "SNEAKY_ASSASSINS_MAX_POWERUP_PROGRESS", "SNEAKY_ASSASSINS_GLOBAL_REVEAL_DURATION_MILLIS",
            "SNEAKY_ASSASSINS_SMOKE_BLINDNESS_DURATION_MILLIS", "SNEAKY_ASSASSINS_POWERUP_PICKUP_RADIUS_BLOCKS",
            "SNEAKY_ASSASSINS_WOOD_SWORD_DAMAGE", "SNEAKY_ASSASSINS_STONE_SWORD_DAMAGE",
            "SNEAKY_ASSASSINS_GOLD_SWORD_DAMAGE", "SNEAKY_ASSASSINS_IRON_SWORD_DAMAGE",
            "SNEAKY_ASSASSINS_DIAMOND_SWORD_DAMAGE", "SNEAKY_ASSASSINS_PROJECTILE_DAMAGE_MIN",
            "SNEAKY_ASSASSINS_PROJECTILE_DAMAGE_PER_SPEED", "SNEAKY_ASSASSINS_POWERUP_BONUS_HEALTH",
            "SNEAKY_ASSASSINS_POWERUP_MAX_HEALTH", "SNEAKY_ASSASSINS_KILL_BONUS_HEALTH",
            "SNEAKY_ASSASSINS_MASTERS_MASTER_POWERUPS_REQUIRED", "SNEAKY_ASSASSINS_NPC_ATTACK_COOLDOWN_MILLIS",
            "SNEAKY_ASSASSINS_NPC_ATTACK_RANGE_BLOCKS", "SNEAKY_ASSASSINS_NPC_ATTACK_DAMAGE",
            "SNEAKY_ASSASSINS_NPC_BRIBE_MOVE_SPEED", "SNEAKY_ASSASSINS_NPC_PATROL_MOVE_SPEED",
            "SNEAKY_ASSASSINS_NPC_RECOVERY_HORIZONTAL", "SNEAKY_ASSASSINS_NPC_RECOVERY_VERTICAL",
            "SNEAKY_ASSASSINS_NPC_MOTION_FACTOR", "SNEAKY_ASSASSINS_NPC_MIN_VERTICAL_SPEED",
            "SNEAKY_ASSASSINS_NPC_MAX_VERTICAL_SPEED", "SNEAKY_ASSASSINS_REVEALER_PROJECTILE_LIFETIME_TICKS",
            "SNEAKY_ASSASSINS_REVEALER_PROJECTILE_SPEED", "SNEAKY_ASSASSINS_REVEALER_PROJECTILE_VERTICAL_SPEED",
            "SNEAKY_ASSASSINS_NPC_PATROL_WAYPOINT_ATTEMPTS", "SNEAKY_ASSASSINS_NPC_SPAWN_SEARCH_ATTEMPTS",
            "SNEAKY_ASSASSINS_PROSECUTOR_DISTINCT_TARGETS_REQUIRED", "SNEAKY_ASSASSINS_DUELS_OTHER_MASTERS_REQUIRED",
            "SNEAKY_ASSASSINS_POWERUP_SMOKE_BOMBS", "SNEAKY_ASSASSINS_KILL_BONUS_SMOKE_BOMBS");
    private static final Set<String> LIVE_CAMERA_NUMERIC_RULES = Set.of(
            "CAMERA_SHY_CAMERA_RANGE_BLOCKS", "CAMERA_SHY_CLOSE_PHOTO_BLOCKS", "CAMERA_SHY_LONG_PHOTO_BLOCKS",
            "CAMERA_SHY_PHOTO_COOLDOWN_MILLIS", "CAMERA_SHY_FINAL_SNAPSHOT_COOLDOWN_MILLIS",
            "CAMERA_SHY_EXPOSURE_PROTECTION_MILLIS", "CAMERA_SHY_DOUBLE_TAKE_WINDOW_MILLIS",
            "CAMERA_SHY_DOUBLE_ELIMINATION_WINDOW_MILLIS", "CAMERA_SHY_PHOTO_LENS_MIN_DOT",
            "CAMERA_SHY_SPOTLIGHT_INTERVAL_MILLIS", "CAMERA_SHY_SPOTLIGHT_GLOW_DURATION_MILLIS",
            "CAMERA_SHY_DECOY_FLASH_LIFETIME_MILLIS", "CAMERA_SHY_SMOKE_LIFETIME_MILLIS",
            "CAMERA_SHY_SMOKE_ESCAPE_CREDIT_WINDOW_MILLIS", "CAMERA_SHY_PURSUIT_ESCAPE_MILLIS",
            "CAMERA_SHY_PURSUIT_ESCAPE_DISTANCE_BLOCKS", "CAMERA_SHY_SPOTTER_RANGE_BLOCKS",
            "CAMERA_SHY_SMOKE_ABILITY_COOLDOWN_MILLIS", "CAMERA_SHY_DECOY_ABILITY_COOLDOWN_MILLIS",
            "CAMERA_SHY_RUNNER_ABILITY_COOLDOWN_MILLIS", "CAMERA_SHY_SPOTTER_ABILITY_COOLDOWN_MILLIS",
            "CAMERA_SHY_UNDERSTUDY_ABILITY_COOLDOWN_MILLIS", "CAMERA_SHY_RUNNER_ABILITY_DURATION_MILLIS",
            "CAMERA_SHY_RUNNER_SPEED_AMPLIFIER", "CAMERA_SHY_RUNNER_ACHIEVEMENT_WINDOW_MILLIS",
            "CAMERA_SHY_SPOTTER_MARK_DURATION_MILLIS", "CAMERA_SHY_DECOY_FORWARD_OFFSET_BLOCKS",
            "CAMERA_SHY_UNDERSTUDY_FORWARD_OFFSET_BLOCKS", "CAMERA_SHY_DECOY_APPROACH_RANGE_BLOCKS",
            "CAMERA_SHY_DECOY_LOOK_RANGE_BLOCKS", "CAMERA_SHY_DECOY_LOOK_MIN_DOT",
            "CAMERA_SHY_SMOKE_OCCLUSION_RADIUS_BLOCKS", "CAMERA_SHY_OUT_OF_FOCUS_WINDOW_TICKS",
            "CAMERA_SHY_OUT_OF_FOCUS_MOVEMENT_DISTANCE_BLOCKS", "CAMERA_SHY_GROUP_PHOTO_MIN_TARGETS",
            "CAMERA_SHY_PAPARAZZI_UNIQUE_PHOTOS_REQUIRED", "CAMERA_SHY_PRIVATE_INVESTIGATOR_TARGETS_REQUIRED",
            "CAMERA_SHY_FALSE_ALARM_TARGETS_REQUIRED", "CAMERA_SHY_ESCAPE_STREAK_REQUIRED",
            "CAMERA_SHY_SMOKE_ESCAPE_STREAK_REQUIRED", "CAMERA_SHY_DOUBLE_ELIMINATION_STREAK_REQUIRED",
            "CAMERA_SHY_REVENGE_KILLS_REQUIRED", "CAMERA_SHY_UNTOUCHABLE_WINS_REQUIRED",
            "CAMERA_SHY_NARROW_ESCAPE_EXPOSURES", "CAMERA_SHY_PICTURE_PERFECT_PHOTOS_REQUIRED",
            "CAMERA_SHY_STAGE_FRIGHT_EXPOSURES", "CAMERA_SHY_STAGE_FRIGHT_DURATION_MILLIS");
    private static final Set<String> LIVE_SPLEEF_NUMERIC_RULES = Set.of(
            "SPLEEF_HUNGER_DRAIN_INTERVAL_MILLIS", "SPLEEF_HUNGER_DRAIN_PER_INTERVAL",
            "SPLEEF_HUNGER_RESTORE_COOLDOWN_MILLIS", "SPLEEF_HUNGER_RESTORE_PER_HIT",
            "SPLEEF_ARCHER_ARROW_INTERVAL_MILLIS", "SPLEEF_ARCHER_MAX_ARROWS",
            "SPLEEF_ARCHER_ARROW_SPEED_MULTIPLIER", "SPLEEF_SNOWBALLER_MAX_SNOWBALLS",
            "SPLEEF_SNOWBALL_SPEED_MULTIPLIER", "SPLEEF_BRAWLER_LEAP_COOLDOWN_MILLIS",
            "SPLEEF_BRAWLER_KNOCKBACK", "SPLEEF_DEFAULT_KNOCKBACK", "SPLEEF_KNOCKBACK_COOLDOWN_MILLIS",
            "SPLEEF_KNOCKBACK_VERTICAL_BASE", "SPLEEF_KNOCKBACK_GROUND_BONUS",
            "SPLEEF_KNOCKBACK_VERTICAL_CAP", "SPLEEF_GUST_COOLDOWN_MILLIS", "SPLEEF_GUST_SPEED_MULTIPLIER",
            "SPLEEF_SMASHER_COOLDOWN_MILLIS", "SPLEEF_SMASHER_RADIUS_BLOCKS",
            "SPLEEF_LEAP_FORWARD_POWER", "SPLEEF_LEAP_VERTICAL_ADD", "SPLEEF_LEAP_VERTICAL_CAP",
            "SPLEEF_LEAP_GROUND_BONUS", "SPLEEF_ARROW_CLEAR_RADIUS_BASE_BLOCKS",
            "SPLEEF_ARROW_CLEAR_RADIUS_SPEED_DIVISOR", "SPLEEF_ARROW_CLEAR_RADIUS_CAP_BLOCKS",
            "SPLEEF_PROJECTILE_CLEAR_RADIUS_MIN_BLOCKS", "SPLEEF_PROJECTILE_CLEAR_RADIUS_MAX_BLOCKS",
            "SPLEEF_PROJECTILE_IMPACT_SEARCH_RADIUS_BLOCKS");
    private static final Set<String> LIVE_SUPER_SPLOOR_NUMERIC_RULES = Set.of(
            "SUPER_SPLOOR_MAX_INK", "SUPER_SPLOOR_INK_PER_PRIMARY_SHOT", "SUPER_SPLOOR_PRIMARY_SHOT_INTERVAL_MILLIS",
            "SUPER_SPLOOR_ROLLER_PRIMARY_INK_COST", "SUPER_SPLOOR_ROLLER_PRIMARY_INTERVAL_MILLIS",
            "SUPER_SPLOOR_CHARGER_PRIMARY_INK_COST", "SUPER_SPLOOR_CHARGER_PRIMARY_INTERVAL_MILLIS",
            "SUPER_SPLOOR_LOBBER_PRIMARY_INK_COST", "SUPER_SPLOOR_LOBBER_PRIMARY_INTERVAL_MILLIS",
            "SUPER_SPLOOR_DUALIES_PRIMARY_INK_COST", "SUPER_SPLOOR_DUALIES_PRIMARY_INTERVAL_MILLIS",
            "SUPER_SPLOOR_SPRAYER_SECONDARY_INK_COST", "SUPER_SPLOOR_ROLLER_SECONDARY_INK_COST",
            "SUPER_SPLOOR_CHARGER_SECONDARY_INK_COST", "SUPER_SPLOOR_LOBBER_SECONDARY_INK_COST",
            "SUPER_SPLOOR_DUALIES_SECONDARY_INK_COST", "SUPER_SPLOOR_SECONDARY_COOLDOWN_MILLIS",
            "SUPER_SPLOOR_MARK_RECOVERY_DELAY_MILLIS", "SUPER_SPLOOR_MARK_RECOVERY_INTERVAL_MILLIS",
            "SUPER_SPLOOR_MAX_PAINT_MARKS", "SUPER_SPLOOR_NORMAL_INK_REGEN_PER_SECOND",
            "SUPER_SPLOOR_SQUID_INK_REGEN_PER_SECOND", "SUPER_SPLOOR_INK_REGEN_INTERVAL_MILLIS",
            "SUPER_SPLOOR_SQUID_WALK_SPEED", "SUPER_SPLOOR_SQUID_CLIMB_VELOCITY",
            "SUPER_SPLOOR_CHARGER_FULL_CHARGE_MILLIS", "SUPER_SPLOOR_CHARGER_RECHARGE_MILLIS",
            "SUPER_SPLOOR_CHARGER_MIN_INK_COST", "SUPER_SPLOOR_CHARGER_MAX_INK_COST",
            "SUPER_SPLOOR_CHARGER_MIN_SPEED", "SUPER_SPLOOR_CHARGER_MAX_SPEED",
            "SUPER_SPLOOR_DODGE_ROLL_MAX_CHARGES", "SUPER_SPLOOR_DODGE_ROLL_RECHARGE_MILLIS",
            "SUPER_SPLOOR_SPECIAL_CHARGE_PER_TERRITORY_UNIT", "SUPER_SPLOOR_SPECIAL_CHARGE_PER_SPLAT",
            "SUPER_SPLOOR_SPECIAL_CHARGE_REQUIRED", "SUPER_SPLOOR_SPAWN_PROTECTION_MILLIS",
            "SUPER_SPLOOR_SPLAT_RESPAWN_MILLIS", "SUPER_SPLOOR_ROLLER_MINE_ARM_MILLIS",
            "SUPER_SPLOOR_ROLLER_MINE_LIFETIME_MILLIS", "SUPER_SPLOOR_ROLLER_MINE_TRIGGER_RADIUS_BLOCKS",
            "SUPER_SPLOOR_ROLLER_MINE_PAINT_RADIUS_BLOCKS", "SUPER_SPLOOR_ROLLER_STAMPEDE_DURATION_MILLIS",
            "SUPER_SPLOOR_ROLLER_STAMPEDE_PAINT_INTERVAL_MILLIS", "SUPER_SPLOOR_ROLLER_STAMPEDE_SPEED",
            "SUPER_SPLOOR_ROLLER_STAMPEDE_HIT_REACH_BLOCKS", "SUPER_SPLOOR_ROLLER_STAMPEDE_HIT_WIDTH_BLOCKS",
            "SUPER_SPLOOR_CHARGER_TRACKER_DURATION_MILLIS", "SUPER_SPLOOR_CHARGER_TRACKER_PULSE_MILLIS",
            "SUPER_SPLOOR_CHARGER_TRACKER_RADIUS_BLOCKS", "SUPER_SPLOOR_CHARGER_PRISM_BEAM_STEPS",
            "SUPER_SPLOOR_CHARGER_PRISM_BEAM_STEP_BLOCKS", "SUPER_SPLOOR_CHARGER_PRISM_BEAM_HIT_RADIUS_BLOCKS",
            "SUPER_SPLOOR_CHARGER_FULL_SHOT_COOLDOWN_MILLIS", "SUPER_SPLOOR_LOBBER_SPRINKLER_DURATION_MILLIS",
            "SUPER_SPLOOR_LOBBER_SPRINKLER_PULSE_MILLIS", "SUPER_SPLOOR_LOBBER_SPRINKLER_PAINT_RADIUS_BLOCKS",
            "SUPER_SPLOOR_LOBBER_SPRINKLER_HIT_RADIUS_BLOCKS", "SUPER_SPLOOR_LOBBER_PAINTSTORM_DURATION_MILLIS",
            "SUPER_SPLOOR_LOBBER_PAINTSTORM_PULSE_MILLIS", "SUPER_SPLOOR_LOBBER_PAINTSTORM_PULSES",
            "SUPER_SPLOOR_LOBBER_PAINTSTORM_PAINT_RADIUS_BLOCKS", "SUPER_SPLOOR_LOBBER_PAINTSTORM_HIT_RADIUS_BLOCKS",
            "SUPER_SPLOOR_DUALIES_SUCTION_BOMB_FUSE_MILLIS", "SUPER_SPLOOR_DUALIES_SUCTION_BOMB_PAINT_RADIUS_BLOCKS",
            "SUPER_SPLOOR_SPRAYER_BURST_BOMB_PAINT_RADIUS_BLOCKS", "SUPER_SPLOOR_SPRAYER_COLOURBURST_PAINT_RADIUS_BLOCKS",
            "SUPER_SPLOOR_DUALIES_AFTERIMAGE_DASH_INTERVAL_MILLIS", "SUPER_SPLOOR_DUALIES_AFTERIMAGE_DASHES",
            "SUPER_SPLOOR_DUALIES_AFTERIMAGE_DISTANCE_BLOCKS", "SUPER_SPLOOR_DUALIES_AFTERIMAGE_VERTICAL_VELOCITY",
            "SUPER_SPLOOR_OVERTIME_LEAD_PERCENT", "SUPER_SPLOOR_SPRAYER_PRIMARY_PROJECTILE_SPEED",
            "SUPER_SPLOOR_LOBBER_PRIMARY_PROJECTILE_SPEED",
            "SUPER_SPLOOR_LOBBER_PRIMARY_VERTICAL_ARC", "SUPER_SPLOOR_DUALIES_PRIMARY_PROJECTILE_SPEED",
            "SUPER_SPLOOR_DUALIES_PRIMARY_SPREAD_RADIANS", "SUPER_SPLOOR_DUALIES_PRIMARY_PROJECTILES",
            "SUPER_SPLOOR_SPRAYER_PRIMARY_PAINT_RADIUS_BLOCKS", "SUPER_SPLOOR_CHARGER_PRIMARY_PAINT_RADIUS_BLOCKS",
            "SUPER_SPLOOR_LOBBER_PRIMARY_PAINT_RADIUS_BLOCKS", "SUPER_SPLOOR_DUALIES_PRIMARY_PAINT_RADIUS_BLOCKS",
            "SUPER_SPLOOR_ROLLER_PRIMARY_PAINT_RADIUS_BLOCKS", "SUPER_SPLOOR_ROLLER_PRIMARY_PAINT_STEPS",
            "SUPER_SPLOOR_ROLLER_PRIMARY_HIT_REACH_BLOCKS", "SUPER_SPLOOR_ROLLER_PRIMARY_HIT_WIDTH_BLOCKS");

    private GameDebugVariableCatalog() { }

    static List<Variable> variables(ArcadePortGame game) {
        if (game == null) return List.of();
        List<String> prefixes = prefixes(game);
        List<Variable> result = new ArrayList<>();
        for (Class<?> source : ruleSources(game)) {
        for (Field field : source.getFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)) continue;
            String name = field.getName();
            boolean sharedRules = source == ArcadePortRules.class;
            if (sharedRules && prefixes.stream().noneMatch(name::startsWith)) continue;
            if (!sharedRules && !Modifier.isPublic(field.getModifiers())) continue;
            if (!sharedRules && !standaloneFieldIsWired(game, name)) continue;
            String key = sharedRules ? name : rulePrefix(game) + name;
            boolean dynamicConsumer = game.isMonsterMaze() || game == ArcadePortGame.SNEAKY_ASSASSINS
                    || game == ArcadePortGame.CAMERA_SHY;
            if (!GameDebugWiredRuleKeys.KEYS.contains(key) && !dynamicConsumer) continue;
            Kind kind = kind(field.getType());
            if (kind == null) continue;
            try {
                Object raw = field.get(null);
                double value = raw instanceof Boolean enabled ? (enabled ? 1.0D : 0.0D)
                        : ((Number) raw).doubleValue();
                if (game == ArcadePortGame.MONSTER_MAZE && source.getSimpleName().equals("MazeDebugRules")
                        && name.equals("JUMPER_MAX_JUMPS")) value = 5.0D;
                double minimum = minimum(name, value, kind);
                double maximum = maximum(name, kind, value);
                if (game == ArcadePortGame.SUPER_SPLOOR && source == SuperSploorRules.class) {
                    minimum = superSploorMinimum(name, kind);
                    maximum = superSploorMaximum(name, kind);
                }
                result.add(new Variable(key, humanize(name), kind, value,
                        minimum, maximum, step(name, value, kind)));
            } catch (IllegalAccessException ignored) {
                // Public constants should be readable; ignore a field if a JVM denies access.
            }
        }
        }
        if (game == ArcadePortGame.WIZARDS) {
            for (au.com.testplex.wizards.WizardsKit kit : au.com.testplex.wizards.WizardsKit.values()) {
                String prefix = "WIZARDS_KIT_" + kit.name() + "_";
                result.add(new Variable(prefix + "MAX_MANA", kit.name() + " maximum mana",
                        Kind.FLOAT, kit.maximumMana(), 1.0D, 1_000.0D, 5.0D));
                result.add(new Variable(prefix + "MANA_PER_SECOND", kit.name() + " mana regeneration",
                        Kind.FLOAT, kit.manaPerSecond(), 0.0D, 50.0D, 0.25D));
                result.add(new Variable(prefix + "STARTING_WANDS", kit.name() + " starting wands",
                        Kind.INTEGER, kit.startingWands(), 1.0D, 9.0D, 1.0D));
            }
            for (au.com.testplex.wizards.WizardsSpell spell : au.com.testplex.wizards.WizardsSpell.implementedSpells()) {
                String prefix = "WIZARDS_SPELL_" + spell.name() + "_";
                result.add(new Variable(prefix + "MANA_COST_MULTIPLIER",
                        spell.displayName() + " mana cost multiplier", Kind.DOUBLE, 1.0D, 0.0D, 5.0D, 0.1D));
                result.add(new Variable(prefix + "COOLDOWN_MULTIPLIER",
                        spell.displayName() + " cooldown multiplier", Kind.DOUBLE, 1.0D, 0.0D, 5.0D, 0.1D));
            }
        }
        result.sort(Comparator.comparing(Variable::key));
        return List.copyOf(result);
    }

    static Variable variable(ArcadePortGame game, String key) {
        if (game == null || key == null || key.isBlank()) return null;
        return variables(game).stream().filter(candidate -> candidate.key().equals(key)).findFirst().orElse(null);
    }

    static boolean appliesImmediately(Variable variable) {
        if (variable == null) return false;
        return variable.kind() == Kind.BOOLEAN && (LIVE_BOOLEAN_RULES.contains(variable.key())
                || LIVE_SUPER_SPLOOR_BOOLEAN_RULES.contains(variable.key()))
                || variable.kind() != Kind.BOOLEAN && (LIVE_SNEAKY_NUMERIC_RULES.contains(variable.key())
                || LIVE_CAMERA_NUMERIC_RULES.contains(variable.key())
                || LIVE_SPLEEF_NUMERIC_RULES.contains(variable.key())
                || LIVE_SUPER_SPLOOR_NUMERIC_RULES.contains(variable.key())
                || variable.key().startsWith("WIZARDS_SPELL_")
                || variable.key().startsWith("WIZARDS_KIT_") && variable.key().endsWith("_MANA_PER_SECOND"));
    }

    static double clamp(Variable variable, double value) {
        if (variable == null || !Double.isFinite(value)) return Double.NaN;
        double bounded = Math.max(variable.minimum(), Math.min(variable.maximum(), value));
        return switch (variable.kind()) {
            case INTEGER, LONG -> Math.rint(bounded);
            case FLOAT -> (double) (float) bounded;
            case DOUBLE -> bounded;
            case BOOLEAN -> Double.NaN;
        };
    }

    private static List<Class<?>> ruleSources(ArcadePortGame game) {
        List<Class<?>> sources = new ArrayList<>(List.of(ArcadePortRules.class));
        String className = switch (game) {
            case CAMERA_SHY -> "CameraShyRules";
            case HOOPS_REMASTERED -> "HoopsRemasteredRules";
            case SUPER_SPLOOR -> "SuperSploorRules";
            case SNAKE -> "au.com.testplex.snake.SnakeRules";
            case MILK_THE_COW -> "MilkCowRules";
            case SPEED_BUILDERS -> "SpeedBuildersRules";
            case PLEXBALL -> "PlexballRules";
            case SKY_WARS -> "SkyWarsMixedArcadeRules";
            case GLADIATORS -> null;
            case WIZARDS -> "au.com.testplex.wizards.WizardsRules";
            case WITHER_ASSAULT -> "WitherAssaultRules";
            case SNEAKY_ASSASSINS -> "SneakyAssassinsRules";
            case MONSTER_MAZE, MONSTER_MAZE_SPEED, MONSTER_MAZE_MODERN -> "me.monstermaze.game.MazeDebugRules";
            default -> null;
        };
        if (className != null) {
            try {
                sources.add(Class.forName(className.contains(".") ? className
                        : GameDebugVariableCatalog.class.getPackageName() + "." + className));
            } catch (ClassNotFoundException ignored) {
                // Some game modules are intentionally not present in every build flavor.
            }
        }
        return List.copyOf(sources);
    }

    private static boolean standaloneFieldIsWired(ArcadePortGame game, String name) {
        if (game == ArcadePortGame.SPEED_BUILDERS) {
            return Set.of("DEFAULT_VIEW_SECONDS", "DEFAULT_BUILD_SECONDS", "MIN_BUILD_SECONDS",
                    "MAX_BUILD_SECONDS", "INTERMISSION_SECONDS", "DEFAULT_MATCH_ROUNDS",
                    "TEMPLATE_MOBS_ENABLED", "PERFECT_SCORE_PERCENT", "FINAL_RESULTS_SECONDS",
                    "BUILD_TIME_DECREASE_PER_ROUND_SECONDS", "PERFECT_BUILD_TIME_TIEBREAK_ENABLED",
                    "DOUBLE_JUMP_VERTICAL_VELOCITY", "DOUBLE_JUMP_ENABLED")
                    .contains(name);
        }
        if (game == ArcadePortGame.WIZARDS) {
            return Set.of("SOUL_STAR_MANA_PER_SECOND", "MAGE_RANDOM_STARTER_SPELL_COUNT",
                    "MAGE_STARTER_SPELL_SELECTION_ATTEMPTS", "FULL_WAND_CONVERSION",
                    "MIN_PLAYERS", "MAX_PLAYERS", "WAND_SLOT_COUNT", "ENDGAME_START_MINUTES", "ENDGAME_INITIAL_STRIKE_MILLIS",
                    "ENDGAME_MIN_STRIKE_MILLIS", "ENDGAME_STRIKE_REDUCTION_PER_TICK_MILLIS",
                    "MANA_REGEN_MULTIPLIER", "PROJECTILE_SPEED_MULTIPLIER",
                    "SPELL_COOLDOWN_MULTIPLIER", "SPELL_DAMAGE_MULTIPLIER",
                    "SPELL_HEAL_MULTIPLIER", "SPELL_MANA_COST_MULTIPLIER").contains(name);
        }
        if (game == ArcadePortGame.WITHER_ASSAULT) return Set.of("MINIONS_PER_WAVE",
                "MINION_COOLDOWN_MILLIS", "MINION_LIFETIME_MILLIS", "SCENT_COOLDOWN_MILLIS",
                "LEAP_COOLDOWN_MILLIS", "MEDIC_REFILL_INTERVAL_MILLIS", "FLETCHER_ARROW_REFILL_MILLIS",
                "SKULL_ATTACK_COOLDOWN_MILLIS", "SMOKE_ARROW_MAX_LIFETIME_TICKS",
                "SMOKE_ARROW_BLINDNESS_TICKS", "SMOKE_ARROW_BLIND_RADIUS_BLOCKS",
                "EDITOR_BUILD_DEPTH_BELOW_MARKER", "MINION_LAUNCH_SPEED", "MINION_TARGET_RANGE_BLOCKS",
                "MINION_SPEED_AMPLIFIER", "MINION_SPEED_DURATION_TICKS", "FIRE_STORM_CHARGE_PER_TICK",
                "FIRE_STORM_DRAIN_PER_TICK", "FIRE_STORM_ARROWS_PER_TICK", "FIRE_STORM_ARROW_SPEED",
                "FIRE_STORM_SPREAD", "FLIGHT_MIN_SPEED", "FLIGHT_SPAWN_HOVER_SPEED", "FLIGHT_MAX_SPEED",
                "FLIGHT_BASE_SPEED", "FLIGHT_SPEED_PER_Y_BLOCK", "FLIGHT_BLEND_START_BLOCKS",
                "FLIGHT_BLEND_END_BLOCKS", "FLIGHT_MAX_SPEED_CHANGE_PER_TICK", "HUMAN_ARROW_DAMAGE_MULTIPLIER",
                "SKULL_BLAST_RADIUS_BLOCKS", "SKULL_MIN_DAMAGE", "SKULL_MAX_DAMAGE",
                "SKULL_KNOCKBACK_POWER", "SKULL_KNOCKBACK_Y_MULTIPLIER", "SKULL_TERRAIN_RADIUS_BLOCKS",
                "SKULL_KNOCKBACK_Y_MINIMUM", "SKULL_BLOCK_RESTORE_TICKS", "HUMAN_LEAP_HORIZONTAL_POWER",
                "ARCHER_LEAP_HORIZONTAL_MULTIPLIER", "HUMAN_LEAP_VERTICAL_MINIMUM",
                "HUMAN_LEAP_VERTICAL_MAXIMUM", "HUMAN_LEAP_VERTICAL_LOOK_ADD", "SKULL_LAUNCH_SPEED").contains(name);
        if (game == ArcadePortGame.HOOPS_REMASTERED) return Set.of("TARGET_SCORE", "REGULATION_MILLIS",
                "SHOT_CLOCK_MILLIS", "HOPS_COOLDOWN_MILLIS", "OFFENSIVE_REBOUND_SHOT_CLOCK_MILLIS",
                "THREE_POINT_DISTANCE", "STEAL_RADIUS", "BLOCK_RADIUS", "BLOCK_COOLDOWN_MILLIS",
                "RIM_SCORING_RADIUS", "CLUTCH_WINDOW_MILLIS", "MIN_PLAYERS", "MAX_PLAYERS",
                "ASSIST_WINDOW_MILLIS", "BUZZER_WINDOW_MILLIS", "SHOT_AIM_TOLERANCE", "LAYUP_DISTANCE",
                "DUNK_DISTANCE", "COMEBACK_SCORE_GAP", "STEAL_COOLDOWN_MILLIS", "BALL_GRAVITY",
                "BALL_WALL_BOUNCE", "BALL_FLOOR_BOUNCE", "BALL_FLOOR_FRICTION",
                "BALL_FLOOR_BOUNCE_FRICTION", "BALL_MOVEMENT_FRICTION", "BALL_MAX_BOUNCES",
                "BALL_LOOSE_LIMIT_MILLIS", "BALL_RECLAIM_MILLIS", "PASS_SPEED", "PASS_VERTICAL_SPEED",
                "PASS_FLIGHT_TICKS_PER_BLOCK", "PASS_MIN_FLIGHT_TICKS", "PASS_MAX_FLIGHT_TICKS",
                "PASS_MIN_DISTANCE", "PASS_MAX_DISTANCE", "PASS_AIM_DOT", "BLOCK_KNOCKBACK_HORIZONTAL",
                "BLOCK_KNOCKBACK_VERTICAL", "HOPS_HORIZONTAL", "HOPS_VERTICAL",
                "SHOT_DUNK_FLIGHT_TICKS_PER_BLOCK", "SHOT_LAYUP_FLIGHT_TICKS_PER_BLOCK",
                "SHOT_NORMAL_FLIGHT_TICKS_PER_BLOCK", "SHOT_DUNK_MIN_FLIGHT_TICKS", "SHOT_DUNK_MAX_FLIGHT_TICKS",
                "SHOT_LAYUP_MIN_FLIGHT_TICKS", "SHOT_LAYUP_MAX_FLIGHT_TICKS",
                "SHOT_NORMAL_MIN_FLIGHT_TICKS", "SHOT_NORMAL_MAX_FLIGHT_TICKS",
                "BALL_CATCH_RADIUS_SQUARED", "SHOT_LAUNCH_PROTECTION_DISTANCE_SQUARED",
                "BLOCK_AIM_DOT", "STEAL_AIM_DOT", "POSSESSION_MOVEMENT_DAMPING").contains(name);
        if (game == ArcadePortGame.SNEAKY_ASSASSINS) return SneakyAssassinsRules.debuggableRule(name);
        if (game == ArcadePortGame.CAMERA_SHY) return CameraShyRules.debuggableRule(name);
        if (game.isMonsterMaze()) {
            return Set.of("MOB_MOVE_SPEED", "BODY_RUSH_DURATION_MILLIS", "CRYO_COOLDOWN_MILLIS",
                    "CRYO_FREEZE_MILLIS", "CRYO_RADIUS_BLOCKS", "JUMP_STRENGTH",
                    "STAGE_TIMER_INITIAL_SECONDS", "STAGE_TIMER_DECREASE_PER_STAGE_SECONDS", "STAGE_TIMER_MIN_SECONDS",
                    "INITIAL_MONSTER_COUNT", "ADDITIONAL_MONSTERS_PER_SAFE_PAD", "MONSTER_HEALTH",
                    "PLAYER_BUMP_COOLDOWN_MILLIS", "BUMP_RADIUS_BLOCKS", "BUMP_DAMAGE",
                    "BUMP_KNOCKBACK_STRENGTH", "BUMP_KNOCKBACK_Y_ADD", "BUMP_KNOCKBACK_Y_MAX",
                    "BODY_RUSH_STRENGTH", "BODY_RUSH_Y_ADD", "BODY_RUSH_Y_MAX",
                    "SAFE_PAD_DECAY_STAGES", "SAFE_PAD_CAPTURE_RADIUS_BLOCKS",
                    "JUMPER_MAX_JUMPS", "JUMPER_JUMP_COOLDOWN_MILLIS",
                    "SLOWBALL_MAX_SNOWBALLS", "SLOWBALL_REGEN_INTERVAL_MILLIS",
                    "SLOWBALL_SLOW_TICKS", "SLOWBALL_SLOW_AMPLIFIER",
                    "BODY_BUILDER_HEALTH_PER_PAD", "BODY_BUILDER_MAX_HEALTH",
                    "BODY_RUSH_ACTIVATION_CHARGES", "BODY_RUSH_DURATION_REDUCTION_MILLIS",
                    "BODY_RUSH_ACHIEVEMENT_CONTACTS", "REPULSOR_RADIUS_BLOCKS",
                    "REPULSOR_LAUNCH_STRENGTH", "REPULSOR_LAUNCH_Y_ADD", "REPULSOR_LAUNCH_Y_MAX",
                    "REPULSOR_GROUND_CLEANUP_DELAY_MILLIS", "REPULSOR_MAX_LIFETIME_MILLIS").contains(name);
        }
        return true;
    }

    static String rulePrefix(ArcadePortGame game) {
        return switch (game) {
            case CAMERA_SHY -> "CAMERA_SHY_";
            case HOOPS_REMASTERED -> "HOOPS_";
            case SUPER_SPLOOR -> "SUPER_SPLOOR_";
            case SNAKE -> "SNAKE_";
            case MILK_THE_COW -> "MILK_THE_COW_";
            case SPEED_BUILDERS -> "SPEED_BUILDERS_";
            case PLEXBALL -> "PLEXBALL_";
            case WIZARDS -> "WIZARDS_";
            case WITHER_ASSAULT -> "WITHER_ASSAULT_";
            case SNEAKY_ASSASSINS -> "SNEAKY_ASSASSINS_";
            case ONE_IN_THE_QUIVER -> "QUIVER_";
            case GLADIATORS -> "GLADIATOR_";
            case MONSTER_MAZE -> "MONSTER_MAZE_";
            case MONSTER_MAZE_SPEED -> "MONSTER_MAZE_SPEED_";
            case MONSTER_MAZE_MODERN -> "MONSTER_MAZE_MODERN_";
            default -> game.name() + "_";
        };
    }

    private static Kind kind(Class<?> type) {
        if (type == int.class) return Kind.INTEGER;
        if (type == long.class) return Kind.LONG;
        if (type == float.class) return Kind.FLOAT;
        if (type == double.class) return Kind.DOUBLE;
        if (type == boolean.class) return Kind.BOOLEAN;
        return null;
    }

    private static List<String> prefixes(ArcadePortGame game) {
        List<String> result = new ArrayList<>(List.of("LEGACY_"));
        result.addAll(switch (game) {
            case BACON_BRAWL -> List.of("BACON_", "BACON_BRAWL_");
            case GLADIATORS -> List.of("GLADIATOR_");
            case ONE_IN_THE_QUIVER -> List.of("QUIVER_", "ONE_IN_THE_QUIVER_");
            case CASTLE_ASSAULT -> List.of("CASTLE_ASSAULT_");
            case HEROES_OF_GWEN -> List.of("HEROES_OF_GWEN_");
            default -> List.of(game.name() + "_");
        });
        return List.copyOf(result);
    }

    private static double minimum(String name, double defaultValue, Kind kind) {
        if (kind == Kind.BOOLEAN) return 0.0D;
        if (name.startsWith("SUPER_SPLOOR_")) {
            if (name.endsWith("_MILLIS")) return 1.0D;
            if (name.endsWith("_BLOCKS") || name.endsWith("_SPEED") || name.endsWith("_VELOCITY")) return 0.05D;
            if (name.endsWith("_PERCENT")) return 0.1D;
            if (name.contains("_PER_")) return 0.01D;
            return 1.0D;
        }
        if (Set.of("SPOTLIGHT_INTERVAL_MILLIS", "SPOTLIGHT_GLOW_DURATION_MILLIS", "DECOY_FLASH_LIFETIME_MILLIS",
                "SMOKE_LIFETIME_MILLIS", "SMOKE_ESCAPE_CREDIT_WINDOW_MILLIS", "PURSUIT_ESCAPE_MILLIS",
                "SMOKE_ABILITY_COOLDOWN_MILLIS", "DECOY_ABILITY_COOLDOWN_MILLIS",
                "RUNNER_ABILITY_COOLDOWN_MILLIS", "SPOTTER_ABILITY_COOLDOWN_MILLIS",
                "UNDERSTUDY_ABILITY_COOLDOWN_MILLIS", "RUNNER_ABILITY_DURATION_MILLIS",
                "RUNNER_ACHIEVEMENT_WINDOW_MILLIS", "SPOTTER_MARK_DURATION_MILLIS",
                "STAGE_FRIGHT_DURATION_MILLIS").contains(name)) return 0.0D;
        if (Set.of("PURSUIT_ESCAPE_DISTANCE_BLOCKS", "SPOTTER_RANGE_BLOCKS", "DECOY_FORWARD_OFFSET_BLOCKS",
                "UNDERSTUDY_FORWARD_OFFSET_BLOCKS", "DECOY_APPROACH_RANGE_BLOCKS", "DECOY_LOOK_RANGE_BLOCKS",
                "SMOKE_OCCLUSION_RADIUS_BLOCKS").contains(name)) return 0.1D;
        if (name.equals("DECOY_LOOK_MIN_DOT")) return 0.0D;
        if (name.equals("RUNNER_SPEED_AMPLIFIER")) return 0.0D;
        if (name.equals("OUT_OF_FOCUS_WINDOW_TICKS")) return 0.0D;
        if (Set.of("GROUP_PHOTO_MIN_TARGETS", "PAPARAZZI_UNIQUE_PHOTOS_REQUIRED",
                "PRIVATE_INVESTIGATOR_TARGETS_REQUIRED", "FALSE_ALARM_TARGETS_REQUIRED",
                "ESCAPE_STREAK_REQUIRED", "SMOKE_ESCAPE_STREAK_REQUIRED", "DOUBLE_ELIMINATION_STREAK_REQUIRED",
                "REVENGE_KILLS_REQUIRED", "UNTOUCHABLE_WINS_REQUIRED", "NARROW_ESCAPE_EXPOSURES",
                "PICTURE_PERFECT_PHOTOS_REQUIRED", "STAGE_FRIGHT_EXPOSURES").contains(name)) return 1.0D;
        if (Set.of("MAX_ACTIVE_POWERUPS",
                "SOURCE_NPC_COUNT", "POWERUP_BONUS_HEALTH", "KILL_BONUS_HEALTH").contains(name)) return 0.0D;
        if (Set.of("MAX_POWERUP_PROGRESS", "MASTER_ASSASSIN_POWERUPS_REQUIRED",
                "MASTERS_MASTER_POWERUPS_REQUIRED", "NPC_PATROL_ORDER_CHANCE_DENOMINATOR", "PROSECUTOR_DISTINCT_TARGETS_REQUIRED",
                "DUELS_OTHER_MASTERS_REQUIRED").contains(name)) return 1.0D;
        if (name.endsWith("_SMOKE_BOMBS") || name.endsWith("_STARTING_ARROWS")
                || name.endsWith("_STARTING_EMERALDS") || name.endsWith("_STARTING_DIAMONDS")) return 0.0D;
        if (Set.of("GLOBAL_REVEAL_DURATION_MILLIS", "SMOKE_BLINDNESS_DURATION_MILLIS",
                "NPC_ATTACK_COOLDOWN_MILLIS").contains(name)) return 0.0D;
        if (name.equals("POWERUP_PICKUP_RADIUS_BLOCKS") || name.equals("NPC_ATTACK_RANGE_BLOCKS")) return 0.1D;
        if (name.equals("NPC_MIN_VERTICAL_SPEED")) return -1.0D;
        if (name.startsWith("NPC_") && (name.endsWith("_SPEED") || name.endsWith("_DAMAGE")
                || name.endsWith("_HORIZONTAL") || name.endsWith("_VERTICAL")
                || name.equals("NPC_MOTION_FACTOR"))) return 0.0D;
        if (name.endsWith("_SWORD_DAMAGE") || name.startsWith("PROJECTILE_DAMAGE_")) return 0.0D;
        if (Set.of("SPEED_REFERENCE_LENGTH", "FOOD_SPAWN_ATTEMPTS", "CANNIBAL_KILLS_REQUIRED",
                "CHOO_CHOO_LENGTH_REQUIRED", "SLIMY_SHEEP_REQUIRED", "ROULETTE_HIT_1_SECONDS",
                "ROULETTE_HIT_2_SECONDS", "ROULETTE_HIT_3_SECONDS", "ROULETTE_HIT_4_SECONDS",
                "ROULETTE_HIT_5_SECONDS", "ROULETTE_HIT_6_SECONDS", "ROULETTE_HIT_7_PLUS_SECONDS",
                "ROULETTE_ANIMATION_INTERVAL_TICKS", "HAZARD_SPAWN_ATTEMPTS",
                "HAZARD_SPAWN_DISTANCE_VARIANTS", "HAZARD_MAX_CUBE_SIZE",
                "HAZARD_STEERING_INTERVAL_TICKS").contains(name)) return 1.0D;
        if (name.equals("IDLE_MOVEMENT_DISTANCE_SQUARED") || name.equals("HAZARD_SPAWN_BASE_DISTANCE") || name.equals("HAZARD_SPAWN_DISTANCE_STEP")
                || name.equals("HAZARD_CLEARANCE_BLOCKS") || name.equals("EDGE_STAY_MARGIN_BLOCKS")
                || name.equals("SELF_TAIL_SAFE_SEGMENTS") || name.equals("PASSIVE_GROWTH_SEGMENTS")
                || name.equals("ROULETTE_ANIMATION_STEPS") || name.equals("MAX_HAZARDS")
                || name.endsWith("ABILITY_CHARGES")) return 0.0D;
        if (name.startsWith("ROULETTE_HIT_") && name.endsWith("_SECONDS")) return 1.0D;
        if (name.equals("TURN_LAG_DEGREES_PER_TICK") || name.equals("OVERSHOOT_TURN_MULTIPLIER")
                || name.equals("WEAK_TURN_THRESHOLD_DEGREES") || name.equals("WEAK_TURN_DEGREES_PER_TICK")
                || name.equals("DRIFT_MAX_TURN_DEGREES") || name.startsWith("MOVEMENT_")) return 0.0D;
        if (name.startsWith("SNAKE_") && name.endsWith("_RADIUS")) return 0.0D;
        if (name.equals("RUNNER_DIVE_EARLY_WINDOW_MILLIS") || name.equals("RUNNER_DIVE_DROP_BLOCKS")
                || name.startsWith("RUNNER_SPECIAL_PLAY_")) return 0.0D;
        if (name.equals("RUNNER_ARCHER_ASSISTS_ACHIEVEMENT_COUNT")
                || name.equals("RUNNER_FROSTBALL_ASSISTS_ACHIEVEMENT_COUNT")) return 1.0D;
        if (name.startsWith("PLEXBALL_TIMING_") && name.endsWith("_MAX_TICKS")) return 0.0D;
        if (name.startsWith("PLEXBALL_BATTING_") || name.startsWith("PLEXBALL_TIMING_POWER_")
                || name.endsWith("_BATTING_POWER") || name.endsWith("_TIMING_PERFECT_POWER")
                || name.endsWith("_TIMING_GREAT_POWER") || name.endsWith("_TIMING_GOOD_POWER")
                || name.endsWith("_TIMING_FAIR_POWER") || name.endsWith("_TIMING_WEAK_POWER")
                || name.endsWith("_TIMING_POOR_POWER")) return 0.0D;
        if (name.equals("PAINTBALL_SPEEDRUNNER_MAX_MILLIS")
                || name.equals("PAINTBALL_GROUND_STARE_MILLIS")) return 0.0D;
        if (name.equals("PAINTBALL_FLAWLESS_MIN_STARTERS") || name.equals("PAINTBALL_KILLING_SPREE_KILLS")
                || name.equals("PAINTBALL_LAST_STAND_KILLS") || name.equals("PAINTBALL_SHARE_YOUR_SCREEN_SHOTS")) return 1.0D;
        if (name.startsWith("COW_BOMB_") || name.startsWith("BODY_SLAM_")
                || name.equals("ASSIST_CREDIT_WINDOW_MILLIS")) {
            if (name.equals("COW_BOMB_LIFETIME_MILLIS")) return 1.0D;
            if (name.endsWith("_MILLIS")) return 0.0D;
        }
        if (name.equals("FARMER_DOUBLE_JUMP_MIN_VERTICAL")
                || name.equals("PERFECT_GAME_MAX_FARMER_DEATHS")) return 0.0D;
        if (name.equals("COMEBACK_LEAD_THRESHOLD") || name.equals("GOT_THE_CHOPS_AIRBORNE_KILLS")
                || name.equals("SLAUGHTERHOUSE_ASSISTS")) return 1.0D;
        if (name.equals("COW_BOMB_CONTACT_RADIUS_BLOCKS") || name.equals("BODY_SLAM_RADIUS_BLOCKS")
                || name.equals("BODY_SLAM_HEIGHT_BLOCKS")) return 0.1D;
        if (name.startsWith("MILK_THE_COW_") && name.endsWith("_MILLIS")) {
            if (name.endsWith("COW_CHARGE_HUNGER_INTERVAL_MILLIS")
                    || name.endsWith("HERD_UPDATE_INTERVAL_MILLIS")) return 1.0D;
            return 0.0D;
        }
        if (name.contains("_FALL_DAMAGE_MULTIPLIER") || name.startsWith("EVOLUTION_CHARGE_")) return 0.0D;
        if (name.contains("_MAX_HEALTH")) return 1.0D;
        if (name.contains("_MELEE_DAMAGE")) return 0.0D;
        if (name.equals("HOOPS_MIN_PLAYERS")) return 2.0D;
        if (name.equals("HOOPS_MAX_PLAYERS")) return 2.0D;
        if (name.equals("QUIVER_SCORE_TO_WIN")) return 1.0D;
        if (name.equals("CASTLE_ASSAULT_KING_HEALTH_BASE")
                || name.equals("CASTLE_ASSAULT_KING_HEALTH_REFERENCE_PLAYERS")) return 1.0D;
        if (name.startsWith("CASTLE_ASSAULT_KILLSTREAK_TIER_")) return 1.0D;
        if (name.equals("CASTLE_ASSAULT_TNT_THROW_SPEED")
                || name.equals("CASTLE_ASSAULT_TNT_THROW_VERTICAL_SPEED")) return 0.0D;
        if (name.equals("PAINTBALL_SHOTGUN_PELLETS")) return 1.0D;
        if (name.equals("DEATH_TAG_RUNNER_KILL_REWARD_GEMS")
                || name.equals("DEATH_TAG_TURTLE_STACK_RESET_MILLIS")
                || name.equals("DEATH_TAG_TRICKSTER_FIREWORKS_COOLDOWN_MILLIS")) return 0.0D;
        if (name.equals("DEATH_TAG_TURTLE_MAX_STACKS")
                || name.equals("DEATH_TAG_TURTLE_EFFECT_DURATION_TICKS")
                || name.equals("DEATH_TAG_TURTLE_HITS_PER_WIND_CHARGE")) return 1.0D;
        if (name.equals("BACON_CRISPY_BURN_DURATION_MILLIS")
                || name.equals("BACON_CRISPY_DASH_HIT_RADIUS_SQUARED")
                || name.equals("BACON_CRISPY_FIRE_CONTACT_RADIUS_SQUARED")) return 0.0D;
        if (name.equals("BACON_CRISPY_SHOT_COUNT")) return 1.0D;
        if (name.equals("BACON_CHRIS_ABILITY_METER_COST")) return 0.0D;
        if (name.equals("BACON_CRISPY_SHOT_SPREAD_RADIANS")) return 0.0D;
        if (name.equals("BACON_CRISPY_SHOT_VELOCITY")) return 0.1D;
        if (name.equals("BACON_CRISPY_IMPACT_FIRE_BLOCKS")) return 1.0D;
        if (name.equals("COW_CHARGE_TARGET_RADIUS_BLOCKS")
                || name.equals("COW_CHARGE_TARGET_HEIGHT_BLOCKS")
                || name.equals("HERD_SPAWN_RADIUS_BLOCKS")) return 0.1D;
        if (name.equals("COW_CHARGE_IMPACT_VERTICAL") || name.startsWith("COW_BOMB_")
                || name.startsWith("BODY_SLAM_") || name.startsWith("FARMER_DOUBLE_JUMP_")) return 0.0D;
        if (name.equals("MILK_THE_COW_PERFECT_GAME_MAX_FARMER_DEATHS")) return 0.0D;
        if (name.equals("MILK_THE_COW_COMEBACK_LEAD_THRESHOLD")
                || name.equals("MILK_THE_COW_GOT_THE_CHOPS_AIRBORNE_KILLS")
                || name.equals("MILK_THE_COW_SLAUGHTERHOUSE_ASSISTS")) return 1.0D;
        if (name.equals("COW_CHARGE_HUNGER_INTERVAL_MILLIS")
                || name.equals("HERD_UPDATE_INTERVAL_MILLIS")
                || name.equals("COW_SPRINT_SPEED_DURATION_TICKS")) return 1.0D;
        if (name.equals("COW_CHARGE_TARGET_COOLDOWN_MILLIS")) return 0.0D;
        if (name.equals("HERD_FOLLOW_MIN_DISTANCE_SQUARED")) return 0.0D;
        if (name.equals("PHOTOS_TO_ELIMINATE")) return 1.0D;
        if (name.equals("CLOSE_PHOTO_BLOCKS") || name.equals("LONG_PHOTO_BLOCKS")) return 0.1D;
        if (name.equals("PHOTO_LENS_MIN_DOT")) return 0.5D;
        if (name.equals("MASTER_BUILDERS_MAX_RATING")) return 1.0D;
        if (name.equals("MASTER_BUILDERS_MAX_BUILDERS")) return 1.0D;
        if (name.equals("MASTER_BUILDERS_THEME_OVERRIDE")) return 0.0D;
        if (name.equals("SUPER_SPLOOR_MAX_PAINT_MARKS")) return 1.0D;
        if (name.equals("BALL_RADIUS")) return 0.05D;
        if (name.equals("BALL_MAX_FLIGHT_TICKS")) return 1.0D;
        if (name.equals("BATTING_POWER_MULTIPLIER")) return 0.25D;
        if (name.equals("CURVE_ACCELERATION") || name.equals("CURVE_ARTIST_SIDE_SPEED")
                || name.equals("SLUGGER_DIVE_SPEED") || name.equals("SPRINTER_BURST_SPEED")
                || name.equals("SKYGUARD_LEAP_SPEED")) return 0.0D;
        if (name.equals("CHARGER_FULL_CHARGE_MILLIS") || name.equals("INK_REGEN_INTERVAL_MILLIS")) return 1.0D;
        if (name.equals("CHARGER_MIN_INK_COST") || name.equals("CHARGER_MAX_INK_COST")) return 0.0D;
        if (name.equals("SPECIAL_CHARGE_REQUIRED")) return 1.0D;
        if ((name.startsWith("CHARGER_") && name.endsWith("_SPEED"))
                || name.startsWith("SPECIAL_CHARGE_PER_")
                || name.endsWith("_INK_REGEN_PER_SECOND")) return 0.0D;
        if (name.equals("COW_SPRINT_SPEED_AMPLIFIER") || name.equals("SCORE_PER_COW_KILL")) return 0.0D;
        if (name.startsWith("GLADIATOR_TIER_") && name.endsWith("_ARROWS")) return 0.0D;
        if (name.equals("GLADIATOR_POTION_HEALING")
                || name.equals("GLADIATOR_MELEE_KNOCKBACK_HORIZONTAL")
                || name.equals("GLADIATOR_MELEE_KNOCKBACK_VERTICAL")
                || name.equals("GLADIATOR_PROJECTILE_KNOCKBACK_HORIZONTAL")
                || name.equals("GLADIATOR_PROJECTILE_KNOCKBACK_VERTICAL")) return 0.0D;
        if (name.equals("GLADIATOR_POTION_SPLASH_RADIUS")) return 0.1D;
        if (name.equals("GLADIATOR_MELEE_NO_DAMAGE_TICKS")
                || name.equals("GLADIATOR_PROJECTILE_NO_DAMAGE_TICKS")
                || name.equals("GLADIATOR_ENVIRONMENTAL_NO_DAMAGE_TICKS")
                || name.equals("GLADIATOR_PVP_DAMAGE_MULTIPLIER")) return 0.0D;
        if (name.equals("GLADIATOR_STARTING_TIER_OVERRIDE")) return -1.0D;
        if (name.equals("GLADIATOR_MODE_OVERRIDE") || name.equals("GLADIATOR_POTION_COUNT")) return 0.0D;
        if (name.equals("GLADIATOR_VOTE_MILLIS") || name.equals("GLADIATOR_ARENA_TRANSITION_MILLIS")) return 0.0D;
        if (name.equals("TURF_WARS_LINES_PER_KILL_MULTIPLIER")) return 1.0D;
        if (name.equals("TURF_WARS_COMEBACK_REMAINING_LINES")) return 0.0D;
        if (name.equals("HEROES_OF_GWEN_OVERTIME_PATH_ADVANCE_DISTANCE_SQUARED")
                || name.equals("HEROES_OF_GWEN_DEVON_TNT_BOW_BASE_DAMAGE")
                || name.equals("HEROES_OF_GWEN_DEVON_TNT_BOW_POWER_DAMAGE_PER_LEVEL")
                || name.equals("HEROES_OF_GWEN_ROWENA_BOW_BASE_DAMAGE")
                || name.equals("HEROES_OF_GWEN_ROWENA_BOW_POWER_DAMAGE_PER_LEVEL")
                || name.equals("HEROES_OF_GWEN_ROWENA_BOMBARDMENT_DAMAGE_FACTOR")
                || name.equals("HEROES_OF_GWEN_WITHER_EARTHQUAKE_DAMAGE")) return 0.0D;
        if (name.equals("HEROES_OF_GWEN_DEVON_FLETCHER_MAX_ARROWS")
                || name.equals("HEROES_OF_GWEN_ROWENA_FLETCHER_MAX_ARROWS")) return 0.0D;
        if (name.equals("HEROES_OF_GWEN_SMALL_HEALTH_POTION_HEAL_HP")
                || name.equals("HEROES_OF_GWEN_LARGE_HEALTH_POTION_HEAL_HP")) return 0.0D;
        if (name.equals("HEROES_OF_GWEN_CAPTURE_UPDATE_INTERVAL_MILLIS")
                || name.equals("HEROES_OF_GWEN_MINION_COMBAT_INTERVAL_MILLIS")) return 1.0D;
        if (name.equals("HEROES_OF_GWEN_CAPTURE_POINT_RANGE_SQUARED")
                || name.equals("HEROES_OF_GWEN_MINION_DAMAGE")
                || name.equals("HEROES_OF_GWEN_MINION_TOWER_DAMAGE")) return 0.0D;
        if (name.equals("HEROES_OF_GWEN_MINION_HEALTH")) return 1.0D;
        if (name.equals("HEROES_OF_GWEN_MINION_SOURCE_TARGET_RANGE_SQUARED")) return 0.0D;
        if (name.equals("HOOPS_BALL_CATCH_RADIUS_SQUARED")
                || name.equals("HOOPS_SHOT_LAUNCH_PROTECTION_DISTANCE_SQUARED")) return 0.0D;
        if (name.equals("HOOPS_BLOCK_AIM_DOT") || name.equals("HOOPS_STEAL_AIM_DOT")) return -1.0D;
        if (name.startsWith("HOOPS_SHOT_") && (name.endsWith("_MIN_FLIGHT_TICKS")
                || name.endsWith("_MAX_FLIGHT_TICKS"))) return 1.0D;
        if (name.equals("SPLEEF_SMASHER_RADIUS_BLOCKS")) return 0.0D;
        if (Set.of("MANA_REGEN_MULTIPLIER", "PROJECTILE_SPEED_MULTIPLIER", "SPELL_COOLDOWN_MULTIPLIER",
                "SPELL_DAMAGE_MULTIPLIER", "SPELL_HEAL_MULTIPLIER", "SPELL_MANA_COST_MULTIPLIER")
                .contains(name)) return 0.0D;
        if (name.equals("TURF_WARS_COMBAT_BOUNDARY_DAMAGE")
                || name.equals("TURF_WARS_BUILD_BOUNDARY_DAMAGE")
                || name.equals("TURF_WARS_TERRITORY_KNOCKBACK_HORIZONTAL")
                || name.equals("TURF_WARS_TERRITORY_KNOCKBACK_VERTICAL")
                || name.equals("TURF_WARS_BARRAGE_ARROW_SPEED")
                || name.equals("TURF_WARS_BARRAGE_ARROW_SPREAD")) return 0.0D;
        if (name.equals("PERFECT_SCORE_PERCENT")) return 1.0D;
        if (name.equals("BUILD_TIME_DECREASE_PER_ROUND_SECONDS")
                || name.equals("FINAL_RESULTS_SECONDS")) return 0.0D;
        if (name.equals("DOUBLE_JUMP_VERTICAL_VELOCITY")) return 0.1D;
        if (name.equals("SPLEEF_ARROW_CLEAR_RADIUS_SPEED_DIVISOR")) return 0.1D;
        if (name.startsWith("SPLEEF_") && name.endsWith("_SPEED_MULTIPLIER")) return 0.1D;
        if (name.startsWith("SPLEEF_") && (name.endsWith("_BLOCKS") || name.contains("_VERTICAL_") || name.endsWith("_BONUS")
                || name.endsWith("_POWER") || name.endsWith("_KNOCKBACK")
                || name.endsWith("_REACH_BLOCKS"))) return 0.0D;
        if (name.equals("SPLEEF_HUNGER_RESTORE_PER_HIT")) return 0.0D;
        if (name.equals("RUNNER_MAX_FROSTBALLS")) return 0.0D;
        if (name.equals("RUNNER_STEPS_ACHIEVEMENT_BLOCKS")
                || name.equals("RUNNER_HOP_ACHIEVEMENT_JUMPS")
                || name.equals("RUNNER_FLASH_ACHIEVEMENT_MAX_WIN_MILLIS")) return 0.0D;
        if (name.equals("SHEEP_QUEST_MAX_CARRY")) return 0.0D;
        if (name.equals("SHEEP_QUEST_SHEPHERD_SLOWNESS_SHEEP_PER_LEVEL")) return 1.0D;
        if (name.startsWith("RUNNER_") && name.endsWith("_MILLIS")) return 0.0D;
        if (name.startsWith("RUNNER_") && name.endsWith("_TICKS")) return 0.0D;
        if (name.startsWith("SHEEP_QUEST_") && (name.endsWith("_MILLIS") || name.endsWith("_TICKS"))) return 0.0D;
        if (name.equals("DRAGON_ESCAPE_SCORE_UPDATE_MILLIS")) return 1.0D;
        if (name.equals("DRAGON_ESCAPE_DEFEATER_WINS")
                || name.equals("DRAGON_ESCAPE_MAP_WINS_FOR_ACHIEVEMENT")
                || name.equals("DRAGON_ESCAPE_RAGEBAITER_KILLS")) return 1.0D;
        if (name.equals("DRAGONS_MAX_DRAGONS") || name.equals("DRAGONS_MAX_SPARKLERS")
                || name.equals("DRAGONS_MARKSMAN_START_ARROWS") || name.equals("DRAGONS_MARKSMAN_MAX_ARROWS")
                || name.equals("DRAGONS_BARRAGE_MAX_ARROWS") || name.equals("DRAGONS_RETARGET_DISTANCE_SQUARED")) return 0.0D;
        if (name.equals("DRAGONS_TAMER_VERTICAL_FLIGHT_CAP")) return 0.0D;
        if (name.equals("EVOLUTION_BLAZE_SPREAD_RADIANS")
                || name.equals("EVOLUTION_GOLEM_SLAM_DAMAGE_FLOOR")
                || name.equals("EVOLUTION_WATER_DAMAGE")) return 0.0D;
        if (name.equals("EVOLUTION_WATER_DAMAGE_INTERVAL_MILLIS")
                || name.equals("EVOLUTION_GOLEM_SLAM_IMPACT_DELAY_MILLIS")
                || name.equals("EVOLUTION_SLIME_SLAM_MAX_DURATION_MILLIS")
                || name.equals("EVOLUTION_SLIME_SLAM_MIN_AIRTIME_MILLIS")) return 1.0D;
        if (name.equals("DRAW_MY_THING_HINT_REVEAL_DURATION_MULTIPLIER")) return 0.1D;
        if (name.equals("DRAW_MY_THING_MAX_HINTED_LETTER_FRACTION")) return 0.0D;
        if (name.equals("DRAW_MY_THING_BRUSH_RADIUS_BLOCKS")
                || name.equals("DRAW_MY_THING_GUESS_COOLDOWN_MILLIS")
                || name.equals("DRAW_MY_THING_INTERMISSION_MILLIS")) return 0.0D;
        if (name.equals("DRAGONS_DODGEMS_QUIET_MILLIS")) return 0.0D;
        if (name.equals("DRAGONS_FROST_PATH_STEP")) return 0.01D;
        if (name.startsWith("DRAGON_ESCAPE_") && name.endsWith("_LEAP_USES")) return 0.0D;
        if (name.startsWith("DRAGON_ESCAPE_LEAP_") && name.endsWith("_Y_BONUS")) return 0.0D;
        if (name.startsWith("DRAGONS_FLIGHT_") && name.endsWith("_VERTICAL_SPEED")) return -1.0D;
        if (name.equals("DRAGONS_FLIGHT_VERTICAL_BIAS") || name.equals("DRAGONS_TURN_RATE")) return 0.0D;
        if (name.equals("DRAGONS_GLIDER_MIN_VERTICAL")) return -1.0D;
        if (name.startsWith("DRAGONS_") && (name.endsWith("_SPEED") || name.endsWith("_KNOCKBACK")
                || name.endsWith("_SPREAD") || name.endsWith("_BONUS") || name.endsWith("_FACTOR")
                || name.endsWith("_CAP") || name.endsWith("_MAX_Y") || name.endsWith("_LAUNCH_Y")
                || name.endsWith("_RANGE_SQUARED") || name.endsWith("_BLOCKS"))) return 0.0D;
        if (name.startsWith("DRAGON_ESCAPE_") && (name.endsWith("_MILLIS") || name.endsWith("_TICKS"))) return 0.0D;
        if (name.equals("DRAGON_ESCAPE_DIGGER_MAX_USES") || name.equals("DRAGON_ESCAPE_DISRUPTOR_MAX_TNT")) return 0.0D;
        if (name.startsWith("DRAGON_ESCAPE_") && (name.endsWith("_SPEED") || name.endsWith("_RADIUS")
                || name.endsWith("_DISTANCE") || name.endsWith("_LAUNCH_Y"))) return 0.0D;
        if (name.equals("SHEEP_QUEST_THROW_BOUNCE_MULTIPLIER")) return -1.0D;
        if (name.equals("SHEEP_QUEST_SHEPHERD_SLOWNESS_MAX_AMPLIFIER")) return 0.0D;
        if (Set.of("MAX_PLAYERS", "EARTH_MAX_DIRT", "CRUMBLE_LAYERS_PER_TICK",
                "CRUMBLE_BLOCKS_PER_TICK", "GUARDIAN_HEALTH", "DEATH_BOMBER_KILLS",
                "METAL_BASE_MAX_HEALTH", "ICE_BRIDGE_LENGTH_BLOCKS",
                "ICE_BRIDGE_BLOCKS_PER_TICK").contains(name)) return 1.0D;
        if (name.equals("GUARDIAN_HOME_LEASH_RADIUS_BLOCKS") || name.equals("CRUMBLE_FALLING_BLOCK_CHANCE")
                || name.equals("ICE_BRIDGE_HALF_WIDTH_BLOCKS")) return 0.0D;
        if (name.equals("METAL_PULL_MIN_VERTICAL") || name.equals("DIRT_PROJECTILE_KNOCKBACK_VERTICAL")) return -4.0D;
        if (name.startsWith("SHEEP_QUEST_") && (name.endsWith("_SPEED") || name.endsWith("_ADD")
                || name.endsWith("_MIN") || name.endsWith("_MAX") || name.endsWith("_SCALE")
                || name.endsWith("_KNOCKBACK") || name.endsWith("_RADIUS")
                || name.endsWith("_BLOCKS"))) return 0.0D;
        if (name.startsWith("RUNNER_") && (name.endsWith("_SPEED") || name.endsWith("_SPEED_MILLIS"))) return 0.0D;
        if (name.startsWith("PAINTBALL_") && name.endsWith("_MILLIS")) return 1.0D;
        if (name.equals("ORE_ISLAND_RADIUS_BLOCKS")) return 1.0D;
        if (name.endsWith("_VEINS_PER_ISLAND") || name.equals("RANDOM_VEIN_EXTRA_SIZE")) return 0.0D;
        if (name.equals("TNT_THROW_STRENGTH") || name.equals("ICE_BRIDGE_LAUNCH_Y")
                || name.equals("FIRE_BURST_DAMAGE") || name.equals("METAL_MAGNET_DAMAGE")
                || name.equals("METAL_MAGNET_PULL_BASE") || name.equals("METAL_MAGNET_PULL_PER_ARMOR")
                || name.equals("EARTH_THROW_SPEED")) return 0.0D;
        if (name.startsWith("PAINTBALL_") && (name.endsWith("_SPREAD") || name.endsWith("_SPEED")
                || name.endsWith("_SPEED_MULTIPLIER") || name.endsWith("_RADIUS")
                || name.endsWith("_CHARGE_PER_TICK") || name.endsWith("_CHARGE_PER_EXTRA_PAINT")
                || name.endsWith("_MULTIPLIER"))) return 0.0D;
        if (name.startsWith("PAINTBALL_") && (name.endsWith("_PAINT") || name.endsWith("_PAINT_PER_HIT")
                || name.endsWith("_PELLETS") || name.endsWith("_TICKS") || name.endsWith("_AMPLIFIER"))) return 0.0D;
        if (name.equals("MICRO_BATTLES_CHEAT_WINDOW_MILLIS")
                || name.equals("MICRO_BATTLES_COMBAT_LOG_EXPIRE_MILLIS")) return 0.0D;
        if (name.equals("MICRO_BATTLES_DASHER_JUMP_HORIZONTAL_SPEED")
                || name.equals("MICRO_BATTLES_DASHER_JUMP_VERTICAL_SPEED")) return 0.0D;
        if (name.equals("MICRO_BATTLES_BORDER_PARTICLE_DISTANCE")) return 0.5D;
        if (name.equals("MICRO_BATTLES_BORDER_PARTICLE_VERTICAL_POINTS")) return 1.0D;
        if (name.equals("MINION_TARGET_RANGE_BLOCKS")) return 0.0D;
        if (name.equals("MINION_LAUNCH_SPEED") || name.equals("FIRE_STORM_SPREAD")
                || name.equals("FIRE_STORM_CHARGE_PER_TICK") || name.equals("FIRE_STORM_DRAIN_PER_TICK")
                || name.equals("FLIGHT_SPEED_PER_Y_BLOCK")) return 0.0D;
        if (name.equals("FLIGHT_BLEND_START_BLOCKS")) return 0.0D;
        if (name.equals("FLIGHT_BLEND_END_BLOCKS")) return 0.1D;
        if (name.equals("HUMAN_ARROW_DAMAGE_MULTIPLIER") || name.equals("SKULL_MIN_DAMAGE")
                || name.equals("SKULL_KNOCKBACK_POWER") || name.equals("SKULL_KNOCKBACK_Y_MULTIPLIER")
                || name.equals("SKULL_KNOCKBACK_Y_MINIMUM")) return 0.0D;
        if (name.equals("SKULL_BLAST_RADIUS_BLOCKS")) return 0.1D;
        if (name.equals("REBOUND_POWER_PER_BLOCK") || name.equals("REBOUND_VERTICAL_ARC_PER_BLOCK")
                || name.equals("LEAPER_JUMP_VERTICAL_ADD") || name.equals("GROUND_POUND_VERTICAL_BASE")
                || name.equals("GROUND_POUND_VERTICAL_PROXIMITY")) return 0.0D;
        if (name.equals("REBOUND_MAX_IMPACT_DISTANCE_SQUARED")) return 0.0D;
        if (name.equals("SAFE_PAD_CAPTURE_RADIUS_BLOCKS")) return 0.5D;
        if (name.equals("JUMPER_MAX_JUMPS") || name.equals("BODY_RUSH_ACHIEVEMENT_CONTACTS")) return 0.0D;
        if (name.equals("SLOWBALL_MAX_SNOWBALLS") || name.equals("BODY_RUSH_ACTIVATION_CHARGES")) return 1.0D;
        if (name.equals("BODY_BUILDER_MAX_HEALTH")) return 20.0D;
        if (name.equals("REPULSOR_RADIUS_BLOCKS")) return 0.1D;
        if (name.equals("MOB_MOVE_SPEED")) return 0.05D;
        if (name.equals("BUMP_RADIUS_BLOCKS")) return 0.25D;
        if (name.endsWith("_SECONDS")) return 1.0D;
        if (name.endsWith("_X") || name.endsWith("_Y") || name.endsWith("_Z")) return -512.0D;
        String upper = name.toUpperCase(Locale.ROOT);
        if (defaultValue > 0.0D && (upper.endsWith("_MILLIS") || upper.endsWith("_TICKS")
                || upper.contains("_INTERVAL_") || upper.contains("_COOLDOWN_"))) return 1.0D;
        if (defaultValue > 0.0D && (upper.contains("MAX_") || upper.contains("COUNT")
                || upper.contains("LENGTH") || upper.contains("INTERVAL") || upper.contains("SPEED")
                || upper.contains("RADIUS") || upper.contains("DISTANCE") || upper.contains("WIDTH")
                || upper.contains("HEIGHT"))) {
            return kind == Kind.INTEGER || kind == Kind.LONG ? 1.0D : 0.001D;
        }
        if (kind == Kind.INTEGER || kind == Kind.LONG) return 0.0D;
        return 0.0D;
    }

    private static double superSploorMinimum(String name, Kind kind) {
        if (kind == Kind.BOOLEAN) return 0.0D;
        if (Set.of("CHARGER_MIN_SPEED", "CHARGER_MIN_INK_COST", "CHARGER_MAX_INK_COST").contains(name)) return 0.0D;
        if (name.endsWith("_MILLIS")) return 1.0D;
        if (name.endsWith("_BLOCKS") || name.endsWith("_SPEED") || name.endsWith("_VELOCITY")) return 0.05D;
        if (name.startsWith("SPECIAL_CHARGE_PER_")) return 0.01D;
        return 1.0D;
    }

    private static double superSploorMaximum(String name, Kind kind) {
        if (kind == Kind.BOOLEAN) return 1.0D;
        if (name.endsWith("_MILLIS")) return 600_000.0D;
        if (name.endsWith("_BLOCKS")) return 64.0D;
        if (name.endsWith("_SPEED") || name.endsWith("_VELOCITY")) return 10.0D;
        if (name.endsWith("_PERCENT")) return 100.0D;
        if (name.equals("MAX_PAINT_MARKS")) return 10.0D;
        if (name.equals("SPECIAL_CHARGE_REQUIRED")) return 100.0D;
        if (name.equals("CHARGER_MAX_INK_COST")) return 500.0D;
        return 1_000.0D;
    }

    private static double maximum(String name, Kind kind, double defaultValue) {
        if (kind == Kind.BOOLEAN) return 1.0D;
        if (name.startsWith("SUPER_SPLOOR_")) {
            if (name.endsWith("_MILLIS")) return 600_000.0D;
            if (name.endsWith("_BLOCKS")) return 64.0D;
            if (name.endsWith("_SPEED") || name.endsWith("_VELOCITY")) return 10.0D;
            if (name.endsWith("_PERCENT")) return 100.0D;
            return 1_000.0D;
        }
        if (Set.of("SPOTLIGHT_INTERVAL_MILLIS", "SPOTLIGHT_GLOW_DURATION_MILLIS", "DECOY_FLASH_LIFETIME_MILLIS",
                "SMOKE_LIFETIME_MILLIS", "SMOKE_ESCAPE_CREDIT_WINDOW_MILLIS", "PURSUIT_ESCAPE_MILLIS",
                "SMOKE_ABILITY_COOLDOWN_MILLIS", "DECOY_ABILITY_COOLDOWN_MILLIS",
                "RUNNER_ABILITY_COOLDOWN_MILLIS", "SPOTTER_ABILITY_COOLDOWN_MILLIS",
                "UNDERSTUDY_ABILITY_COOLDOWN_MILLIS", "RUNNER_ABILITY_DURATION_MILLIS",
                "RUNNER_ACHIEVEMENT_WINDOW_MILLIS", "SPOTTER_MARK_DURATION_MILLIS",
                "STAGE_FRIGHT_DURATION_MILLIS").contains(name)) return 600_000.0D;
        if (Set.of("PURSUIT_ESCAPE_DISTANCE_BLOCKS", "SPOTTER_RANGE_BLOCKS", "DECOY_FORWARD_OFFSET_BLOCKS",
                "UNDERSTUDY_FORWARD_OFFSET_BLOCKS", "DECOY_APPROACH_RANGE_BLOCKS", "DECOY_LOOK_RANGE_BLOCKS",
                "SMOKE_OCCLUSION_RADIUS_BLOCKS").contains(name)) return 64.0D;
        if (name.equals("DECOY_LOOK_MIN_DOT")) return 1.0D;
        if (name.equals("RUNNER_SPEED_AMPLIFIER")) return 4.0D;
        if (name.equals("OUT_OF_FOCUS_WINDOW_TICKS")) return 1_200.0D;
        if (Set.of("GROUP_PHOTO_MIN_TARGETS", "PAPARAZZI_UNIQUE_PHOTOS_REQUIRED",
                "PRIVATE_INVESTIGATOR_TARGETS_REQUIRED", "FALSE_ALARM_TARGETS_REQUIRED",
                "ESCAPE_STREAK_REQUIRED", "SMOKE_ESCAPE_STREAK_REQUIRED", "DOUBLE_ELIMINATION_STREAK_REQUIRED",
                "REVENGE_KILLS_REQUIRED", "UNTOUCHABLE_WINS_REQUIRED", "NARROW_ESCAPE_EXPOSURES",
                "PICTURE_PERFECT_PHOTOS_REQUIRED", "STAGE_FRIGHT_EXPOSURES").contains(name)) return 1_000.0D;
        if (name.equals("MAX_POWERUP_PROGRESS") || name.equals("MASTER_ASSASSIN_POWERUPS_REQUIRED")
                || name.equals("MASTERS_MASTER_POWERUPS_REQUIRED")) return 32.0D;
        if (name.equals("MAX_ACTIVE_POWERUPS")) return 32.0D;
        if (name.endsWith("_SMOKE_BOMBS") || name.endsWith("_STARTING_ARROWS")
                || name.endsWith("_STARTING_EMERALDS") || name.endsWith("_STARTING_DIAMONDS")) return 64.0D;
        if (name.equals("SOURCE_NPC_COUNT")) return 500.0D;
        if (name.equals("NPC_PATROL_WAYPOINT_ATTEMPTS")) return 64.0D;
        if (name.equals("NPC_SPAWN_SEARCH_ATTEMPTS")) return 1_000.0D;
        if (name.equals("REVEALER_PROJECTILE_LIFETIME_TICKS")) return 1_200.0D;
        if (name.equals("POWERUP_PICKUP_RADIUS_BLOCKS") || name.equals("NPC_ATTACK_RANGE_BLOCKS")) return 16.0D;
        if (name.endsWith("_SWORD_DAMAGE") || name.startsWith("PROJECTILE_DAMAGE_")) return 40.0D;
        if (Set.of("POWERUP_BONUS_HEALTH", "POWERUP_MAX_HEALTH", "KILL_BONUS_HEALTH").contains(name)) {
            return 40.0D;
        }
        if (name.startsWith("NPC_") && (name.endsWith("_SPEED") || name.endsWith("_DAMAGE")
                || name.endsWith("_HORIZONTAL") || name.endsWith("_VERTICAL")
                || name.equals("NPC_MOTION_FACTOR"))) return 8.0D;
        if (name.startsWith("NPC_") && name.endsWith("_DENOMINATOR")) return 1_000.0D;
        if (Set.of("GLOBAL_REVEAL_DURATION_MILLIS", "SMOKE_BLINDNESS_DURATION_MILLIS",
                "NPC_ATTACK_COOLDOWN_MILLIS", "REVEALER_PROJECTILE_LIFETIME_TICKS").contains(name)) {
            return name.endsWith("_TICKS") ? 1_200.0D : 600_000.0D;
        }
        if (Set.of("IDLE_TIMEOUT_MILLIS", "PREPARE_COUNTDOWN_MILLIS", "FOOD_SPAWN_INTERVAL_MILLIS",
                "GROWTH_INTERVAL_MILLIS", "FIRST_HAZARD_DELAY_MILLIS", "HAZARD_INTERVAL_MILLIS",
                "HAZARD_RETRY_MILLIS", "SPEED_ABILITY_DURATION_MILLIS", "REVERSE_ABILITY_DURATION_MILLIS",
                "INVULNERABLE_ABILITY_DURATION_MILLIS", "KIT_ABILITY_COOLDOWN_MILLIS").contains(name)) {
            return 600_000.0D;
        }
        if (name.equals("SPEED_REFERENCE_LENGTH") || name.equals("CHOO_CHOO_LENGTH_REQUIRED")) return 10_000.0D;
        if (name.equals("FOOD_SPAWN_RADIUS")) return 256.0D;
        if (name.equals("FOOD_PICKUP_RADIUS") || name.equals("TAIL_COLLISION_RADIUS")) return 16.0D;
        if (name.equals("START_SPAWN_RADIUS_BLOCKS")) return 16.0D;
        if (name.equals("IDLE_MOVEMENT_DISTANCE_SQUARED")) return 256.0D;
        if (name.equals("SEGMENT_SPACING") || name.equals("BODY_FOLLOW_SPACING")) return 8.0D;
        if (name.equals("MOVEMENT_SOURCE_BASE_MULTIPLIER") || name.equals("MOVEMENT_GROWTH_CAP")
                || name.equals("MOVEMENT_SPEED_BOOST_MULTIPLIER")) return 8.0D;
        if (name.equals("MOVEMENT_CONVERSION_SCALE")) return 2.0D;
        if (name.equals("MOVEMENT_CONVERSION_BASE")) return 8.0D;
        if (name.equals("OVERSHOOT_TURN_MULTIPLIER")) return 4.0D;
        if (name.endsWith("_DEGREES_PER_TICK") || name.endsWith("_TURN_DEGREES")
                || name.equals("WEAK_TURN_THRESHOLD_DEGREES")) return 180.0D;
        if (name.equals("FOOD_SPAWN_ATTEMPTS") || name.equals("HAZARD_SPAWN_ATTEMPTS")) return 1_000.0D;
        if (name.equals("HAZARD_SPAWN_BASE_DISTANCE") || name.equals("HAZARD_SPAWN_DISTANCE_STEP")
                || name.equals("HAZARD_SPAWN_DISTANCE_VARIANTS") || name.equals("HAZARD_CLEARANCE_BLOCKS")) return 128.0D;
        if (name.equals("HAZARD_MAX_CUBE_SIZE")) return 16.0D;
        if (name.equals("HAZARD_STEERING_SPEED")) return 8.0D;
        if (name.equals("HAZARD_CONTACT_RADIUS")) return 32.0D;
        if (name.equals("MAX_HAZARDS")) return 64.0D;
        if (name.equals("ROULETTE_ANIMATION_STEPS")) return 100.0D;
        if (name.equals("ROULETTE_ANIMATION_INTERVAL_TICKS")) return 100.0D;
        if (name.equals("BUSTED_STREAK_REQUIRED")) return 100.0D;
        if (name.equals("EDGE_STAY_MARGIN_BLOCKS")) return 128.0D;
        if (name.endsWith("ABILITY_CHARGES")) return 64.0D;
        if (name.equals("PASSIVE_GROWTH_SEGMENTS") || name.equals("FOOD_GROWTH_SEGMENTS")) return 64.0D;
        if (name.equals("SELF_TAIL_SAFE_SEGMENTS")) return 32.0D;
        if (name.equals("CANNIBAL_KILLS_REQUIRED") || name.equals("SLIMY_SHEEP_REQUIRED")) return 1_000.0D;
        if (name.startsWith("PLEXBALL_TIMING_POWER_") || name.startsWith("PLEXBALL_TIMING_")
                && name.endsWith("_POWER")) return 1.0D;
        if (name.startsWith("PLEXBALL_TIMING_") && name.endsWith("_MAX_TICKS")) return 200.0D;
        if (name.startsWith("PLEXBALL_BATTING_")) return 10.0D;
        if (name.endsWith("_BATTING_POWER")) return 4.0D;
        if (name.equals("PLEXBALL_TIMING_CYCLE_MIN_TICKS")
                || name.equals("PLEXBALL_TIMING_CYCLE_MAX_TICKS")) return 200.0D;
        if (name.equals("PLEXBALL_TECHNICIAN_TIMING_BONUS_TICKS")) return 40.0D;
        if (name.equals("PLEXBALL_INTRO_TICKS") || name.equals("PLEXBALL_TURN_BREAK_TICKS")) return 600.0D;
        if (name.contains("_FALL_DAMAGE_MULTIPLIER")) return 1.0D;
        if (name.equals("RUNNER_FAILED_UPWARDS_MAX_HEALTH")) return 20.0D;
        if (name.equals("TURF_WARS_TERRITORY_KNOCKBACK_VERTICAL")) return 5.0D;
        if (name.contains("_MAX_HEALTH")) return 80.0D;
        if (name.contains("_MELEE_DAMAGE")) return 40.0D;
        if (name.startsWith("EVOLUTION_CHARGE_")) return 0.2D;
        if (name.equals("GLADIATOR_MELEE_KNOCKBACK_HORIZONTAL")
                || name.equals("GLADIATOR_PROJECTILE_KNOCKBACK_HORIZONTAL")) return 3.0D;
        if (name.equals("GLADIATOR_MELEE_KNOCKBACK_VERTICAL")
                || name.equals("GLADIATOR_PROJECTILE_KNOCKBACK_VERTICAL")) return 2.0D;
        if (name.startsWith("PAINTBALL_") && name.endsWith("_KNOCKBACK_MULTIPLIER")) return 3.0D;
        if (name.equals("SPLEEF_KNOCKBACK_VERTICAL_CAP")) return 12.0D;
        if (name.contains("_KNOCKBACK")) return 8.0D;
        if (name.contains("_LAUNCH_SPEED") || name.contains("_LAUNCH_HORIZONTAL")) return 8.0D;
        if (name.contains("_LAUNCH_VERTICAL") || name.endsWith("_EGG_SPEED")) return 5.0D;
        if (name.equals("HOOPS_MIN_PLAYERS") || name.equals("HOOPS_MAX_PLAYERS")) return 16.0D;
        if (name.equals("QUIVER_SCORE_TO_WIN")) return 200.0D;
        if (name.equals("PAINTBALL_SPEEDRUNNER_MAX_MILLIS")
                || name.equals("PAINTBALL_GROUND_STARE_MILLIS")) return 600_000.0D;
        if (name.equals("PAINTBALL_FLAWLESS_MIN_STARTERS") || name.equals("PAINTBALL_KILLING_SPREE_KILLS")
                || name.equals("PAINTBALL_LAST_STAND_KILLS") || name.equals("PAINTBALL_SHARE_YOUR_SCREEN_SHOTS")) return 100.0D;
        if (name.equals("PERFECT_SCORE_PERCENT")) return 100.0D;
        if (name.equals("BUILD_TIME_DECREASE_PER_ROUND_SECONDS")) return 30.0D;
        if (name.equals("FINAL_RESULTS_SECONDS")) return 60.0D;
        if (name.equals("DOUBLE_JUMP_VERTICAL_VELOCITY")) return 2.5D;
        if (name.equals("SPLEEF_HUNGER_RESTORE_PER_HIT")) return 20.0D;
        if (name.startsWith("SPLEEF_") && name.endsWith("_SPEED_MULTIPLIER")) return 4.0D;
        if (name.equals("SPLEEF_LEAP_FORWARD_POWER") || name.equals("SPLEEF_LEAP_VERTICAL_ADD")
                || name.equals("SPLEEF_LEAP_VERTICAL_CAP") || name.equals("SPLEEF_LEAP_GROUND_BONUS")
                || name.equals("SPLEEF_KNOCKBACK_VERTICAL_BASE")
                || name.equals("SPLEEF_KNOCKBACK_GROUND_BONUS")) return 5.0D;
        if (name.equals("SPLEEF_KNOCKBACK_VERTICAL_CAP")) return 12.0D;
        if (name.equals("SPLEEF_ARROW_CLEAR_RADIUS_SPEED_DIVISOR")) return 10.0D;
        if (name.startsWith("SPLEEF_ARROW_CLEAR_RADIUS_") && name.endsWith("_BLOCKS")) return 8.0D;
        if (name.equals("SPLEEF_PROJECTILE_CLEAR_RADIUS_MIN_BLOCKS")) return 4.0D;
        if (name.equals("SPLEEF_PROJECTILE_CLEAR_RADIUS_MAX_BLOCKS")) return 8.0D;
        if (name.equals("SPLEEF_PROJECTILE_IMPACT_SEARCH_RADIUS_BLOCKS")) return 4.0D;
        if (name.equals("HEROES_OF_GWEN_OVERTIME_DELAY_MILLIS")) return 3_600_000.0D;
        if (name.equals("HEROES_OF_GWEN_OVERTIME_PATH_ADVANCE_DISTANCE_SQUARED")) return 256.0D;
        if (name.equals("HEROES_OF_GWEN_DEVON_TNT_BOW_BASE_DAMAGE")
                || name.equals("HEROES_OF_GWEN_ROWENA_BOW_BASE_DAMAGE")) return 100.0D;
        if (name.equals("HEROES_OF_GWEN_DEVON_TNT_BOW_POWER_DAMAGE_PER_LEVEL")
                || name.equals("HEROES_OF_GWEN_ROWENA_BOW_POWER_DAMAGE_PER_LEVEL")) return 5.0D;
        if (name.equals("HEROES_OF_GWEN_ROWENA_BOMBARDMENT_DAMAGE_FACTOR")) return 8.0D;
        if (name.equals("HEROES_OF_GWEN_WITHER_EARTHQUAKE_DAMAGE")) return 40.0D;
        if (name.equals("HEROES_OF_GWEN_DEVON_FLETCHER_MAX_ARROWS")
                || name.equals("HEROES_OF_GWEN_ROWENA_FLETCHER_MAX_ARROWS")) return 64.0D;
        if (name.equals("HEROES_OF_GWEN_SMALL_HEALTH_POTION_HEAL_HP")
                || name.equals("HEROES_OF_GWEN_LARGE_HEALTH_POTION_HEAL_HP")) return 40.0D;
        if (name.equals("HEROES_OF_GWEN_CAPTURE_UPDATE_INTERVAL_MILLIS")
                || name.equals("HEROES_OF_GWEN_MINION_COMBAT_INTERVAL_MILLIS")) return 30_000.0D;
        if (name.equals("HEROES_OF_GWEN_CAPTURE_POINT_RANGE_SQUARED")
                || name.equals("HEROES_OF_GWEN_MINION_SOURCE_TARGET_RANGE_SQUARED")) return 1_024.0D;
        if (name.equals("HEROES_OF_GWEN_MINION_HEALTH")) return 200.0D;
        if (name.equals("HEROES_OF_GWEN_MINION_DAMAGE")
                || name.equals("HEROES_OF_GWEN_MINION_TOWER_DAMAGE")) return 40.0D;
        if (name.equals("HOOPS_BALL_CATCH_RADIUS_SQUARED")
                || name.equals("HOOPS_SHOT_LAUNCH_PROTECTION_DISTANCE_SQUARED")) return 100.0D;
        if (name.equals("HOOPS_BLOCK_AIM_DOT") || name.equals("HOOPS_STEAL_AIM_DOT")) return 1.0D;
        if (name.startsWith("HOOPS_SHOT_") && (name.endsWith("_MIN_FLIGHT_TICKS")
                || name.endsWith("_MAX_FLIGHT_TICKS"))) return 60.0D;
        if (name.equals("SPLEEF_SMASHER_RADIUS_BLOCKS")) return 8.0D;
        if (name.equals("DEATH_TAG_TURTLE_STACK_RESET_MILLIS")
                || name.equals("DEATH_TAG_TRICKSTER_FIREWORKS_COOLDOWN_MILLIS")) return 600_000.0D;
        if (name.equals("DEATH_TAG_TURTLE_MAX_STACKS")) return 10.0D;
        if (name.equals("DEATH_TAG_TURTLE_EFFECT_DURATION_TICKS")) return 1_200.0D;
        if (name.equals("DEATH_TAG_TURTLE_HITS_PER_WIND_CHARGE")) return 100.0D;
        if (name.equals("BACON_CRISPY_BURN_DURATION_MILLIS")) return 60_000.0D;
        if (name.equals("BACON_CRISPY_DASH_HIT_RADIUS_SQUARED")
                || name.equals("BACON_CRISPY_FIRE_CONTACT_RADIUS_SQUARED")) return 64.0D;
        if (name.equals("BACON_CRISPY_IMPACT_FIRE_BLOCKS")) return 9.0D;
        if (name.equals("BACON_CRISPY_SHOT_COUNT")) return 9.0D;
        if (name.equals("BACON_CHRIS_ABILITY_METER_COST")) return 100.0D;
        if (name.equals("BACON_CRISPY_SHOT_SPREAD_RADIANS")) return 0.6D;
        if (name.equals("BACON_CRISPY_SHOT_VELOCITY")) return 3.0D;
        if (name.equals("COW_CHARGE_HUNGER_INTERVAL_MILLIS")
                || name.equals("HERD_UPDATE_INTERVAL_MILLIS")
                || name.equals("COW_CHARGE_TARGET_COOLDOWN_MILLIS")) return 120_000.0D;
        if (name.startsWith("COW_BOMB_") || name.startsWith("BODY_SLAM_")
                || name.equals("ASSIST_CREDIT_WINDOW_MILLIS")) {
            if (name.endsWith("_MILLIS")) return 600_000.0D;
            if (name.endsWith("_RADIUS_BLOCKS") || name.endsWith("_HEIGHT_BLOCKS")) return 64.0D;
            return 16.0D;
        }
        if (name.startsWith("FARMER_DOUBLE_JUMP_") || name.equals("COW_CHARGE_IMPACT_VERTICAL")) return 16.0D;
        if (name.equals("COMEBACK_LEAD_THRESHOLD") || name.equals("GOT_THE_CHOPS_AIRBORNE_KILLS")
                || name.equals("SLAUGHTERHOUSE_ASSISTS") || name.equals("PERFECT_GAME_MAX_FARMER_DEATHS")) return 100.0D;
        if (name.equals("COW_CHARGE_TARGET_RADIUS_BLOCKS")
                || name.equals("COW_CHARGE_TARGET_HEIGHT_BLOCKS")
                || name.equals("HERD_SPAWN_RADIUS_BLOCKS")) return 64.0D;
        if (name.equals("COW_CHARGE_DAMAGE")) return 40.0D;
        if (name.startsWith("COW_CHARGE_KNOCKBACK_") || name.startsWith("COW_CHARGE_VERTICAL_")) return 8.0D;
        if (name.equals("HERD_FOLLOW_MIN_DISTANCE_SQUARED")) return 4_096.0D;
        if (name.equals("FARMER_HEAL_PER_MILK_HP")) return 40.0D;
        if (name.equals("COW_SPRINT_SPEED_DURATION_TICKS")) return 1_200.0D;
        if (name.equals("COW_SPRINT_SPEED_AMPLIFIER")) return 10.0D;
        if (name.equals("SCORE_PER_COW_KILL")) return 64.0D;
        if (name.equals("PHOTOS_TO_ELIMINATE")) return 100.0D;
        if (name.equals("CLOSE_PHOTO_BLOCKS")) return 32.0D;
        if (name.equals("LONG_PHOTO_BLOCKS")) return 64.0D;
        if (name.equals("PHOTO_LENS_MIN_DOT")) return 0.999D;
        if (name.equals("BALL_RADIUS")) return 1.0D;
        if (name.equals("BALL_MAX_FLIGHT_TICKS")) return 400.0D;
        if (name.equals("BATTING_POWER_MULTIPLIER")) return 3.0D;
        if (name.equals("CURVE_ACCELERATION")) return 0.03D;
        if (name.equals("CURVE_ARTIST_SIDE_SPEED")) return 1.0D;
        if (name.equals("SLUGGER_DIVE_SPEED") || name.equals("SPRINTER_BURST_SPEED")
                || name.equals("SKYGUARD_LEAP_SPEED")) return 5.0D;
        if (name.startsWith("CHARGER_") && name.endsWith("_MILLIS")) return 120_000.0D;
        if (name.endsWith("_INK_COST")) return 500.0D;
        if (name.startsWith("CHARGER_") && name.endsWith("_SPEED")) return 10.0D;
        if (name.equals("INK_REGEN_INTERVAL_MILLIS")) return 120_000.0D;
        if (name.endsWith("_INK_REGEN_PER_SECOND")) return 200.0D;
        if (name.equals("SPECIAL_CHARGE_REQUIRED")) return 100.0D;
        if (name.startsWith("SPECIAL_CHARGE_PER_")) return 100.0D;
        if (name.equals("MAX_INK") || name.equals("INK_PER_PRIMARY_SHOT")) return 500.0D;
        if (name.equals("MAX_PAINT_MARKS")) return 10.0D;
        if (name.equals("DODGE_ROLL_MAX_CHARGES")) return 5.0D;
        if (name.equals("SQUID_WALK_SPEED") || name.equals("SQUID_CLIMB_VELOCITY")) return 1.0D;
        if (name.equals("MASTER_BUILDERS_MAX_RATING")) return 6.0D;
        if (name.equals("MASTER_BUILDERS_MAX_BUILDERS")) return 16.0D;
        if (name.equals("MASTER_BUILDERS_THEME_OVERRIDE")) return 16.0D;
        if (Set.of("MANA_REGEN_MULTIPLIER", "PROJECTILE_SPEED_MULTIPLIER", "SPELL_COOLDOWN_MULTIPLIER",
                "SPELL_DAMAGE_MULTIPLIER", "SPELL_HEAL_MULTIPLIER", "SPELL_MANA_COST_MULTIPLIER")
                .contains(name)) return 5.0D;
        if (name.startsWith("GLADIATOR_TIER_") && name.endsWith("_ARROWS")) return 64.0D;
        if (name.equals("GLADIATOR_POTION_HEALING")) return 20.0D;
        if (name.equals("GLADIATOR_POTION_SPLASH_RADIUS")) return 16.0D;
        if (name.equals("GLADIATOR_POTION_COUNT")) return 64.0D;
        if (name.equals("GLADIATOR_STARTING_TIER_OVERRIDE")) return 3.0D;
        if (name.equals("GLADIATOR_MODE_OVERRIDE")) return 4.0D;
        if (name.equals("GLADIATOR_VOTE_MILLIS") || name.equals("GLADIATOR_ARENA_TRANSITION_MILLIS")) return 60_000.0D;
        if (name.equals("GLADIATOR_MELEE_NO_DAMAGE_TICKS")
                || name.equals("GLADIATOR_PROJECTILE_NO_DAMAGE_TICKS")
                || name.equals("GLADIATOR_ENVIRONMENTAL_NO_DAMAGE_TICKS")) return 40.0D;
        if (name.equals("GLADIATOR_PVP_DAMAGE_MULTIPLIER")) return 5.0D;
        if (name.equals("TURF_WARS_LINES_PER_KILL_MULTIPLIER")) return 8.0D;
        if (name.equals("TURF_WARS_COMEBACK_REMAINING_LINES")) return 100.0D;
        if (name.equals("TURF_WARS_COMBAT_BOUNDARY_DAMAGE")) return 40.0D;
        if (name.equals("TURF_WARS_BUILD_BOUNDARY_DAMAGE")) return 500.0D;
        if (name.equals("TURF_WARS_TERRITORY_KNOCKBACK_HORIZONTAL")) return 8.0D;
        if (name.equals("TURF_WARS_TERRITORY_KNOCKBACK_VERTICAL")) return 5.0D;
        if (name.equals("TURF_WARS_BARRAGE_ARROW_SPEED")) return 8.0D;
        if (name.equals("TURF_WARS_BARRAGE_ARROW_SPREAD")) return 1.0D;
        if (name.equals("JUMPER_MAX_JUMPS") || name.equals("SLOWBALL_MAX_SNOWBALLS")
                || name.equals("BODY_RUSH_ACTIVATION_CHARGES")
                || name.equals("BODY_RUSH_ACHIEVEMENT_CONTACTS")) return 64.0D;
        if (name.equals("SLOWBALL_SLOW_AMPLIFIER")) return 10.0D;
        if (name.equals("SLOWBALL_SLOW_TICKS")) return 1_200.0D;
        if (name.equals("SLOWBALL_REGEN_INTERVAL_MILLIS") || name.endsWith("CLEANUP_DELAY_MILLIS")
                || name.equals("REPULSOR_MAX_LIFETIME_MILLIS")
                || name.equals("BODY_RUSH_DURATION_REDUCTION_MILLIS")) return 120_000.0D;
        if (name.equals("BODY_BUILDER_MAX_HEALTH")) return 80.0D;
        if (name.equals("BODY_BUILDER_HEALTH_PER_PAD")) return 40.0D;
        if (name.equals("REPULSOR_RADIUS_BLOCKS")) return 64.0D;
        if (name.startsWith("REPULSOR_LAUNCH_")) return 8.0D;
        if (name.equals("JUMPER_JUMP_COOLDOWN_MILLIS")) return 120_000.0D;
        if (name.startsWith("RUNNER_") && name.endsWith("_SPEED")) return 4.0D;
        if (name.equals("RUNNER_SWITCH_GRACE_MILLIS")) return 5_000.0D;
        if (name.equals("RUNNER_BLOCK_DECAY_MILLIS")) return 60_000.0D;
        if (name.equals("RUNNER_FROSTBALL_INTERVAL_MILLIS")) return 10_000.0D;
        if (name.equals("RUNNER_LEAP_COOLDOWN_MILLIS")
                || name.equals("RUNNER_QUICKSHOT_COOLDOWN_MILLIS")) return 120_000.0D;
        if (name.equals("RUNNER_MAX_FROSTBALLS")) return 64.0D;
        if (name.equals("RUNNER_ARCHER_ASSISTS_ACHIEVEMENT_COUNT")
                || name.equals("RUNNER_FROSTBALL_ASSISTS_ACHIEVEMENT_COUNT")) return 100.0D;
        if (name.equals("RUNNER_FAILED_UPWARDS_MAX_HEALTH")) return 20.0D;
        if (name.equals("RUNNER_SPECIAL_PLAY_MIN_FALL_DISTANCE")) return 100.0D;
        if (name.equals("RUNNER_SPECIAL_PLAY_GROUND_DISTANCE")) return 16.0D;
        if (name.equals("RUNNER_SPECIAL_PLAY_MAX_DOWNWARD_VELOCITY")) return 5.0D;
        if (name.equals("RUNNER_DIVE_DROP_BLOCKS")) return 64.0D;
        if (name.equals("RUNNER_DIVE_EARLY_WINDOW_MILLIS")) return 600_000.0D;
        if (name.equals("RUNNER_STEPS_ACHIEVEMENT_BLOCKS")) return 100_000.0D;
        if (name.equals("RUNNER_HOP_ACHIEVEMENT_JUMPS")) return 1_000.0D;
        if (name.equals("RUNNER_FLASH_ACHIEVEMENT_MAX_WIN_MILLIS")) return 600_000.0D;
        if (name.equals("RUNNER_FROSTBALL_SLOW_TICKS")) return 1_200.0D;
        if (name.equals("RUNNER_FROSTBALL_SLOW_AMPLIFIER")) return 10.0D;
        if (name.startsWith("SHEEP_QUEST_") && name.endsWith("_MILLIS")) return 120_000.0D;
        if (name.startsWith("SHEEP_QUEST_") && name.endsWith("_TICKS")) return 2_400.0D;
        if (name.equals("SHEEP_QUEST_BESERKER_JUMP_COOLDOWN_MILLIS")
                || name.equals("SHEEP_QUEST_BESERKER_LEAP_WINDOW_MILLIS")) return 120_000.0D;
        if (name.equals("SHEEP_QUEST_BESERKER_LEAP_HORIZONTAL_SPEED")
                || name.equals("SHEEP_QUEST_BESERKER_LEAP_VERTICAL_ADD")
                || name.equals("SHEEP_QUEST_BESERKER_LEAP_VERTICAL_MIN")
                || name.equals("SHEEP_QUEST_BESERKER_LEAP_VERTICAL_MAX")) return 4.0D;
        if (name.equals("SHEEP_QUEST_BRUTE_THROW_SPEED")
                || name.equals("SHEEP_QUEST_BRUTE_THROW_VERTICAL_MIN")
                || name.equals("SHEEP_QUEST_BRUTE_THROW_VERTICAL_MAX")
                || name.equals("SHEEP_QUEST_BRUTE_THROW_VERTICAL_PITCH_SCALE")) return 4.0D;
        if (name.equals("SHEEP_QUEST_THROW_HIT_RADIUS") || name.equals("SHEEP_QUEST_CAPTURE_RADIUS_BLOCKS")
                || name.equals("SHEEP_QUEST_HOME_RETURN_DISTANCE_BLOCKS")) return 64.0D;
        if (name.equals("SHEEP_QUEST_THROW_KNOCKBACK")
                || name.equals("SHEEP_QUEST_THROW_VERTICAL_KNOCKBACK")
                || name.equals("SHEEP_QUEST_THROW_BOUNCE_VERTICAL")) return 8.0D;
        if (name.equals("SHEEP_QUEST_THROW_BOUNCE_MULTIPLIER")) return 1.0D;
        if (name.equals("SHEEP_QUEST_SHEPHERD_SLOWNESS_SHEEP_PER_LEVEL")) return 64.0D;
        if (name.equals("SHEEP_QUEST_SHEPHERD_SLOWNESS_MAX_AMPLIFIER")) return 127.0D;
        if (name.equals("SHEEP_QUEST_CAPTURE_SCORE")) return 100.0D;
        if (name.startsWith("SHEEP_QUEST_") && (name.endsWith("_THRESHOLD")
                || name.endsWith("_ACHIEVEMENT_COUNT") || name.endsWith("_WINS")
                || name.endsWith("_PER_TARGET"))) return 10_000.0D;
        if (name.equals("MAX_PLAYERS")) return 16.0D;
        if (name.equals("EARTH_MAX_DIRT")) return 64.0D;
        if (name.equals("CRUMBLE_LAYERS_PER_TICK")) return 16.0D;
        if (name.equals("CRUMBLE_BLOCKS_PER_TICK")) return 256.0D;
        if (name.equals("GUARDIAN_HEALTH")) return 100.0D;
        if (name.equals("DEATH_BOMBER_KILLS")) return 1_000.0D;
        if (name.equals("CRUMBLE_FALLING_BLOCK_CHANCE")) return 1.0D;
        if (name.equals("METAL_BASE_MAX_HEALTH")) return 40.0D;
        if (name.equals("METAL_ARMOR_HEALTH_PER_PIECE")) return 20.0D;
        if (Set.of("METAL_PULL_MIN_VERTICAL", "AIR_DOUBLE_JUMP_MIN_VERTICAL", "AIR_DOUBLE_JUMP_VERTICAL_ADD" )
                .contains(name)) return 8.0D;
        if (name.equals("DIRT_PROJECTILE_HIT_RADIUS_SQUARED")) return 64.0D;
        if (name.equals("DIRT_PROJECTILE_DAMAGE") || name.equals("GUARDIAN_EXPLOSION_DAMAGE")) return 40.0D;
        if (name.equals("GUARDIAN_HOME_SPEED")) return 4.0D;
        if (name.equals("AIR_SAFE_RETURN_VERTICAL_OFFSET")
                || name.equals("DIRT_PROJECTILE_KNOCKBACK_HORIZONTAL_MULTIPLIER")) return 8.0D;
        if (name.equals("ICE_BRIDGE_LENGTH_BLOCKS")) return 64.0D;
        if (name.equals("ICE_BRIDGE_HALF_WIDTH_BLOCKS")) return 8.0D;
        if (name.equals("ICE_BRIDGE_BLOCKS_PER_TICK")) return 64.0D;
        if (name.equals("AIR_DOUBLE_JUMP_COOLDOWN_MILLIS")
                || name.equals("EARTH_DIRT_REFILL_INTERVAL_MILLIS")) return 120_000.0D;
        if (name.startsWith("SHEEP_QUEST_") && name.endsWith("_SPEED")) return 4.0D;
        if (name.startsWith("SHEEP_QUEST_") && (name.endsWith("_RADIUS") || name.endsWith("_BLOCKS"))) return 64.0D;
        if (name.startsWith("DRAGON_ESCAPE_") && name.endsWith("_MILLIS")) return 120_000.0D;
        if (name.startsWith("DRAGON_ESCAPE_") && name.endsWith("_TICKS")) return 2_400.0D;
        if (name.equals("DRAGON_ESCAPE_DIGGER_MAX_USES") || name.equals("DRAGON_ESCAPE_DISRUPTOR_MAX_TNT")) return 64.0D;
        if (name.equals("DRAGON_ESCAPE_CHASE_SPEED")) return 2.0D;
        if (name.equals("DRAGON_ESCAPE_RACE_GAP_ESTIMATED_SPEED")) return 30.0D;
        if (name.equals("DRAGON_ESCAPE_DEFEATER_WINS")
                || name.equals("DRAGON_ESCAPE_MAP_WINS_FOR_ACHIEVEMENT")
                || name.equals("DRAGON_ESCAPE_RAGEBAITER_KILLS")) return 1_000.0D;
        if (name.endsWith("_LEAP_USES")) return 64.0D;
        if (name.startsWith("DRAGON_ESCAPE_LEAP_") && name.endsWith("_Y_BONUS")) return 2.0D;
        if (name.equals("DRAGON_ESCAPE_STRATS_GAP_BLOCKS")) return 64.0D;
        if (name.equals("DRAGONS_MAX_DRAGONS") || name.equals("DRAGONS_MAX_SPARKLERS")
                || name.equals("DRAGONS_MARKSMAN_START_ARROWS") || name.equals("DRAGONS_MARKSMAN_MAX_ARROWS")
                || name.equals("DRAGONS_BARRAGE_MAX_ARROWS")) return 64.0D;
        if (name.equals("DRAGONS_TAMER_LOOKAHEAD_BLOCKS") || name.equals("DRAGONS_SPARKLER_MIN_ATTRACT_Y")
                || name.equals("DRAGONS_RETARGET_ALTITUDE_Y")) return 128.0D;
        if (name.equals("DRAGONS_RETARGET_DISTANCE_SQUARED")) return 4_096.0D;
        if (name.equals("DRAGONS_TURN_RATE")) return 1.0D;
        if (name.startsWith("DRAGONS_FLIGHT_") && name.endsWith("_VERTICAL_SPEED")) return 1.0D;
        if (name.equals("DRAGONS_FLIGHT_VERTICAL_BIAS")) return 1.0D;
        if (name.equals("DRAGONS_TAMER_VERTICAL_FLIGHT_CAP")) return 8.0D;
        if (name.equals("DRAGONS_FALL_DAMAGE_REDUCTION")) return 40.0D;
        if (name.equals("DRAGONS_DODGEMS_QUIET_MILLIS")) return 120_000.0D;
        if (name.equals("DRAW_MY_THING_HINT_REVEAL_DURATION_MULTIPLIER")) return 10.0D;
        if (name.equals("DRAW_MY_THING_MAX_HINTED_LETTER_FRACTION")) return 1.0D;
        if (name.equals("DRAW_MY_THING_BRUSH_RADIUS_BLOCKS")) return 4.0D;
        if (name.equals("DRAW_MY_THING_GUESS_COOLDOWN_MILLIS")
                || name.equals("DRAW_MY_THING_INTERMISSION_MILLIS")) return 120_000.0D;
        if (name.equals("EVOLUTION_BLAZE_SPREAD_RADIANS")) return 0.5D;
        if (name.equals("EVOLUTION_GOLEM_SLAM_DAMAGE_FLOOR") || name.equals("EVOLUTION_WATER_DAMAGE")) return 20.0D;
        if (name.equals("EVOLUTION_WATER_DAMAGE_INTERVAL_MILLIS")
                || name.equals("EVOLUTION_GOLEM_SLAM_IMPACT_DELAY_MILLIS")
                || name.equals("EVOLUTION_SLIME_SLAM_MAX_DURATION_MILLIS")) return 10_000.0D;
        if (name.equals("EVOLUTION_SLIME_SLAM_MIN_AIRTIME_MILLIS")) return 2_000.0D;
        if (name.equals("DRAGONS_BARRAGE_ARROW_SPREAD")) return 1.0D;
        if (name.startsWith("DRAGONS_") && (name.endsWith("_Y_BONUS") || name.endsWith("_MAX_Y")
                || name.endsWith("_VERTICAL") || name.endsWith("_VERTICAL_BASE")
                || name.endsWith("_VERTICAL_CAP") || name.endsWith("_VERTICAL_FLIGHT_FACTOR")
                || name.endsWith("_VERTICAL_SPEED") || name.endsWith("_LAUNCH_Y"))) return 8.0D;
        if (name.startsWith("DRAGONS_") && (name.endsWith("_SPEED") || name.endsWith("_KNOCKBACK"))) return 8.0D;
        if (name.equals("DRAGONS_COWARD_LEAP_DANGER_FALL_BLOCKS")) return 128.0D;
        if (name.equals("DRAGONS_FROST_PATH_STEP")) return 4.0D;
        if (name.equals("DRAGON_ESCAPE_BLOCK_RADIUS") || name.equals("DRAGON_ESCAPE_DIGGER_NEARBY_PLAYER_RADIUS")
                || name.equals("DRAGON_ESCAPE_DISRUPTOR_TRIGGER_RADIUS")) return 64.0D;
        if (name.equals("DRAGON_ESCAPE_FINISH_RADIUS") || name.equals("DRAGON_ESCAPE_DUPLICATE_WAYPOINT_DISTANCE")) return 32.0D;
        if (name.equals("DRAGON_ESCAPE_DISRUPTOR_LAUNCH_Y")) return 5.0D;
        if (name.equals("CASTLE_ASSAULT_KING_HEALTH_BASE")) return 5_000.0D;
        if (name.equals("CASTLE_ASSAULT_KING_HEALTH_REFERENCE_PLAYERS")) return 64.0D;
        if (name.startsWith("CASTLE_ASSAULT_KILLSTREAK_TIER_")) return 1_000.0D;
        if (name.equals("CASTLE_ASSAULT_CARRIER_FUSE_MILLIS")) return 600_000.0D;
        if (name.equals("CASTLE_ASSAULT_TNT_FUSE_TICKS")) return 1_200.0D;
        if (name.equals("CASTLE_ASSAULT_TNT_BASE_RADIUS")
                || name.equals("CASTLE_ASSAULT_TNT_DEMOLITIONIST_RADIUS_BONUS")) return 32.0D;
        if (name.equals("CASTLE_ASSAULT_TNT_THROW_SPEED")) return 4.0D;
        if (name.equals("CASTLE_ASSAULT_TNT_THROW_VERTICAL_SPEED")) return 2.0D;
        if (name.equals("CASTLE_ASSAULT_TNT_MAX_DAMAGE")) return 40.0D;
        if (name.equals("CASTLE_ASSAULT_TNT_KNOCKBACK_MULTIPLIER")) return 3.0D;
        if (name.equals("CASTLE_ASSAULT_TNT_VERTICAL_KNOCKBACK")) return 2.0D;
        if (name.equals("CASTLE_ASSAULT_TNT_KING_DAMAGE")) return 500.0D;
        if (name.startsWith("PAINTBALL_") && name.endsWith("_MILLIS")) return 10_000.0D;
        if (name.equals("TNT_THROW_FUSE_TICKS")) return 1_200.0D;
        if (name.equals("TNT_THROW_COOLDOWN_MILLIS") || name.equals("ICE_BRIDGE_COOLDOWN_MILLIS")
                || name.equals("ICE_BRIDGE_DURATION_MILLIS") || name.equals("KIT_FIRE_COOLDOWN_MILLIS")
                || name.equals("METAL_MAGNET_COOLDOWN_MILLIS") || name.equals("EARTH_THROW_COOLDOWN_MILLIS")) return 120_000.0D;
        if (name.equals("TNT_THROW_STRENGTH") || name.equals("EARTH_THROW_SPEED")) return 8.0D;
        if (name.equals("ORE_ISLAND_RADIUS_BLOCKS")) return 32.0D;
        if (name.endsWith("_VEINS_PER_ISLAND")) return 32.0D;
        if (name.endsWith("_VEIN_SIZE")) return 16.0D;
        if (name.equals("RANDOM_VEIN_EXTRA_SIZE")) return 16.0D;
        if (name.equals("ICE_BRIDGE_LAUNCH_Y")) return 3.0D;
        if (name.equals("FIRE_BURST_RADIUS_BLOCKS") || name.equals("METAL_MAGNET_RANGE_BLOCKS")) return 64.0D;
        if (name.equals("FIRE_BURST_DAMAGE") || name.equals("METAL_MAGNET_DAMAGE")) return 40.0D;
        if (name.equals("FIRE_BURST_FIRE_TICKS")) return 2_400.0D;
        if (name.equals("METAL_MAGNET_PULL_BASE")) return 4.0D;
        if (name.equals("METAL_MAGNET_PULL_PER_ARMOR")) return 2.0D;
        if (name.equals("PAINTBALL_SHOTGUN_PELLETS")) return 32.0D;
        if (name.equals("DEATH_TAG_RUNNER_KILL_REWARD_GEMS")) return 64.0D;
        if (name.endsWith("_PAINT_PER_HIT") || name.endsWith("_SNIPER_BASE_PAINT")
                || name.endsWith("_SNIPER_MAX_EXTRA_PAINT")) return 4.0D;
        if (name.endsWith("_SPREAD")) return 1.0D;
        if (name.endsWith("_PROJECTILE_SPEED") || name.endsWith("_ARROW_SPEED")) return 30.0D;
        if (name.equals("PAINTBALL_SNIPER_SCOPE_CHARGE_PER_TICK")
                || name.equals("PAINTBALL_SNIPER_SCOPE_CHARGE_PER_EXTRA_PAINT")) return 1.0D;
        if (name.endsWith("_KNOCKBACK_MULTIPLIER")) return 3.0D;
        if (name.endsWith("_AMPLIFIER")) return 10.0D;
        if (name.endsWith("_TICKS")) return 1_200.0D;
        if (name.equals("MICRO_BATTLES_CHEAT_WINDOW_MILLIS")) return 30_000.0D;
        if (name.equals("MICRO_BATTLES_COMBAT_LOG_EXPIRE_MILLIS")) return 120_000.0D;
        if (name.equals("MICRO_BATTLES_BORDER_PARTICLE_DISTANCE")) return 16.0D;
        if (name.equals("MICRO_BATTLES_BORDER_PARTICLE_VERTICAL_POINTS")) return 8.0D;
        if (name.equals("MICRO_BATTLES_DASHER_JUMP_HORIZONTAL_SPEED")
                || name.equals("MICRO_BATTLES_DASHER_JUMP_VERTICAL_SPEED")) return 2.0D;
        if (name.equals("MINION_TARGET_RANGE_BLOCKS")) return 128.0D;
        if (name.equals("MINION_LAUNCH_SPEED")) return 4.0D;
        if (name.equals("MINION_SPEED_AMPLIFIER")) return 4.0D;
        if (name.equals("MINION_SPEED_DURATION_TICKS")) return 2_400.0D;
        if (name.equals("FIRE_STORM_CHARGE_PER_TICK") || name.equals("FIRE_STORM_DRAIN_PER_TICK")) return 0.25D;
        if (name.equals("FIRE_STORM_ARROWS_PER_TICK")) return 16.0D;
        if (name.equals("FIRE_STORM_ARROW_SPEED")) return 5.0D;
        if (name.equals("FIRE_STORM_SPREAD")) return 0.5D;
        if (name.equals("FLIGHT_MIN_SPEED") || name.equals("FLIGHT_SPAWN_HOVER_SPEED")
                || name.equals("FLIGHT_MAX_SPEED") || name.equals("FLIGHT_BASE_SPEED")) return 1.0D;
        if (name.equals("FLIGHT_SPEED_PER_Y_BLOCK")) return 0.1D;
        if (name.equals("FLIGHT_BLEND_START_BLOCKS")) return 32.0D;
        if (name.equals("FLIGHT_BLEND_END_BLOCKS")) return 64.0D;
        if (name.equals("FLIGHT_MAX_SPEED_CHANGE_PER_TICK")) return 0.25D;
        if (name.equals("HUMAN_ARROW_DAMAGE_MULTIPLIER")) return 2.0D;
        if (name.equals("SKULL_BLAST_RADIUS_BLOCKS")) return 32.0D;
        if (name.equals("SKULL_MIN_DAMAGE") || name.equals("SKULL_MAX_DAMAGE")) return 40.0D;
        if (name.equals("SKULL_KNOCKBACK_POWER")) return 5.0D;
        if (name.equals("SKULL_KNOCKBACK_Y_MULTIPLIER")) return 2.0D;
        if (name.equals("SKULL_KNOCKBACK_Y_MINIMUM")) return 3.0D;
        if (name.equals("SKULL_TERRAIN_RADIUS_BLOCKS")) return 8.0D;
        if (name.equals("SKULL_BLOCK_RESTORE_TICKS")) return 2_400.0D;
        if (name.equals("REBOUND_POWER_PER_BLOCK")) return 1.0D;
        if (name.equals("REBOUND_VERTICAL_ARC_PER_BLOCK")) return 0.5D;
        if (name.equals("REBOUND_BASE_POWER") || name.equals("REBOUND_MAX_POWER")) return 5.0D;
        if (name.equals("REBOUND_MAX_IMPACT_DISTANCE_SQUARED")) return 16.0D;
        if (name.equals("GROUND_POUND_RADIUS_BLOCKS")) return 32.0D;
        if (name.equals("GROUND_POUND_HORIZONTAL_STRENGTH")) return 4.0D;
        if (name.equals("GROUND_POUND_VERTICAL_BASE") || name.equals("GROUND_POUND_VERTICAL_PROXIMITY")) return 4.0D;
        if (name.equals("LEAPER_JUMP_HORIZONTAL") || name.equals("LEAPER_JUMP_VERTICAL_ADD")
                || name.equals("LEAPER_JUMP_VERTICAL_MIN") || name.equals("LEAPER_JUMP_VERTICAL_MAX")) return 2.0D;
        if (name.equals("BRAWLER_HEALTH_PER_KILL")) return 20.0D;
        if (name.equals("SAFE_PAD_CAPTURE_RADIUS_BLOCKS")) return 2.0D;
        if (name.equals("MOB_MOVE_SPEED")) return 2.0D;
        if (name.equals("BUMP_RADIUS_BLOCKS")) return 5.0D;
        if (name.equals("SMOKE_ARROW_BLIND_RADIUS_BLOCKS")) return 32.0D;
        if (name.equals("SMOKE_ARROW_MAX_LIFETIME_TICKS")) return 2_400.0D;
        if (name.equals("EDITOR_BUILD_DEPTH_BELOW_MARKER")) return 32.0D;
        if (name.equals("MONSTER_HEALTH")) return 40.0D;
        if (name.equals("BUMP_DAMAGE")) return 20.0D;
        if (name.equals("BUMP_KNOCKBACK_STRENGTH") || name.equals("BODY_RUSH_STRENGTH")) return 3.0D;
        if (name.equals("BUMP_KNOCKBACK_Y_ADD") || name.equals("BODY_RUSH_Y_ADD")) return 3.0D;
        if (name.equals("BUMP_KNOCKBACK_Y_MAX") || name.equals("BODY_RUSH_Y_MAX")) return 5.0D;
        if (name.equals("INITIAL_MONSTER_COUNT")) return 1_000.0D;
        if (name.equals("ADDITIONAL_MONSTERS_PER_SAFE_PAD")) return 500.0D;
        if (name.endsWith("_SECONDS")) return 600.0D;
        if (name.endsWith("_X") || name.endsWith("_Y") || name.endsWith("_Z")) return 512.0D;
        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.contains("COUNT") || upper.contains("MAX_") || upper.contains("PLAYERS")
                || upper.contains("SLOTS") || upper.contains("ATTEMPTS") || upper.contains("AMMO")
                || upper.contains("ARROWS") || upper.contains("WOOL") || upper.contains("WAVES")
                || upper.contains("STAGES") || upper.contains("LEVEL") || upper.contains("KILLS")
                || upper.contains("CHARGES") || upper.contains("USES")) return 64.0D;
        if (kind == Kind.INTEGER) return 1_000.0D;
        if (kind == Kind.LONG) return 86_400_000.0D;
        if (Math.abs(defaultValue) <= 1.0D) return 32.0D;
        return Math.max(100.0D, Math.min(100_000.0D, Math.abs(defaultValue) * 100.0D));
    }

    private static double step(String name, double defaultValue, Kind kind) {
        if (name.startsWith("SUPER_SPLOOR_")) {
            if (name.endsWith("_MILLIS")) return 100.0D;
            if (name.endsWith("_BLOCKS") || name.endsWith("_SPEED") || name.endsWith("_VELOCITY")
                    || name.endsWith("_PERCENT")) return 0.1D;
            return 1.0D;
        }
        if (kind == Kind.BOOLEAN) return 1.0D;
        if (Set.of("SPOTLIGHT_INTERVAL_MILLIS", "SPOTLIGHT_GLOW_DURATION_MILLIS", "DECOY_FLASH_LIFETIME_MILLIS",
                "SMOKE_LIFETIME_MILLIS", "SMOKE_ESCAPE_CREDIT_WINDOW_MILLIS", "PURSUIT_ESCAPE_MILLIS",
                "SMOKE_ABILITY_COOLDOWN_MILLIS", "DECOY_ABILITY_COOLDOWN_MILLIS",
                "RUNNER_ABILITY_COOLDOWN_MILLIS", "SPOTTER_ABILITY_COOLDOWN_MILLIS",
                "UNDERSTUDY_ABILITY_COOLDOWN_MILLIS", "RUNNER_ABILITY_DURATION_MILLIS",
                "RUNNER_ACHIEVEMENT_WINDOW_MILLIS", "SPOTTER_MARK_DURATION_MILLIS",
                "STAGE_FRIGHT_DURATION_MILLIS").contains(name)) return 100.0D;
        if (Set.of("PURSUIT_ESCAPE_DISTANCE_BLOCKS", "SPOTTER_RANGE_BLOCKS", "DECOY_FORWARD_OFFSET_BLOCKS",
                "UNDERSTUDY_FORWARD_OFFSET_BLOCKS", "DECOY_APPROACH_RANGE_BLOCKS", "DECOY_LOOK_RANGE_BLOCKS",
                "SMOKE_OCCLUSION_RADIUS_BLOCKS").contains(name)) return 0.1D;
        if (name.equals("DECOY_LOOK_MIN_DOT")) return 0.01D;
        if (name.equals("RUNNER_SPEED_AMPLIFIER") || name.equals("OUT_OF_FOCUS_WINDOW_TICKS")
                || Set.of("GROUP_PHOTO_MIN_TARGETS", "PAPARAZZI_UNIQUE_PHOTOS_REQUIRED",
                "PRIVATE_INVESTIGATOR_TARGETS_REQUIRED", "FALSE_ALARM_TARGETS_REQUIRED",
                "ESCAPE_STREAK_REQUIRED", "SMOKE_ESCAPE_STREAK_REQUIRED", "DOUBLE_ELIMINATION_STREAK_REQUIRED",
                "REVENGE_KILLS_REQUIRED", "UNTOUCHABLE_WINS_REQUIRED", "NARROW_ESCAPE_EXPOSURES",
                "PICTURE_PERFECT_PHOTOS_REQUIRED", "STAGE_FRIGHT_EXPOSURES").contains(name)) return 1.0D;
        if (name.endsWith("_SWORD_DAMAGE") || name.startsWith("PROJECTILE_DAMAGE_")
                || name.startsWith("NPC_") && (name.endsWith("_SPEED") || name.endsWith("_DAMAGE")
                || name.endsWith("_HORIZONTAL") || name.endsWith("_VERTICAL")
                || name.equals("NPC_MOTION_FACTOR"))
                || name.equals("POWERUP_PICKUP_RADIUS_BLOCKS")) return 0.1D;
        if (name.endsWith("_RADIUS_BLOCKS")) return kind == Kind.INTEGER || kind == Kind.LONG ? 1.0D : 0.1D;
        if (name.equals("NPC_PATROL_ORDER_CHANCE_DENOMINATOR")
                || name.equals("NPC_PATROL_WAYPOINT_ATTEMPTS") || name.equals("NPC_SPAWN_SEARCH_ATTEMPTS")
                || name.endsWith("_SMOKE_BOMBS") || name.endsWith("_STARTING_ARROWS")
                || name.endsWith("_STARTING_EMERALDS") || name.endsWith("_STARTING_DIAMONDS")
                || Set.of("POWERUP_BONUS_HEALTH", "POWERUP_MAX_HEALTH", "KILL_BONUS_HEALTH").contains(name)
                || name.equals("MAX_POWERUP_PROGRESS")
                || name.equals("MASTER_ASSASSIN_POWERUPS_REQUIRED")
                || name.equals("MASTERS_MASTER_POWERUPS_REQUIRED")
                || name.equals("PROSECUTOR_DISTINCT_TARGETS_REQUIRED")
                || name.equals("DUELS_OTHER_MASTERS_REQUIRED")) return 1.0D;
        if (Set.of("GLOBAL_REVEAL_DURATION_MILLIS", "SMOKE_BLINDNESS_DURATION_MILLIS",
                "NPC_ATTACK_COOLDOWN_MILLIS").contains(name)) return 100.0D;
        if ((name.endsWith("_RADIUS") || name.endsWith("_SPACING"))
                && (kind == Kind.INTEGER || kind == Kind.LONG)) return 1.0D;
        if (name.startsWith("ROULETTE_HIT_") && name.endsWith("_SECONDS")) return 1.0D;
        if (name.endsWith("_DEGREES_PER_TICK") || name.endsWith("_TURN_DEGREES")
                || name.equals("WEAK_TURN_THRESHOLD_DEGREES")) return 1.0D;
        if (name.equals("MOVEMENT_SOURCE_BASE_MULTIPLIER") || name.equals("MOVEMENT_GROWTH_CAP")
                || name.equals("MOVEMENT_SPEED_BOOST_MULTIPLIER") || name.equals("MOVEMENT_CONVERSION_SCALE")
                || name.equals("MOVEMENT_CONVERSION_BASE") || name.equals("OVERSHOOT_TURN_MULTIPLIER")
                || name.endsWith("_RADIUS") || name.endsWith("_SPACING")) return 0.05D;
        if (name.equals("BACON_CHRIS_ABILITY_METER_COST")) return 5.0D;
        if (name.equals("BACON_CRISPY_SHOT_COUNT")) return 1.0D;
        if (name.equals("BACON_CRISPY_SHOT_SPREAD_RADIANS")) return 0.01D;
        if (name.equals("BACON_CRISPY_SHOT_VELOCITY")) return 0.1D;
        if (name.equals("BALL_RADIUS")) return 0.05D;
        if (name.equals("BATTING_POWER_MULTIPLIER")) return 0.05D;
        if (name.equals("CURVE_ACCELERATION")) return 0.0005D;
        if (Set.of("SLUGGER_DIVE_SPEED", "SPRINTER_BURST_SPEED", "SKYGUARD_LEAP_SPEED",
                "CURVE_ARTIST_SIDE_SPEED", "CHARGER_MIN_SPEED", "CHARGER_MAX_SPEED",
                "SQUID_WALK_SPEED", "SQUID_CLIMB_VELOCITY").contains(name)) return 0.05D;
        if (name.endsWith("_PRIMARY_INTERVAL_MILLIS") || name.equals("SECONDARY_COOLDOWN_MILLIS")
                || name.equals("CHARGER_FULL_CHARGE_MILLIS") || name.equals("INK_REGEN_INTERVAL_MILLIS")) return 100.0D;
        if (name.startsWith("SPECIAL_CHARGE_PER_")) return 0.1D;
        if (name.endsWith("_INK_REGEN_PER_SECOND")) return 1.0D;
        if (name.startsWith("CHARGER_") && name.endsWith("_SPEED")) return 0.1D;
        if (name.startsWith("EVOLUTION_CHARGE_")) return 0.001D;
        if (name.startsWith("SKY_WARS_") && name.endsWith("_MILLIS")) return 100.0D;
        if (name.startsWith("COW_BOMB_") || name.startsWith("BODY_SLAM_")
                || name.equals("ASSIST_CREDIT_WINDOW_MILLIS")) {
            if (name.endsWith("_MILLIS")) return 100.0D;
            if (name.endsWith("_RADIUS_BLOCKS") || name.endsWith("_HEIGHT_BLOCKS")) return 0.1D;
            return 0.1D;
        }
        if (name.startsWith("FARMER_DOUBLE_JUMP_") || name.equals("COW_CHARGE_IMPACT_VERTICAL")) return 0.1D;
        if (name.equals("COMEBACK_LEAD_THRESHOLD") || name.equals("GOT_THE_CHOPS_AIRBORNE_KILLS")
                || name.equals("SLAUGHTERHOUSE_ASSISTS") || name.equals("PERFECT_GAME_MAX_FARMER_DEATHS")) return 1.0D;
        if (name.startsWith("RUNNER_") && name.endsWith("_SPEED")) return 0.1D;
        if (name.equals("RUNNER_SWITCH_GRACE_MILLIS")) return 50.0D;
        if (name.equals("RUNNER_STEPS_ACHIEVEMENT_BLOCKS")) return 100.0D;
        if (name.equals("RUNNER_HOP_ACHIEVEMENT_JUMPS")) return 5.0D;
        if (name.startsWith("SHEEP_QUEST_") && name.endsWith("_MILLIS")) return 100.0D;
        if (name.startsWith("SHEEP_QUEST_") && (name.endsWith("_SPEED") || name.endsWith("_RADIUS")
                || name.endsWith("_BLOCKS") || name.endsWith("_KNOCKBACK"))) return 0.1D;
        if (name.startsWith("SHEEP_QUEST_") && (name.endsWith("_THRESHOLD")
                || name.endsWith("_ACHIEVEMENT_COUNT") || name.endsWith("_WINS")
                || name.endsWith("_PER_TARGET") || name.endsWith("_AMPLIFIER")
                || name.endsWith("_SHEEP_PER_LEVEL") || name.endsWith("_SCORE"))) return 1.0D;
        if (name.startsWith("SKY_WARS_") && (name.endsWith("_DIRT") || name.endsWith("_PER_TICK")
                || name.endsWith("_KILLS") || name.endsWith("_PLAYERS"))) return 1.0D;
        if (name.endsWith("_CHANCE")) return 0.05D;
        if (Set.of("MANA_REGEN_MULTIPLIER", "PROJECTILE_SPEED_MULTIPLIER", "SPELL_COOLDOWN_MULTIPLIER",
                "SPELL_DAMAGE_MULTIPLIER", "SPELL_HEAL_MULTIPLIER", "SPELL_MANA_COST_MULTIPLIER")
                .contains(name)) return 0.1D;
        if (name.contains("_FALL_DAMAGE_MULTIPLIER")) return 0.05D;
        if (name.endsWith("_MILLIS")) return defaultValue >= 10_000.0D ? 1_000.0D : defaultValue >= 1_000.0D ? 100.0D : 25.0D;
        if (name.endsWith("_TICKS")) return defaultValue >= 100.0D ? 20.0D : 1.0D;
        if (kind == Kind.INTEGER || Math.abs(defaultValue) >= 10.0D) return 1.0D;
        return Math.max(0.05D, Math.abs(defaultValue) / 10.0D);
    }

    static String humanize(String key) {
        if ("GLADIATOR_MODE_OVERRIDE".equals(key)) return "mode override (0 Normal, 1 Rodless, 2 Bowless, 3 Modern, 4 Pot)";
        if ("GLADIATOR_STARTING_TIER_OVERRIDE".equals(key)) return "starting tier override (-1 auto, 0-3 arena tier)";
        return key.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
