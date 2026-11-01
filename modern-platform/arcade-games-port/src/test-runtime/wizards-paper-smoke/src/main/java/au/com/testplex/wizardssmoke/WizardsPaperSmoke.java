package au.com.testplex.wizardssmoke;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Disposable Paper 26.3 acceptance harness:
 *  - loads every source snapshot through WizardsGameManager.createArena();
 *  - then waits for three real protocol clients and starts a three-player Wizards round;
 *  - verifies kit/spawn/loot state, real eliminations, spectator respawn, progression and restoration.
 *
 * This plugin is only packaged in the temporary CI smoke server, never in Testplex jars.
 */
public final class WizardsPaperSmoke extends JavaPlugin implements Listener {
    private static final List<String> POOLS = List.of("Wizards", "Nano Wizards");
    private static final List<String> MAPS = List.of(
            "haven", "wizardarena", "orbisharbor", "aspen", "hogwarts",
            "industrialwest", "alicudi", "thaumaturgecolony", "magicallypossible", "kalamit");
    private static final String GAMEPLAY_MAP = "source-maps/Wizards/haven.zip";
    private static final String OWNER_NAME = "jn_playzmc";
    private static final String RIVAL_NAME = "WizardSmoke2";
    private static final String THIRD_NAME = "WizardSmoke3";
    private static final int MAX_CLIENT_WAIT_TICKS = 120 * 20 / 5;
    private static final int MAX_START_WAIT_TICKS = 60 * 20 / 5;

