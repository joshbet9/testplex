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
            "SPLEEF_HUNGER_DRAIN_ENABLED");

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
            boolean dynamicConsumer = game.isMonsterMaze() || game == ArcadePortGame.SNEAKY_ASSASSINS;
            if (!GameDebugWiredRuleKeys.KEYS.contains(key) && !dynamicConsumer) continue;
            Kind kind = kind(field.getType());
            if (kind == null) continue;
            try {
                Object raw = field.get(null);
                double value = raw instanceof Boolean enabled ? (enabled ? 1.0D : 0.0D)
                        : ((Number) raw).doubleValue();
                result.add(new Variable(key, humanize(name), kind, value,
                        minimum(name, value, kind), maximum(name, kind, value), step(name, value, kind)));
            } catch (IllegalAccessException ignored) {
                // Public constants should be readable; ignore a field if a JVM denies access.
            }
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
        return variable != null && variable.kind() == Kind.BOOLEAN
                && LIVE_BOOLEAN_RULES.contains(variable.key());
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
                    "TEMPLATE_MOBS_ENABLED", "PERFECT_SCORE_PERCENT")
                    .contains(name);
        }
        if (game == ArcadePortGame.WIZARDS) {
            return Set.of("SOUL_STAR_MANA_PER_SECOND", "MAGE_RANDOM_STARTER_SPELL_COUNT",
                    "MAGE_STARTER_SPELL_SELECTION_ATTEMPTS", "FULL_WAND_CONVERSION",
                    "MIN_PLAYERS", "MAX_PLAYERS", "WAND_SLOT_COUNT", "ENDGAME_START_MINUTES", "ENDGAME_INITIAL_STRIKE_MILLIS",
                    "ENDGAME_MIN_STRIKE_MILLIS", "ENDGAME_STRIKE_REDUCTION_PER_TICK_MILLIS").contains(name);
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
                "SKULL_KNOCKBACK_Y_MINIMUM", "SKULL_BLOCK_RESTORE_TICKS").contains(name);
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
                "SHOT_NORMAL_FLIGHT_TICKS_PER_BLOCK", "POSSESSION_MOVEMENT_DAMPING").contains(name);
        if (game == ArcadePortGame.SNEAKY_ASSASSINS) return SneakyAssassinsRules.debuggableRule(name);
        if (game.isMonsterMaze()) {
            return Set.of("MOB_MOVE_SPEED", "BODY_RUSH_DURATION_MILLIS", "CRYO_COOLDOWN_MILLIS",
                    "CRYO_FREEZE_MILLIS", "CRYO_RADIUS_BLOCKS", "JUMP_STRENGTH",
                    "STAGE_TIMER_INITIAL_SECONDS", "STAGE_TIMER_DECREASE_PER_STAGE_SECONDS", "STAGE_TIMER_MIN_SECONDS",
                    "INITIAL_MONSTER_COUNT", "ADDITIONAL_MONSTERS_PER_SAFE_PAD", "MONSTER_HEALTH",
                    "PLAYER_BUMP_COOLDOWN_MILLIS", "BUMP_RADIUS_BLOCKS", "BUMP_DAMAGE",
                    "BUMP_KNOCKBACK_STRENGTH", "BUMP_KNOCKBACK_Y_ADD", "BUMP_KNOCKBACK_Y_MAX",
                    "BODY_RUSH_STRENGTH", "BODY_RUSH_Y_ADD", "BODY_RUSH_Y_MAX",
                    "SAFE_PAD_DECAY_STAGES", "SAFE_PAD_CAPTURE_RADIUS_BLOCKS").contains(name);
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
        if (name.contains("_FALL_DAMAGE_MULTIPLIER") || name.startsWith("EVOLUTION_CHARGE_")) return 0.0D;
        if (name.contains("_MAX_HEALTH")) return 1.0D;
        if (name.contains("_MELEE_DAMAGE")) return 0.0D;
        if (name.equals("HOOPS_MIN_PLAYERS")) return 2.0D;
        if (name.equals("HOOPS_MAX_PLAYERS")) return 2.0D;
        if (name.equals("CASTLE_ASSAULT_KING_HEALTH_BASE")
                || name.equals("CASTLE_ASSAULT_KING_HEALTH_REFERENCE_PLAYERS")) return 1.0D;
        if (name.equals("CASTLE_ASSAULT_TNT_THROW_SPEED")
                || name.equals("CASTLE_ASSAULT_TNT_THROW_VERTICAL_SPEED")) return 0.0D;
        if (name.equals("PAINTBALL_SHOTGUN_PELLETS")) return 1.0D;
        if (name.equals("PERFECT_SCORE_PERCENT")) return 1.0D;
        if (name.equals("RUNNER_MAX_FROSTBALLS")) return 0.0D;
        if (name.equals("SHEEP_QUEST_MAX_CARRY")) return 0.0D;
        if (name.startsWith("RUNNER_") && name.endsWith("_MILLIS")) return 0.0D;
        if (name.startsWith("RUNNER_") && name.endsWith("_TICKS")) return 0.0D;
        if (name.startsWith("SHEEP_QUEST_") && (name.endsWith("_MILLIS") || name.endsWith("_TICKS"))) return 0.0D;
        if (name.equals("SHEEP_QUEST_THROW_BOUNCE_MULTIPLIER")) return -1.0D;
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

    private static double maximum(String name, Kind kind, double defaultValue) {
        if (kind == Kind.BOOLEAN) return 1.0D;
        if (name.contains("_FALL_DAMAGE_MULTIPLIER")) return 1.0D;
        if (name.contains("_MAX_HEALTH")) return 80.0D;
        if (name.contains("_MELEE_DAMAGE")) return 40.0D;
        if (name.startsWith("EVOLUTION_CHARGE_")) return 0.2D;
        if (name.startsWith("PAINTBALL_") && name.endsWith("_KNOCKBACK_MULTIPLIER")) return 3.0D;
        if (name.contains("_KNOCKBACK")) return 8.0D;
        if (name.contains("_LAUNCH_SPEED") || name.contains("_LAUNCH_HORIZONTAL")) return 8.0D;
        if (name.contains("_LAUNCH_VERTICAL") || name.endsWith("_EGG_SPEED")) return 5.0D;
        if (name.equals("HOOPS_MIN_PLAYERS") || name.equals("HOOPS_MAX_PLAYERS")) return 16.0D;
        if (name.equals("PERFECT_SCORE_PERCENT")) return 100.0D;
        if (name.startsWith("RUNNER_") && name.endsWith("_SPEED")) return 4.0D;
        if (name.equals("RUNNER_SWITCH_GRACE_MILLIS")) return 5_000.0D;
        if (name.equals("RUNNER_BLOCK_DECAY_MILLIS")) return 60_000.0D;
        if (name.equals("RUNNER_FROSTBALL_INTERVAL_MILLIS")) return 10_000.0D;
        if (name.equals("RUNNER_LEAP_COOLDOWN_MILLIS")
                || name.equals("RUNNER_QUICKSHOT_COOLDOWN_MILLIS")) return 120_000.0D;
        if (name.equals("RUNNER_MAX_FROSTBALLS")) return 64.0D;
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
        if (name.startsWith("SHEEP_QUEST_") && name.endsWith("_SPEED")) return 4.0D;
        if (name.startsWith("SHEEP_QUEST_") && (name.endsWith("_RADIUS") || name.endsWith("_BLOCKS"))) return 64.0D;
        if (name.equals("CASTLE_ASSAULT_KING_HEALTH_BASE")) return 5_000.0D;
        if (name.equals("CASTLE_ASSAULT_KING_HEALTH_REFERENCE_PLAYERS")) return 64.0D;
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
        if (kind == Kind.BOOLEAN) return 1.0D;
        if (name.startsWith("EVOLUTION_CHARGE_")) return 0.001D;
        if (name.startsWith("SKY_WARS_") && name.endsWith("_MILLIS")) return 100.0D;
        if (name.startsWith("RUNNER_") && name.endsWith("_SPEED")) return 0.1D;
        if (name.equals("RUNNER_SWITCH_GRACE_MILLIS")) return 50.0D;
        if (name.startsWith("SHEEP_QUEST_") && name.endsWith("_MILLIS")) return 100.0D;
        if (name.startsWith("SHEEP_QUEST_") && (name.endsWith("_SPEED") || name.endsWith("_RADIUS")
                || name.endsWith("_BLOCKS") || name.endsWith("_KNOCKBACK"))) return 0.1D;
        if (name.contains("_FALL_DAMAGE_MULTIPLIER")) return 0.05D;
        if (name.endsWith("_MILLIS")) return defaultValue >= 10_000.0D ? 1_000.0D : defaultValue >= 1_000.0D ? 100.0D : 25.0D;
        if (name.endsWith("_TICKS")) return defaultValue >= 100.0D ? 20.0D : 1.0D;
        if (kind == Kind.INTEGER || Math.abs(defaultValue) >= 10.0D) return 1.0D;
        return Math.max(0.05D, Math.abs(defaultValue) / 10.0D);
    }

    static String humanize(String key) {
        return key.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
