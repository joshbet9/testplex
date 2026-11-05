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
                    "TEMPLATE_MOBS_ENABLED")
                    .contains(name);
        }
        if (game == ArcadePortGame.WIZARDS) {
            return Set.of("SOUL_STAR_MANA_PER_SECOND", "MAGE_RANDOM_STARTER_SPELL_COUNT",
                    "MAGE_STARTER_SPELL_SELECTION_ATTEMPTS", "FULL_WAND_CONVERSION",
                    "MIN_PLAYERS", "MAX_PLAYERS", "WAND_SLOT_COUNT", "ENDGAME_START_MINUTES", "ENDGAME_INITIAL_STRIKE_MILLIS",
                    "ENDGAME_MIN_STRIKE_MILLIS", "ENDGAME_STRIKE_REDUCTION_PER_TICK_MILLIS").contains(name);
        }
        if (game == ArcadePortGame.WITHER_ASSAULT) return Set.of("MINIONS_PER_WAVE").contains(name);
        if (game == ArcadePortGame.HOOPS_REMASTERED) return Set.of("TARGET_SCORE", "REGULATION_MILLIS",
                "SHOT_CLOCK_MILLIS", "HOPS_COOLDOWN_MILLIS", "OFFENSIVE_REBOUND_SHOT_CLOCK_MILLIS",
                "THREE_POINT_DISTANCE", "STEAL_RADIUS", "BLOCK_RADIUS", "BLOCK_COOLDOWN_MILLIS",
                "RIM_SCORING_RADIUS", "CLUTCH_WINDOW_MILLIS").contains(name);
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
        if (name.equals("SAFE_PAD_CAPTURE_RADIUS_BLOCKS")) return 2.0D;
        if (name.equals("MOB_MOVE_SPEED")) return 2.0D;
        if (name.equals("BUMP_RADIUS_BLOCKS")) return 5.0D;
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
        if (name.endsWith("_MILLIS")) return defaultValue >= 10_000.0D ? 1_000.0D : defaultValue >= 1_000.0D ? 100.0D : 25.0D;
        if (name.endsWith("_TICKS")) return defaultValue >= 100.0D ? 20.0D : 1.0D;
        if (kind == Kind.INTEGER || Math.abs(defaultValue) >= 10.0D) return 1.0D;
        return Math.max(0.05D, Math.abs(defaultValue) / 10.0D);
    }

    static String humanize(String key) {
        return key.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