    private final List<String> results = new ArrayList<>();
    private Plugin arcade;
    private Object wizards;
    private BukkitTask gameplayTask;
    private Player owner;
    private Player rival;
    private Player third;
    private World gameplayArena;
    private PlayerBaseline ownerBaseline;
    private PlayerBaseline rivalBaseline;
    private PlayerBaseline thirdBaseline;
    private int gameplayStage;
    private int gameplayTicks;
    private boolean shuttingDown;
    private String lastDamageObservation = "no damage event observed";
    private String lastAnyDamageObservation = "no entity damage event observed";

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTask(this, this::runAll);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void observeWizardsDamage(EntityDamageEvent event) {
        if (rival == null || !rival.equals(event.getEntity())) return;
        lastAnyDamageObservation = "cause=" + event.getCause()
                + ",damage=" + event.getDamage()
                + ",final=" + event.getFinalDamage()
                + ",cancelled=" + event.isCancelled()
                + ",victimHealth=" + rival.getHealth()
                + ",victimMode=" + rival.getGameMode()
                + ",victimInvulnerable=" + readMarkedInvulnerability(rival)
                + ",noDamageTicks=" + rival.getNoDamageTicks()
                + ",lastDamage=" + rival.getLastDamage();
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            lastDamageObservation = "damager=" + byEntity.getDamager().getType()
                    + ",damage=" + byEntity.getDamage()
                    + ",final=" + byEntity.getFinalDamage()
                    + ",cancelled=" + byEntity.isCancelled()
                    + ",victimHealth=" + rival.getHealth()
                    + ",victimMode=" + rival.getGameMode()
                    + ",victimInvulnerable=" + readMarkedInvulnerability(rival)
                    + ",noDamageTicks=" + rival.getNoDamageTicks();
        }
    }

    private void runAll() {
        try {
            arcade = Bukkit.getPluginManager().getPlugin("ArcadeMechanicsModern");
            require(arcade != null && arcade.isEnabled(), "ArcadeMechanicsModern is not enabled");
            wizards = field(arcade, "wizards");
            require(wizards != null, "The shared Arcade Wizards controller was not initialised");
            Method createArena = method(wizards.getClass(), "createArena");

            int total = POOLS.size() * MAPS.size();
            for (String pool : POOLS) {
                for (String map : MAPS) {
                    String resource = "source-maps/" + pool + "/" + map + ".zip";
                    long started = System.nanoTime();
                    try {
                        arcade.getConfig().set("selected-maps.wizards", resource);
                        arcade.saveConfig();

                        Object value = createArena.invoke(wizards);
                        require(value instanceof World, "createArena() did not return a loaded world");
                        World world = (World) value;

                        String activeResource = (String) field(wizards, "activeMapResource");
                        require(resource.equals(activeResource),
                                "source archive was rejected or generated-arena fallback was used (active="
                                        + activeResource + ")");

                        @SuppressWarnings("unchecked")
                        List<Location> starts = (List<Location>) field(wizards, "sourceSpawnLocations");
                        @SuppressWarnings("unchecked")
                        List<Location> chests = (List<Location>) field(wizards, "chests");
                        require(starts != null && starts.size() >= 4,
                                "only " + (starts == null ? 0 : starts.size()) + " terrain-safe starts were recovered");
                        require(chests != null && chests.size() >= 8,
                                "only " + (chests == null ? 0 : chests.size()) + " usable chest locations were recovered");

                        validateStarts(world, starts);
                        validateChests(world, chests);
                        double borderSize = world.getWorldBorder().getSize();
                        require(borderSize >= 32.0D && borderSize <= 8192.0D,
                                "invalid or implausible world border size: " + borderSize);
                        require("arcadeport_source_wizards".equals(world.getName()),
                                "source map loaded into unexpected world " + world.getName());

                        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
                        record("PASS", resource, starts.size(), chests.size(), elapsedMs, "loaded");
                        getLogger().info("WIZARDS_SOURCE_MAP_SMOKE|PASS|" + resource
                                + "|starts=" + starts.size() + "|chests=" + chests.size()
                                + "|border=" + Math.round(borderSize) + "|ms=" + elapsedMs);
                    } catch (Throwable failure) {
                        Throwable cause = rootCause(failure);
                        String message = cause.getClass().getSimpleName() + ": " + safeMessage(cause);
                        record("FAIL", resource, 0, 0, (System.nanoTime() - started) / 1_000_000L, message);
                        getLogger().severe("WIZARDS_SOURCE_MAP_SMOKE|FAIL|" + resource + "|" + message);
                    }
                }
            }

            long passed = results.stream().filter(line -> line.startsWith("PASS\t")).count();
            long failed = results.stream().filter(line -> line.startsWith("FAIL\t")).count();
            getLogger().info("WIZARDS_SOURCE_MAP_SMOKE|SUMMARY|passed=" + passed
                    + "|failed=" + failed + "|total=" + total);
            require(passed == total && failed == 0,
                    "one or more source maps failed before player-lifecycle acceptance");
            beginGameplaySmoke();
        } catch (Throwable failure) {
            failAndShutdown("HARNESS_FAIL", "harness", failure);
        }
    }

    private void beginGameplaySmoke() {
        arcade.getConfig().set("selected-maps.wizards", GAMEPLAY_MAP);
        arcade.saveConfig();
        getLogger().info("WIZARDS_GAMEPLAY_SMOKE|WAITING|three offline protocol clients ("
                + OWNER_NAME + ", " + RIVAL_NAME + " and " + THIRD_NAME + ")");
        gameplayTask = Bukkit.getScheduler().runTaskTimer(this, this::tickGameplaySmoke, 20L, 5L);
    }

    private void tickGameplaySmoke() {
        if (shuttingDown) return;
        gameplayTicks++;
        if (gameplayStage == 0 && gameplayTicks % 200 == 0) {
            getLogger().info("WIZARDS_GAMEPLAY_SMOKE|WAIT_PROGRESS|ticks=" + gameplayTicks
                    + "|online=" + Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        try {
            switch (gameplayStage) {
                case 0 -> waitForClientsAndQueue();
                case 1 -> verifyLiveRoundAndEliminateFirstOpponent();
                case 2 -> verifySpectatorAndEliminateFinalOpponent();
                case 3 -> verifyPlayerRestorationAndFinish();
                case 4 -> {
                    if (gameplayTicks >= 5 && paperDamageReady(rival)) {
                        runGenericDamageProbe();
                    } else if (gameplayTicks >= 100) {
                        failAndShutdown("GAMEPLAY_FAIL", "lifecycle",
                                new IllegalStateException("Paper client is not damage-ready after teleport/load handshake: "
                                        + nmsDamageState(rival)));
                    }
                }
                case 5 -> { if (gameplayTicks >= 5) runAttributedDamageProbe(); }
                case 6 -> { if (gameplayTicks >= 5) runRainbowBeamAcceptance(); }
                default -> failAndShutdown("GAMEPLAY_FAIL", "lifecycle",
                        new IllegalStateException("unknown gameplay smoke stage " + gameplayStage));
            }
        } catch (Throwable failure) {
            failAndShutdown("GAMEPLAY_FAIL", "lifecycle", rootCause(failure));
        }
    }

    private void waitForClientsAndQueue() throws Exception {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        owner = online.stream().filter(player -> OWNER_NAME.equalsIgnoreCase(player.getName())).findFirst().orElse(null);
        rival = online.stream().filter(player -> RIVAL_NAME.equalsIgnoreCase(player.getName())).findFirst().orElse(null);
        third = online.stream().filter(player -> THIRD_NAME.equalsIgnoreCase(player.getName())).findFirst().orElse(null);
        if (owner == null || rival == null || third == null) {
            if (gameplayTicks >= MAX_CLIENT_WAIT_TICKS) {
                throw new IllegalStateException("timed out waiting for three real clients; online="
                        + online.stream().map(Player::getName).toList());
            }
            return;
        }

        // Reset through the host manager, exactly as the operator Stop Game action does.
        Object arcadeManager = field(arcade, "manager");
        method(arcadeManager.getClass(), "stop", String.class).invoke(
                arcadeManager, "Wizards Paper smoke setup");
        seedBaselineProfile(owner, 0);
        seedBaselineProfile(rival, 1);
        seedBaselineProfile(third, 2);
        ownerBaseline = captureBaseline(owner);
        rivalBaseline = captureBaseline(rival);
        thirdBaseline = captureBaseline(third);
        require(GAMEPLAY_MAP.equals(arcade.getConfig().getString("selected-maps.wizards")),
                "gameplay map selection was not persisted");
        require(!arcade.getConfig().getBoolean("wizards.allow-solo-start", true),
                "one-player Wizards starts must remain disabled");
        require(Bukkit.dispatchCommand(owner, "arcadeport join wizards"),
                "owner could not join the Wizards queue");
        require(Bukkit.dispatchCommand(rival, "arcadeport join wizards"),
                "second client could not join the Wizards queue");
        require(Bukkit.dispatchCommand(third, "arcadeport join wizards"),
                "third client could not join the Wizards queue");
        require(Bukkit.dispatchCommand(owner, "arcadeport kit wizards mage"),
                "Mage kit selection command was rejected");
        require(Bukkit.dispatchCommand(rival, "arcadeport kit wizards sorcerer"),
                "Sorcerer kit selection command was rejected");
        require(Bukkit.dispatchCommand(third, "arcadeport kit wizards mystic"),
                "Mystic kit selection command was rejected");

        Class<?> arcadeGameType = Class.forName("au.com.testplex.arcadeports.ArcadePortGame");
        Object wizardsGameId = arcadeGameType.getField("WIZARDS").get(null);
        int queued = (Integer) method(arcadeManager.getClass(), "queuedCount", arcadeGameType)
                .invoke(arcadeManager, wizardsGameId);
        require(queued == 3, "expected three players in the shared Arcade queue, found " + queued);
        require(Bukkit.dispatchCommand(owner, "arcadeport start wizards immediate"),
                "owner could not immediately start Wizards through the shared Arcade queue");
        gameplayStage = 1;
        gameplayTicks = 0;
        getLogger().info("WIZARDS_GAMEPLAY_SMOKE|QUEUED|players=" + owner.getName() + ","
                + rival.getName() + "," + third.getName()
                + "|map=" + GAMEPLAY_MAP + "|kits=MAGE,SORCERER,MYSTIC");
    }

    private void verifyLiveRoundAndEliminateFirstOpponent() throws Exception {
        boolean running = (Boolean) field(wizards, "running");
        if (!running) {
            if (gameplayTicks >= MAX_START_WAIT_TICKS) {
                throw new IllegalStateException("Wizards did not start after three clients queued");
            }
            return;
        }

        @SuppressWarnings("unchecked")
        Map<Object, Object> states = (Map<Object, Object>) field(wizards, "states");
        @SuppressWarnings("unchecked")
        Map<Object, Object> snapshots = (Map<Object, Object>) field(wizards, "snapshots");
        @SuppressWarnings("unchecked")
        List<Location> chests = (List<Location>) field(wizards, "chests");
        gameplayArena = (World) field(wizards, "arena");
        String activeResource = (String) field(wizards, "activeMapResource");
        require(GAMEPLAY_MAP.equals(activeResource),
                "actual match fell back from selected map (active=" + activeResource + ")");
        require(gameplayArena != null && "arcadeport_source_wizards".equals(gameplayArena.getName()),
                "actual Wizards round is not using the selected source world");
        require(gameplayArena.getPVP(), "the live Wizards arena did not enable PvP");
        require(states.size() == 3, "expected three active wizard states, found " + states.size());
        require(snapshots.size() == 3, "player snapshots were not captured for all three clients");
        require(chests.size() >= 8, "live round has fewer than eight loot chests");

        validateLivePlayer(owner, states, "MAGE");
        validateLivePlayer(rival, states, "SORCERER");
        validateLivePlayer(third, states, "MYSTIC");
        validateLootRefilled(chests);

        // Exercise a real supported spell through the production cast path before
        // forcing eliminations. This checks mana, cooldown and projectile creation.
        Object ownerState = states.get(owner.getUniqueId());
        Class<?> spellType = Class.forName("au.com.testplex.wizards.WizardsSpell");
        @SuppressWarnings("unchecked")
        Object manaBolt = Enum.valueOf((Class<? extends Enum>) spellType.asSubclass(Enum.class), "MANA_BOLT");
        @SuppressWarnings("unchecked")
        Map<Object, Object> known = (Map<Object, Object>) field(ownerState, "known");
        int spellLevel = ((Number) known.get(manaBolt)).intValue();
        require(spellLevel >= 1, "the default Wizard did not receive Mana Bolt");
        // Match mana starts empty; seed the test state so the cast can be
        // checked immediately without waiting for natural regeneration.
        Field manaField = ownerState.getClass().getDeclaredField("mana");
        manaField.setAccessible(true);
        manaField.setFloat(ownerState, 100.0F);
        double manaBefore = ((Number) field(ownerState, "mana")).doubleValue();
        @SuppressWarnings("unchecked")
        Map<Object, Object> manaBoltsBefore = (Map<Object, Object>) field(wizards, "manaBolts");
        int projectilesBefore = manaBoltsBefore.size();
        Method castSpell = method(wizards.getClass(), "castSpell", Player.class, ownerState.getClass(),
                spellType, org.bukkit.entity.Entity.class, Block.class);
        castSpell.invoke(wizards, owner, ownerState, manaBolt, null, null);
        double manaAfter = ((Number) field(ownerState, "mana")).doubleValue();
        @SuppressWarnings("unchecked")
        Map<Object, Object> manaBoltsAfter = (Map<Object, Object>) field(wizards, "manaBolts");
        require(Math.abs((manaBefore - manaAfter) - 15.0D) < 0.01D,
                "Mana Bolt did not debit its configured 15 mana (before=" + manaBefore + ", after=" + manaAfter + ")");
        require(manaBoltsAfter.size() == projectilesBefore + 1,
                "Mana Bolt did not create exactly one tracked homing projectile");
        @SuppressWarnings("unchecked")
        Map<Object, Object> cooldowns = (Map<Object, Object>) field(ownerState, "cooldowns");
        require(cooldowns.containsKey(manaBolt)
                        && ((Number) cooldowns.get(manaBolt)).longValue() > System.currentTimeMillis(),
                "Mana Bolt did not apply its five-second cooldown");
        getLogger().info("WIZARDS_GAMEPLAY_SMOKE|SPELL_CAST|spell=MANA_BOLT|level=" + spellLevel
                + "|manaCost=15|projectiles=" + manaBoltsAfter.size() + "|cooldown=5");

        getLogger().info("WIZARDS_GAMEPLAY_SMOKE|ROUND_STARTED|players=3|map=" + GAMEPLAY_MAP
                + "|chests=" + chests.size() + "|snapshotCount=" + snapshots.size());
        // Let spawn protection and Paper's internal damage cooldown expire naturally.
        gameplayStage = 4;
        gameplayTicks = 0;
    }

    private void runGenericDamageProbe() {
        rival.setInvulnerable(false);
        rival.setNoDamageTicks(0);
        rival.setLastDamage(0.0D);
        rival.setHealth(10.0D);
        double before = rival.getHealth();
        lastAnyDamageObservation = "no entity damage event observed";
        rival.damage(1.0D);
        double after = rival.isDead() ? 0.0D : rival.getHealth();
        getLogger().info("WIZARDS_DAMAGE_PROBE|GENERIC|health=" + before + "->" + after
                + "|observed=" + lastAnyDamageObservation + "|internal=" + nmsDamageState(rival));
        require(after < before, "ordinary LivingEntity.damage(1) did not damage a live Wizards player");
        gameplayStage = 5;
        gameplayTicks = 0;
    }

    private void runAttributedDamageProbe() {
        rival.setInvulnerable(false);
        rival.setNoDamageTicks(0);
        rival.setLastDamage(0.0D);
        rival.setHealth(10.0D);
        double before = rival.getHealth();
        lastAnyDamageObservation = "no entity damage event observed";
        rival.damage(1.0D, owner);
        double after = rival.isDead() ? 0.0D : rival.getHealth();
        getLogger().info("WIZARDS_DAMAGE_PROBE|ATTRIBUTED|health=" + before + "->" + after
                + "|observed=" + lastAnyDamageObservation + "|internal=" + nmsDamageState(rival));
        require(after < before, "caster-attributed LivingEntity.damage(1, owner) did not damage a live Wizards player");
        gameplayStage = 6;
        gameplayTicks = 0;
    }

    private void runRainbowBeamAcceptance() throws Exception {
        @SuppressWarnings("unchecked")
        Map<Object, Object> states = (Map<Object, Object>) field(wizards, "states");
        Object ownerState = states.get(owner.getUniqueId());
        require(ownerState != null && states.containsKey(rival.getUniqueId()),
                "caster or target state disappeared before Rainbow Beam acceptance");
        Class<?> spellType = Class.forName("au.com.testplex.wizards.WizardsSpell");
        Object rainbowBeam = Enum.valueOf((Class<? extends Enum>) spellType.asSubclass(Enum.class), "RAINBOW_BEAM");
        @SuppressWarnings("unchecked")
        Map<Object, Object> known = (Map<Object, Object>) field(ownerState, "known");
        known.put(rainbowBeam, 5);
        Location beamTarget = findSafeLocationNear(owner);
        require(beamTarget != null, "could not find a safe nearby test location for the direct-target spell");
        rival.teleport(beamTarget);
        rival.setInvulnerable(false);
        rival.setNoDamageTicks(0);
        rival.setLastDamage(0.0D);
        rival.setHealth(0.25D);
        double beamDistance = owner.getEyeLocation().distance(rival.getEyeLocation());
        Class<?> rulesType = Class.forName("au.com.testplex.wizards.WizardsRules");
        double expectedBeamDamage = ((Number) method(rulesType, "rainbowBeamDamage", int.class, double.class)
                .invoke(null, 5, beamDistance)).doubleValue();
        double manaBeforeBeam = ((Number) field(ownerState, "mana")).doubleValue();
        @SuppressWarnings("unchecked")
        Map<Object, Object> beamCooldowns = (Map<Object, Object>) field(ownerState, "cooldowns");
        beamCooldowns.remove(rainbowBeam);
        owner.setInvulnerable(false);
        Method castSpell = method(wizards.getClass(), "castSpell", Player.class, ownerState.getClass(),
                spellType, org.bukkit.entity.Entity.class, Block.class);
        castSpell.invoke(wizards, owner, ownerState, rainbowBeam, rival, null);
        double manaAfterBeam = ((Number) field(ownerState, "mana")).doubleValue();
        long beamCooldownUntil = ((Number) beamCooldowns.getOrDefault(rainbowBeam, 0L)).longValue();
        getLogger().info("WIZARDS_BEAM_DIAGNOSTIC|mana=" + manaBeforeBeam + "->" + manaAfterBeam
                + "|knownLevel=" + known.get(rainbowBeam)
                + "|cooldownUntil=" + beamCooldownUntil
                + "|valid=" + rival.isValid() + "|dead=" + rival.isDead()
                + "|sameWorld=" + rival.getWorld().equals(gameplayArena)
                + "|noDamageTicks=" + rival.getNoDamageTicks()
                + "|lastDamageAmount=" + rival.getLastDamage()
                + "|markedInvulnerable=" + readMarkedInvulnerability(rival)
                + "|health=" + (rival.isDead() ? 0.0D : rival.getHealth()));
        require(Math.abs((manaBeforeBeam - manaAfterBeam) - 17.0D) < 0.01D,
                "Rainbow Beam did not pass its production cast gates (mana=" + manaBeforeBeam + "->"
                        + manaAfterBeam + ", knownLevel=" + known.get(rainbowBeam)
                        + ", cooldown=" + beamCooldownUntil + ")");
        require(rival.isDead(), "Rainbow Beam damage failed (health="
                + (rival.isDead() ? 0.0D : rival.getHealth()) + ", expectedDamage=" + expectedBeamDamage
                + ", distance=" + beamDistance + ", pvp=" + gameplayArena.getPVP()
                + ", mode=" + rival.getGameMode() + ", invulnerable=" + readMarkedInvulnerability(rival)
                + ", noDamageTicks=" + rival.getNoDamageTicks()
                + ", lastDamage=" + lastDamageObservation
                + ", lastAnyDamage=" + lastAnyDamageObservation + ")");
        require(((Number) field(ownerState, "kills")).intValue() == 1,
                "the direct-target spell elimination was not credited to its caster");
        getLogger().info("WIZARDS_GAMEPLAY_SMOKE|SPELL_IMPACT|spell=RAINBOW_BEAM|level=5"
                + "|damageApplied=true|killCredited=true|manaCost=17|distance=" + beamDistance);
        getLogger().info("WIZARDS_GAMEPLAY_SMOKE|ELIMINATION|victim=" + rival.getName()
                + "|attacker=" + owner.getName() + "|stage=1");
        gameplayStage = 2;
        gameplayTicks = 0;
    }

    private void verifySpectatorAndEliminateFinalOpponent() throws Exception {
        boolean running = (Boolean) field(wizards, "running");
        if (!running) {
            throw new IllegalStateException("the match ended before the third player could verify spectator mode");
        }
        if (rival == null || !rival.isOnline() || rival.isDead()) {
            if (gameplayTicks >= 160) {
                throw new IllegalStateException("the first eliminated player did not complete respawn");
            }
            return;
        }

        @SuppressWarnings("unchecked")
        Map<Object, Object> states = (Map<Object, Object>) field(wizards, "states");
        Object rivalState = states.get(rival.getUniqueId());
        Object ownerState = states.get(owner.getUniqueId());
        Object thirdState = states.get(third.getUniqueId());
        require(rivalState != null && Boolean.FALSE.equals(field(rivalState, "alive")),
                "eliminated player was incorrectly marked alive after respawn");
        require(rival.getGameMode() == GameMode.SPECTATOR,
                "eliminated player did not respawn into spectator mode");
        require(rival.getWorld().equals(gameplayArena),
                "eliminated player was not kept in the active Wizards arena while spectating");
        require(ownerState != null && thirdState != null
                        && Boolean.TRUE.equals(field(ownerState, "alive"))
                        && Boolean.TRUE.equals(field(thirdState, "alive")),
                "the two remaining players are not alive after the first elimination");
        require(((Number) field(ownerState, "kills")).intValue() == 1,
                "the first PvP elimination was not credited to the attacker");

        getLogger().info("WIZARDS_GAMEPLAY_SMOKE|SPECTATOR|player=" + rival.getName()
                + "|aliveRemaining=2|creditedKills=1");
        // Finish the last opponent with actual half-heart melee hits, ensuring
        // the cap remains on melee after spell damage has been separated.
        third.setNoDamageTicks(0);
        third.setHealth(20.0D);
        int meleeStrikes = 0;
        while (!third.isDead() && meleeStrikes < 48) {
            third.setNoDamageTicks(0);
            double before = third.getHealth();
            third.damage(1.0D, owner);
            double dealt = before - (third.isDead() ? 0.0D : third.getHealth());
            require(dealt >= 0.49D && dealt <= 0.51D,
                    "a direct melee strike did not retain the half-heart damage cap (dealt=" + dealt + ")");
            meleeStrikes++;
        }
        require(third.isDead(), "the final opponent could not be eliminated by repeated capped melee hits");
        require(((Number) field(ownerState, "kills")).intValue() == 2,
                "the final melee elimination was not credited to its attacker");
        getLogger().info("WIZARDS_GAMEPLAY_SMOKE|ELIMINATION|victim=" + third.getName()
                + "|attacker=" + owner.getName() + "|stage=2");
        gameplayStage = 3;
        gameplayTicks = 0;
    }

    private void verifyPlayerRestorationAndFinish() throws Exception {
        boolean running = (Boolean) field(wizards, "running");
        @SuppressWarnings("unchecked")
        Map<Object, Object> states = (Map<Object, Object>) field(wizards, "states");
        @SuppressWarnings("unchecked")
        Map<Object, Object> snapshots = (Map<Object, Object>) field(wizards, "snapshots");
        @SuppressWarnings("unchecked")
        Map<Object, Object> queue = (Map<Object, Object>) field(wizards, "queue");
        @SuppressWarnings("unchecked")
        Map<Object, Object> pendingRestore = (Map<Object, Object>) field(wizards, "pendingRestore");

        if (running || !states.isEmpty() || !snapshots.isEmpty() || !queue.isEmpty()
                || !pendingRestore.isEmpty()) {
            if (gameplayTicks >= 240) {
                throw new IllegalStateException("round/player state was not fully restored after PvP eliminations"
                        + " (running=" + running + ", states=" + states.size()
                        + ", snapshots=" + snapshots.size() + ", queue=" + queue.size()
                        + ", pendingRestore=" + pendingRestore.size() + ")");
            }
            return;
        }

        require(owner.isOnline() && rival.isOnline() && third.isOnline(),
                "a client disconnected during death/respawn restoration acceptance");
        require(!owner.getWorld().equals(gameplayArena), "winner was not restored out of the match world");
        require(!rival.getWorld().equals(gameplayArena), "spectator was not restored out of the match world");
        require(!third.getWorld().equals(gameplayArena), "final eliminated player was not restored out of the match world");
        validateRestoredPlayer(owner, ownerBaseline);
        validateRestoredPlayer(rival, rivalBaseline);
        validateRestoredPlayer(third, thirdBaseline);

        String statsRoot = "local-stats." + owner.getUniqueId() + ".wizards.";
        require(arcade.getConfig().getLong(statsRoot + "wins") == 1L,
                "match winner did not receive exactly one Wizards win");
        require(arcade.getConfig().getLong(statsRoot + "kills") == 2L,
                "winner did not receive both player elimination credits");

        record("GAMEPLAY_PASS", GAMEPLAY_MAP, 3, 0, 0,
                "three real clients; Mage/Sorcerer/Mystic; live start and chest refill; two PvP eliminations; spectator respawn; kill/win progression; full lobby state restored for winner and both eliminated players");
        getLogger().info("WIZARDS_GAMEPLAY_SMOKE|PASS|players=3|kits=MAGE,SORCERER,MYSTIC"
                + "|roundStarted=true|lootRefilled=true|pvpEliminations=2|spectatorRespawn=true"
                + "|killWinProgress=true|snapshotsRestored=true");
        long mapPasses = results.stream().filter(line -> line.startsWith("PASS	")).count();
        getLogger().info("WIZARDS_SOURCE_MAP_SMOKE|FINAL|mapsPassed=" + mapPasses
                + "|gameplay=PASS|threeClientCombatRound=PASS");
        shutdownServer();
    }

    private void seedBaselineProfile(Player player, int variant) {
        Material[] tokens = {Material.EMERALD, Material.DIAMOND, Material.GOLD_INGOT};
        Material[] helmets = {Material.LEATHER_HELMET, Material.GOLDEN_HELMET, Material.IRON_HELMET};
        PotionEffectType[] effects = {
                PotionEffectType.SPEED, PotionEffectType.JUMP_BOOST, PotionEffectType.NIGHT_VISION
        };
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setItem(20, new ItemStack(tokens[variant], variant + 2));
        player.getInventory().setHelmet(new ItemStack(helmets[variant]));
        player.setHealth(12.0D + variant);
        player.setInvulnerable(true);
        player.setFoodLevel(12 + variant);
        player.setSaturation(2.0F + variant);
        player.setLevel(6 + variant);
        player.setExp(0.25F);
        player.setAbsorptionAmount(1.5D + variant);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.addPotionEffect(new PotionEffect(effects[variant], 1200, variant, false, false, true));
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setWalkSpeed(0.25F + variant * 0.03F);
        player.setFlySpeed(0.12F + variant * 0.02F);
        player.setSprinting(true);
        player.setCompassTarget(player.getWorld().getSpawnLocation().clone().add(variant + 5, 0, variant + 2));
    }

    private PlayerBaseline captureBaseline(Player player) {
        return new PlayerBaseline(player.getWorld(), player.getLocation().clone(),
                cloneItems(player.getInventory().getContents()), cloneItems(player.getInventory().getArmorContents()),
                player.getGameMode(), player.getScoreboard(), Boolean.TRUE.equals(readMarkedInvulnerability(player)),
                player.getHealth(), player.getAbsorptionAmount(),
                player.getFoodLevel(), player.getSaturation(), player.getLevel(), player.getExp(),
                player.getAllowFlight(), player.isFlying(), player.isSprinting(),
                player.getWalkSpeed(), player.getFlySpeed(), player.getFireTicks(),
                player.getCompassTarget() == null ? null : player.getCompassTarget().clone(),
                new ArrayList<>(player.getActivePotionEffects()));
    }

    private Location findSafeLocationNear(Player anchor) {
        World world = anchor.getWorld();
        int originX = anchor.getLocation().getBlockX();
        int originZ = anchor.getLocation().getBlockZ();
        for (int radius = 2; radius <= 10; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = originX + dx;
                    int z = originZ + dz;
                    int y = world.getHighestBlockYAt(x, z) + 1;
                    Block floor = world.getBlockAt(x, y - 1, z);
                    Block feet = world.getBlockAt(x, y, z);
                    Block head = world.getBlockAt(x, y + 1, z);
                    if (!floor.getType().isSolid() || !feet.isPassable() || !head.isPassable()) continue;
                    Material ground = floor.getType();
                    if (ground == Material.MAGMA_BLOCK || ground == Material.CACTUS
                            || ground == Material.CAMPFIRE || ground == Material.SOUL_CAMPFIRE
                            || ground == Material.FIRE || ground == Material.SOUL_FIRE
                            || ground == Material.POWDER_SNOW) continue;
                    return new Location(world, x + 0.5D, y, z + 0.5D);
                }
            }
        }
        return null;
    }

    private void validateRestoredPlayer(Player player, PlayerBaseline baseline) {
        require(baseline != null, "missing original lobby baseline for " + player.getName());
        require(player.getWorld().equals(baseline.world()), player.getName() + " was not restored to the original world");
        require(player.getLocation().distanceSquared(baseline.location()) <= 1.0D,
                player.getName() + " was not restored to the original lobby location");
        require(player.getGameMode() == baseline.gameMode(),
                player.getName() + " game mode was not restored");
        require(Boolean.TRUE.equals(readMarkedInvulnerability(player)) == baseline.invulnerable(),
                player.getName() + " lobby invulnerability was not restored");
        require(player.getScoreboard() == baseline.scoreboard(),
                player.getName() + " scoreboard was not restored");
        require(Math.abs(player.getHealth() - baseline.health()) < 0.01D,
                player.getName() + " health was not restored");
        require(Math.abs(player.getAbsorptionAmount() - baseline.absorption()) < 0.01D,
                player.getName() + " absorption hearts were not restored");
        require(player.getFoodLevel() == baseline.foodLevel()
                        && Math.abs(player.getSaturation() - baseline.saturation()) < 0.01F,
                player.getName() + " food/saturation was not restored");
        require(player.getLevel() == baseline.level() && Math.abs(player.getExp() - baseline.exp()) < 0.001F,
                player.getName() + " experience state was not restored");
        require(player.getAllowFlight() == baseline.allowFlight() && player.isFlying() == baseline.flying(),
                player.getName() + " flight permissions/state were not restored");
        require(player.isSprinting() == baseline.sprinting(),
                player.getName() + " sprint state was not restored");
        require(Math.abs(player.getWalkSpeed() - baseline.walkSpeed()) < 0.001F
                        && Math.abs(player.getFlySpeed() - baseline.flySpeed()) < 0.001F,
                player.getName() + " movement speed settings were not restored");
        require(player.getFireTicks() == baseline.fireTicks(),
                player.getName() + " fire timer was not restored");
        require(baseline.compassTarget() == null || (player.getCompassTarget() != null
                        && player.getCompassTarget().getWorld().equals(baseline.compassTarget().getWorld())
                        && player.getCompassTarget().distanceSquared(baseline.compassTarget()) < 0.01D),
                player.getName() + " compass target was not restored");
        require(player.getActivePotionEffects().size() == baseline.effects().size(),
                player.getName() + " potion-effect count was not restored");
        for (PotionEffect expected : baseline.effects()) {
            PotionEffect actual = player.getPotionEffect(expected.getType());
            require(actual != null && actual.getAmplifier() == expected.getAmplifier(),
                    player.getName() + " potion effect " + expected.getType() + " was not restored");
        }

        assertItemsEqual(player.getName() + " inventory", player.getInventory().getContents(), baseline.inventory());
        assertItemsEqual(player.getName() + " armour", player.getInventory().getArmorContents(), baseline.armour());
    }

    private void assertItemsEqual(String label, ItemStack[] actual, ItemStack[] expected) {
        require(actual.length == expected.length, label + " size changed after restoration");
        for (int slot = 0; slot < expected.length; slot++) {
            ItemStack before = expected[slot];
            ItemStack after = actual[slot];
            if (before == null || before.getType().isAir()) {
                require(after == null || after.getType().isAir(), label + " slot " + slot + " was not restored to empty");
            } else {
                require(after != null && before.isSimilar(after) && before.getAmount() == after.getAmount(),
                        label + " slot " + slot + " was not restored");
            }
        }
    }

    private ItemStack[] cloneItems(ItemStack[] items) {
        ItemStack[] copy = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) copy[i] = items[i] == null ? null : items[i].clone();
        return copy;
    }

    private record PlayerBaseline(
            World world, Location location, ItemStack[] inventory, ItemStack[] armour, GameMode gameMode,
            Scoreboard scoreboard, boolean invulnerable, double health, double absorption, int foodLevel, float saturation,
            int level, float exp, boolean allowFlight, boolean flying, boolean sprinting,
            float walkSpeed, float flySpeed, int fireTicks, Location compassTarget,
            List<PotionEffect> effects) {}

    /**
     * The target Paper runtime may expose isMarkedInvulnerable even when the
     * project's shared Bukkit compile surface does not. Reflect rather than
     * compiling against a version-specific getter.
     */
    private static boolean paperDamageReady(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = readFieldValue(handle, "connection");
            if (connection == null || connection instanceof String) return false;
            return Boolean.TRUE.equals(invokeNoArgs(connection, "hasClientLoaded"))
                    && !Boolean.TRUE.equals(invokeNoArgs(handle, "isChangingDimension"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String nmsDamageState(Player player) {
        String[] names = {"damageCooldownTime", "lastHurt", "invulnerableDuration", "invulnerableTime", "hurtTime",
                "invulnerable", "removed"};
        StringBuilder result = new StringBuilder();
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            for (String name : names) {
                Object value = "missing";
                Class<?> cursor = handle.getClass();
                while (cursor != null) {
                    try {
                        Field field = cursor.getDeclaredField(name);
                        field.setAccessible(true);
                        value = field.get(handle);
                        break;
                    } catch (NoSuchFieldException missing) {
                        cursor = cursor.getSuperclass();
                    }
                }
                if (result.length() > 0) result.append(',');
                result.append(name).append('=').append(value);
            }
            Object abilities = handle.getClass().getMethod("getAbilities").invoke(handle);
            result.append(",abilities.invulnerable=").append(readFieldValue(abilities, "invulnerable"));
            result.append(",handle.isAlive=").append(invokeNoArgs(handle, "isAlive"));
            result.append(",handle.isSpectator=").append(invokeNoArgs(handle, "isSpectator"));
            result.append(",handle.isCreative=").append(invokeNoArgs(handle, "isCreative"));
            result.append(",handle.isChangingDimension=").append(invokeNoArgs(handle, "isChangingDimension"));
            result.append(",handle.isInvulnerable=").append(invokeNoArgs(handle, "isInvulnerable"));
            Object connection = readFieldValue(handle, "connection");
            result.append(",connection.hasClientLoaded=")
                    .append(connection == null || connection instanceof String
                            ? String.valueOf(connection) : invokeNoArgs(connection, "hasClientLoaded"));
            Object damageSources = handle.getClass().getMethod("damageSources").invoke(handle);
            Object generic = damageSources.getClass().getMethod("generic").invoke(damageSources);
            Object level = handle.getClass().getMethod("level").invoke(handle);
            result.append(",handle.isInvulnerableToBaseGeneric=")
                    .append(invokeSingleArgument(handle, "isInvulnerableToBase", generic));
            Method query = null;
            for (Class<?> cursor = handle.getClass(); cursor != null && query == null; cursor = cursor.getSuperclass()) {
                for (Method method : cursor.getDeclaredMethods()) {
                    if (method.getName().equals("isInvulnerableTo") && method.getParameterCount() == 2
                            && method.getParameterTypes()[0].isInstance(level)
                            && method.getParameterTypes()[1].isInstance(generic)) {
                        query = method;
                        break;
                    }
                }
            }
            if (query != null) {
                query.setAccessible(true);
                result.append(",handle.isInvulnerableToGeneric=").append(query.invoke(handle, level, generic));
            } else {
                result.append(",handle.isInvulnerableToGeneric=missing");
            }
        } catch (Throwable failure) {
            result.append(",diagnosticError=").append(failure.getClass().getSimpleName())
                    .append(':').append(failure.getMessage());
        }
        return result.toString();
    }

    private static Object readFieldValue(Object target, String name) {
        try {
            Class<?> cursor = target.getClass();
            while (cursor != null) {
                try {
                    Field field = cursor.getDeclaredField(name);
                    field.setAccessible(true);
                    return field.get(target);
                } catch (NoSuchFieldException missing) {
                    cursor = cursor.getSuperclass();
                }
            }
            return "missing";
        } catch (Throwable failure) {
            return "error:" + failure.getClass().getSimpleName();
        }
    }

    private static Object invokeNoArgs(Object target, String name) {
        try {
            Method method = target.getClass().getMethod(name);
            return method.invoke(target);
        } catch (Throwable failure) {
            return "error:" + failure.getClass().getSimpleName();
        }
    }

    private static Object invokeSingleArgument(Object target, String name, Object argument) {
        try {
            Class<?> cursor = target.getClass();
            while (cursor != null) {
                for (Method method : cursor.getDeclaredMethods()) {
                    if (method.getName().equals(name) && method.getParameterCount() == 1
                            && method.getParameterTypes()[0].isInstance(argument)) {
                        method.setAccessible(true);
                        return method.invoke(target, argument);
                    }
                }
                cursor = cursor.getSuperclass();
            }
            return "missing";
        } catch (Throwable failure) {
            return "error:" + failure.getClass().getSimpleName() + ":" + failure.getMessage();
        }
    }

    private static Boolean readMarkedInvulnerability(Player player) {
        try {
            java.lang.reflect.Method getter = player.getClass().getMethod("isMarkedInvulnerable");
            Object value = getter.invoke(player);
            return value instanceof Boolean flag ? flag : null;
        } catch (ReflectiveOperationException | SecurityException ignored) {
            return null;
        }
    }

    private void validateLivePlayer(Player player, Map<Object, Object> states, String expectedKit) throws Exception {
        require(player != null && player.isOnline(), "a queued test client is no longer online");
        require(player.getWorld().equals(gameplayArena), player.getName() + " was not teleported into source arena");
        require(gameplayArena.getWorldBorder().isInside(player.getLocation()),
                player.getName() + " spawned outside the source-map border");
        Object state = states.get(player.getUniqueId());
        require(state != null, "missing live WizardState for " + player.getName());
        require(Boolean.TRUE.equals(field(state, "alive")), player.getName() + " is not marked alive");
        require(expectedKit.equals(String.valueOf(field(state, "kit"))),
                player.getName() + " has kit " + field(state, "kit") + ", expected " + expectedKit);
        ItemStack wand = player.getInventory().getItem(0);
        require(wand != null && !wand.getType().isAir(), player.getName() + " did not receive their first wand");
        ItemStack menu = player.getInventory().getItem(8);
        require(menu != null && !menu.getType().isAir(), player.getName() + " did not receive the spell menu item");
        require(player.getScoreboard().getObjective(DisplaySlot.SIDEBAR) != null,
                player.getName() + " did not receive the Wizards match scoreboard");
        require(player.getActivePotionEffects().isEmpty(),
                player.getName() + " carried lobby potion effects into the match");
        require(!Boolean.TRUE.equals(readMarkedInvulnerability(player)),
                player.getName() + " carried lobby invulnerability into the match");
        require(player.getAbsorptionAmount() == 0.0D,
                player.getName() + " carried lobby absorption hearts into the match");
        require(!player.getAllowFlight() && !player.isFlying() && !player.isSprinting(),
                player.getName() + " carried lobby flight/sprint state into the match");
        require(Math.abs(player.getWalkSpeed() - 0.2F) < 0.001F
                        && Math.abs(player.getFlySpeed() - 0.1F) < 0.001F,
                player.getName() + " did not receive standard match movement speeds");
    }

    private void validateLootRefilled(List<Location> chests) {
        int nonEmpty = 0;
        for (Location location : chests) {
            Block block = location.getBlock();
            require(block.getType() == Material.CHEST && block.getState() instanceof Chest,
                    "live round lost a chest block entity at " + location);
            Inventory inventory = ((Chest) block.getState()).getBlockInventory();
            long occupied = java.util.Arrays.stream(inventory.getContents())
                    .filter(item -> item != null && !item.getType().isAir()).count();
            if (occupied > 0) nonEmpty++;
        }
        require(nonEmpty == chests.size(),
                "only " + nonEmpty + "/" + chests.size() + " chests were refilled with actual Wizards loot");
        getLogger().info("WIZARDS_GAMEPLAY_SMOKE|LOOT_REFILL|occupied=" + nonEmpty
                + "|total=" + chests.size());
    }

    private void validateStarts(World world, List<Location> starts) {
        Set<String> unique = new HashSet<>();
        for (Location start : starts) {
            require(start != null && world.equals(start.getWorld()), "spawn marker belongs to another world");
            require(world.getWorldBorder().isInside(start), "spawn is outside the configured world border: " + start);
            int x = start.getBlockX(), y = start.getBlockY(), z = start.getBlockZ();
            Block floor = world.getBlockAt(x, y - 1, z);
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            require(floor.getType().isSolid(), "spawn has no solid floor: " + start);
            require(feet.isPassable() && head.isPassable(), "spawn has insufficient headroom: " + start);
            Material ground = floor.getType();
            require(ground != Material.MAGMA_BLOCK && ground != Material.CACTUS
                            && ground != Material.CAMPFIRE && ground != Material.SOUL_CAMPFIRE
                            && ground != Material.FIRE && ground != Material.SOUL_FIRE
                            && ground != Material.POWDER_SNOW,
                    "spawn is on hazardous terrain " + ground + ": " + start);
            require(unique.add(x + ":" + y + ":" + z), "duplicate start location: " + start);
        }
    }

    private void validateChests(World world, List<Location> chests) {
        Set<String> unique = new HashSet<>();
        for (Location location : chests) {
            require(location != null && world.equals(location.getWorld()), "chest marker belongs to another world");
            require(world.getWorldBorder().isInside(location), "chest is outside the world border: " + location);
            Block block = location.getBlock();
            require(block.getType() == Material.CHEST, "chest position is not a chest block: " + location);
            require(block.getState() instanceof Chest, "chest block entity was not recreated: " + location);
            Chest chest = (Chest) block.getState();
            require(chest.getBlockInventory().getSize() >= 27, "chest inventory is not usable: " + location);
            require(unique.add(location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ()),
                    "duplicate chest location: " + location);
        }
    }

    private void failAndShutdown(String status, String resource, Throwable failure) {
        Throwable cause = rootCause(failure);
        String message = cause.getClass().getSimpleName() + ": " + safeMessage(cause);
        getLogger().severe("WIZARDS_GAMEPLAY_SMOKE|" + status + "|" + message);
        record(status, resource, 0, 0, gameplayTicks * 50L, message);
        shutdownServer();
    }