[CmdletBinding()]
param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

function Assert-Contains {
    param(
        [Parameter(Mandatory=$true)][string]$Name,
        [Parameter(Mandatory=$true)][string]$File,
        [Parameter(Mandatory=$true)][string]$Expected
    )

    if (-not (Test-Path -LiteralPath $File)) {
        throw "${Name}: source file not found: $File"
    }

    $content = Get-Content -LiteralPath $File -Raw
    if (-not $content.Contains($Expected)) {
        $script:failures.Add($Name)
        Write-Host "[FAIL] $Name" -ForegroundColor Red
    }
    else {
        Write-Host "[PASS] $Name" -ForegroundColor Green
    }
}

function Assert-NotContains {
    param(
        [Parameter(Mandatory=$true)][string]$Name,
        [Parameter(Mandatory=$true)][string]$File,
        [Parameter(Mandatory=$true)][string]$Unexpected
    )

    if (-not (Test-Path -LiteralPath $File)) {
        throw "${Name}: source file not found: $File"
    }

    $content = Get-Content -LiteralPath $File -Raw
    if ($content.Contains($Unexpected)) {
        $script:failures.Add($Name)
        Write-Host "[FAIL] $Name" -ForegroundColor Red
    }
    else {
        Write-Host "[PASS] $Name" -ForegroundColor Green
    }
}

$failures = [System.Collections.Generic.List[string]]::new()
$mazeDir = Join-Path $repoRoot "Plugins\Nautilus.Game.Arcade\src\nautilus\game\arcade\game\games\monstermaze"
$coreDir = Join-Path $repoRoot "Plugins\Mineplex.Core\src\mineplex\core\game"
$gameTypeFile = Join-Path $repoRoot "Plugins\Nautilus.Game.Arcade\src\nautilus\game\arcade\GameType.java"
$customDataFile = Join-Path $repoRoot "Plugins\Mineplex.Core\src\mineplex\core\customdata\CustomDataManager.java"
$displayFile = Join-Path $coreDir "GameDisplay.java"
$kitFile = Join-Path $coreDir "kit\GameKit.java"
$mazeFile = Join-Path $mazeDir "Maze.java"
$mazeCoreFile = Join-Path $mazeDir "MonsterMaze.java"
$mapManagerFile = Join-Path $repoRoot "Plugins\Nautilus.Game.Arcade\src\nautilus\game\arcade\managers\GameCreationManager.java"
$arcadeManagerFile = Join-Path $repoRoot "Plugins\Nautilus.Game.Arcade\src\nautilus\game\arcade\ArcadeManager.java"
$setGameFile = Join-Path $repoRoot "Plugins\Nautilus.Game.Arcade\src\nautilus\game\arcade\gui\privateServer\page\SetGamePage.java"
$cosmeticsFile = Join-Path $repoRoot "Plugins\Nautilus.Game.Arcade\src\nautilus\game\arcade\cosmetics\ArcadeCosmeticsMenu.java"
$deployScriptFile = Join-Path $repoRoot ".github\scripts\deploy-testplex-hyperv.sh"
$disguiseManagerFile = Join-Path $repoRoot "Plugins\Mineplex.Core\src\mineplex\core\disguise\DisguiseManager.java"
$disguiseFactoryFile = Join-Path $repoRoot "Plugins\Mineplex.Core\src\mineplex\core\disguise\DisguiseFactory.java"
$silverfishDisguiseFile = Join-Path $repoRoot "Plugins\Mineplex.Core\src\mineplex\core\disguise\disguises\DisguiseSilverfish.java"
$endermiteDisguiseFile = Join-Path $repoRoot "Plugins\Mineplex.Core\src\mineplex\core\disguise\disguises\DisguiseEndermite.java"
$mushroomCowDisguiseFile = Join-Path $repoRoot "Plugins\Mineplex.Core\src\mineplex\core\disguise\disguises\DisguiseMushroomCow.java"
$layoutFile = Join-Path $mazeDir "MMMazes.java"
$safePadFile = Join-Path $mazeDir "SafePad.java"
$pbFile = Join-Path $mazeDir "MonsterMazePersonalBestManager.java"

Write-Host "Monster Maze native port validation" -ForegroundColor Cyan
Write-Host "Repository: $repoRoot"

if (-not $SkipBuild) {
    $java = Get-Command java -ErrorAction SilentlyContinue
    if (-not $java) {
        throw "Java was not found on PATH. Configure a Java 8 JDK before running this validation."
    }
    $javaVersion = (& java -version 2>&1 | Out-String)
    if ($javaVersion -notmatch 'version "1\.8') {
        throw ("Mineplex Reborn requires Java 8 for this build. Detected:" + [Environment]::NewLine + $javaVersion)
    }
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        throw "Maven (mvn) was not found on PATH."
    }

    Write-Host ""
    Write-Host "Building the full Mineplex Reborn Maven reactor..." -ForegroundColor Cyan
    $pom = Join-Path $repoRoot "Plugins\pom.xml"
    $libs = Join-Path $repoRoot "libs"
    & mvn --batch-mode --no-transfer-progress -f $pom "-Dmineplex.libs.dir=$libs" -DskipTests install
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE. Fix build errors before in-game testing."
    }

    $arcadeJar = Join-Path $repoRoot "Plugins\out\Arcade.jar"
    if (-not (Test-Path -LiteralPath $arcadeJar)) {
        throw "Maven succeeded but Plugins\out\Arcade.jar was not produced."
    }
    if (-not (Get-Command jar -ErrorAction SilentlyContinue)) {
        throw "The JDK jar command was not found on PATH."
    }

    $jarEntries = & jar tf $arcadeJar
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect the built Arcade.jar."
    }
    foreach ($classEntry in @(
        "nautilus/game/arcade/game/games/monstermaze/MonsterMazeSpeedGame.class",
        "nautilus/game/arcade/game/games/monstermaze/MonsterMazeModern.class",
        "nautilus/game/arcade/game/games/monstermaze/MonsterMazePersonalBestManager.class"
    )) {
        if ($jarEntries -notcontains $classEntry) {
            throw "Built Arcade.jar is missing $classEntry"
        }
    }
    Write-Host "[PASS] Arcade.jar contains Speed, Modern and native PB classes" -ForegroundColor Green
}
else {
    Write-Host "Build skipped by request; running source-level contracts only." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Checking native integration contracts..." -ForegroundColor Cyan
Assert-Contains "Original Monster Maze remains registered" $gameTypeFile "MonsterMaze(MonsterMaze.class, GameDisplay.MonsterMaze)"
Assert-Contains "Monster Maze Speed registered" $gameTypeFile "MonsterMazeSpeed(MonsterMazeSpeedGame.class, GameDisplay.MonsterMazeSpeed"
Assert-Contains "Monster Maze Modern registered" $gameTypeFile "MonsterMazeModern(MonsterMazeModern.class, GameDisplay.MonsterMazeModern"
Assert-Contains "Variants can use dedicated maps and fall back to original pool" $gameTypeFile "new GameType[]{GameType.MonsterMaze}, true"
Assert-Contains "Original ruleset exists" (Join-Path $mazeDir "MonsterMazeMode.java") "ORIGINAL"
Assert-Contains "Speed ruleset exists" (Join-Path $mazeDir "MonsterMazeMode.java") "SPEED"
Assert-Contains "Modern ruleset exists" (Join-Path $mazeDir "MonsterMazeMode.java") "MODERN"
Assert-Contains "Modern starts with 225 monsters; other modes use 150" $mazeCoreFile "_maze.fillSpawn(isActualModernMode() ? 225 : 150)"
Assert-Contains "Safe Pad geometry is symmetric" $safePadFile "dx > -2.5 && dx < 2.5"
Assert-Contains "Safe Pad full-snapshot cleanup exists" $safePadFile "public void restoreAll()"
Assert-Contains "Safe Pad beacon restoration is objective-only" $safePadFile "restoreObjectiveBeaconIfMissing"
Assert-Contains "Monster spawns are queued in bounded per-tick batches" $mazeFile "MAX_MOB_SPAWNS_PER_TICK = 25"
Assert-Contains "Pending mob spawns are cleared on game end" $mazeFile "_pendingSpawnLocations.clear();"
Assert-Contains "Modern uses the standalone timer curve" $mazeFile "35 - (_curSafe * 20 / 9)"
Assert-Contains "Modern adds 30 mobs instead of stacking the 15-mob wave" $mazeFile "fillSpawn(30)"
Assert-Contains "Body Rush intercepts monster contact" $mazeFile "bodyRushKit.consumeMonsterHit(player)"
Assert-Contains "Cryo Blitz freezes monsters" $mazeFile "public int freezeMonsters(Location origin"
Assert-Contains "Maverick targets the next Safe Pad" $mazeFile "getMobKnockTarget()"
Assert-Contains "Variant map catalogs fall back to non-empty original maps" $mapManagerFile "if (maps != null && !maps.isEmpty())"
Assert-Contains "Map root supports explicit system-property override" $arcadeManagerFile "mineplex.maps.dir"
Assert-Contains "Map root supports explicit environment override" $arcadeManagerFile "MINEPLEX_MAPS_DIR"
Assert-Contains "Map root targets the deployed shared map directory" $arcadeManagerFile "/home/monstermaze/update/maps"
Assert-Contains "Map root detects a nested maps bundle" $arcadeManagerFile "containsMapArchives(nestedMaps)"
Assert-Contains "Failed map root lookups are retried later" $arcadeManagerFile "_mapRootResolutionAttempted = false;"
Assert-Contains "Micro Battle category aliases resolve" $arcadeManagerFile "trimTrailingS(actual).equals(trimTrailingS(wanted))"
Assert-Contains "Spleef typo in the legacy map bundle resolves" $arcadeManagerFile '"speef".equals(actual)'
Assert-Contains "Deployment fixes restrictive map directory modes" $deployScriptFile 'find "$MAP_DEST" -type d -exec chmod 0755 {} +'
Assert-Contains "Deployment keeps map ZIPs readable" $deployScriptFile 'find "$MAP_DEST" -type f -iname ''*.zip'' -exec chmod 0644 {} +'
Assert-Contains "Deployment checks map ZIPs as the service account" $deployScriptFile 'runuser -u monstermaze -- find "$MAP_DEST" -type f -iname ''*.zip'' -print | wc -l'
Assert-Contains "Set Game selection continues without detected maps" $setGameFile "boolean mapsAvailable = hasMapArchive(type);"
Assert-Contains "Set Game warns when no maps are detected instead of rejecting selection" $setGameFile "It will not start until a map is available."
Assert-NotContains "Set Game no longer blocks selection when the map check is empty" $setGameFile "The game has not been selected."
Assert-Contains "Game startup is blocked before WorldData initialisation when no ZIPs exist" $mapManagerFile "if (!usesGeneratedMap(gameType) && (_maps == null || _maps.isEmpty()))"
Assert-Contains "Generated UHC worlds do not need map ZIPs" $mapManagerFile "return type == GameType.UHC || type == GameType.UHCSolo"
Assert-Contains "Monster Maze cosmetics use one grouped mob-set line" $cosmeticsFile 'compositeFeature("mob-line", "Mob Sets & Custom Colours"'
Assert-Contains "Monster Maze includes the source mob-set catalogue" $cosmeticsFile '"creeper", "skeleton", "spider", "zombie", "slime", "zombie_pigman"'
Assert-Contains "Snake cosmetics are grouped into one line" $cosmeticsFile 'compositeFeature("snake-style", "Snake Skin & Ability Effects"'
Assert-Contains "Paintball cosmetics are grouped into one line" $cosmeticsFile 'compositeFeature("paintball-style", "Weapon Skins & Impact Effects"'
Assert-Contains "Evolution cosmetics are grouped into one line" $cosmeticsFile 'compositeFeature("evolution-style", "Evolution Appearance & Effects"'
Assert-Contains "Micro Battles cosmetics are grouped into one line" $cosmeticsFile 'compositeFeature("battle-style", "Glass Colour & Border Effects"'
Assert-Contains "Dragon Escape keeps a separate trail line and grouped finish line" $cosmeticsFile 'compositeFeature("finish-presentation", "Finish Presentation"'
Assert-Contains "Win effects have a dedicated catalogue" $cosmeticsFile "private void openWinEffects(Player player)"
Assert-NotContains "Per-game pages no longer show a separate win-effect placeholder" $cosmeticsFile "Win Effect Plan"
Assert-Contains "Custom Pack unlock can be provided by the entitlement adapter" $cosmeticsFile "setMonsterMazeCustomPackUnlocked(UUID playerId, boolean unlocked)"
Assert-Contains "Per-mob-set beacon and Safe Pad colours persist independently" $cosmeticsFile "mob-set-colours."
Assert-Contains "Enabled Monster Maze mob sets are exposed to gameplay" $cosmeticsFile "getEnabledMonsterMazeMobSets(UUID playerId)"
Assert-Contains "Custom mob-set selection is persisted separately from default map appearance" $cosmeticsFile "mob-sets-customized"
Assert-Contains "Operators can grant the Custom Pack for test runs" $cosmeticsFile "arcadecosmetics unlock <online-player> monstermaze custom-pack"
Assert-Contains "Monster mobs use viewer-specific disguise packet selection" $disguiseManagerFile "getActiveDisguise(int entityId, Player viewer)"
Assert-Contains "Spawn packets resolve a disguise for the current viewer" $disguiseManagerFile "getActiveDisguise(entityId, owner)"
Assert-Contains "Monster Maze registers a per-viewer mob disguise" $mazeFile "disguise(disguise, viewer -> viewerIds.contains(viewer.getUniqueId()))"
Assert-Contains "Monster Maze chooses one enabled mob set per viewer per run" $mazeFile "getMobSetForPlayer(Player player)"
Assert-Contains "Safe Pad applies client-specific cosmetic colours" $safePadFile "applyViewerCosmeticColours()"
Assert-Contains "Safe Pad clears fake colour packets on cleanup" $safePadFile "clearViewerCosmeticColours()"
Assert-Contains "Safe Pad warning colours override cosmetic tint" $safePadFile "warningTintActive"
Assert-Contains "Disguise factory supports cave spider" $disguiseFactoryFile "case CAVE_SPIDER:"
Assert-Contains "Disguise factory supports guardian" $disguiseFactoryFile "case GUARDIAN:"
Assert-Contains "Disguise factory supports rabbit" $disguiseFactoryFile "case RABBIT:"
Assert-Contains "Disguise factory supports silverfish" $disguiseFactoryFile "case SILVERFISH:"
Assert-Contains "Disguise factory supports endermite" $disguiseFactoryFile "case ENDERMITE:"
Assert-Contains "Disguise factory supports mooshroom" $disguiseFactoryFile "case MUSHROOM_COW:"
Assert-Contains "Silverfish disguise class exists" $silverfishDisguiseFile "EntityType.SILVERFISH"
Assert-Contains "Endermite disguise class exists" $endermiteDisguiseFile "EntityType.ENDERMITE"
Assert-Contains "Mooshroom disguise class exists" $mushroomCowDisguiseFile "EntityType.MUSHROOM_COW"
Assert-Contains "Random maze selection returns the selected non-repeat map" $layoutFile "return selected;"
Assert-Contains "Fixed pattern selection is available" $layoutFile "getPatternMapPreset"
Assert-Contains "Pattern selection is configurable" $mazeCoreFile "getPatternMapPreset(_center, getMazeBlockData(), requestedPattern)"
Assert-Contains "Personal bests use Mineplex custom data" $pbFile "customData.setData(player, key, bestToSave)"
Assert-Contains "Personal best keys distinguish game mode, pattern and kit" $pbFile 'displayPrefix + "pb.pattern" + pattern + "." + slug(kitName)'
Assert-Contains "Personal bests can be viewed with /game pb" $pbFile "public static void show("
Assert-Contains "Custom data setter saves through Mineplex storage" $customDataFile "public void setData(Player player, String key, int value)"
Assert-Contains "Speed and Modern have native display entries" $displayFile "MonsterMazeSpeed("

Assert-Contains "Maze preset is selected before normal player placement" $mazeCoreFile "selectMazePreset();"
Assert-Contains "Maze layout void cells are cleared to prevent a second floor" $layoutFile "for (Location loc : _air)"
Assert-Contains "Monster Maze team start uses the centre marker" $mazeCoreFile "playerTeam.GetSpawns().add(centralSpawn.clone());"
Assert-Contains "Players are reseated at the central start after maze construction" $mazeCoreFile "player.teleport(centralSpawn);"
Assert-Contains "Visibility toggle is implemented as an interact control" $mazeCoreFile "onVisibilityToggle(PlayerInteractEvent event)"
Assert-Contains "Visibility dye cycles visible hidden and transparent modes" $mazeCoreFile "int next = current == 10 ? 8 : current == 8 ? 5 : 10;"
Assert-Contains "Transparent mode uses a per-viewer invisibility packet" $mazeCoreFile "PacketPlayOutEntityEffect(target.getEntityId(), invisible)"
Assert-Contains "Leaving transparent mode removes the client-only effect" $mazeCoreFile "PacketPlayOutRemoveEntityEffect(target.getEntityId(), new MobEffect(MobEffectList.INVISIBILITY.id, 1, 0))"
Assert-Contains "Transparent mode has a per-viewer ghost team" $mazeCoreFile 'registerNewTeam("mm_ghosts")'
Assert-Contains "Personal scoreboards include active Safe Pad distance" $mazeCoreFile 'board.write(C.cAqua + C.Bold + "Distance");'
Assert-Contains "Personal scoreboards show whether the player is safe" $mazeCoreFile '(safe ? "SAFE" : "UNSAFE")'
Assert-Contains "Personal scoreboards and visibility are restored at round end" $mazeCoreFile "clearPersonalScoreboardsAndVisibility();"
Assert-Contains "Compass is refreshed immediately when the active Safe Pad changes" $mazeFile "updateCompassTargets();"
Assert-Contains "First-stage path is generated through walkable maze cells" $mazeFile "Build a genuine walkable breadcrumb"
Assert-Contains "First-stage path particle effect is emitted" $mazeFile "point.getWorld().playEffect(point, Effect.HAPPY_VILLAGER, 0)"
Assert-Contains "The first two Safe Pads get fireworks" $mazeFile "playFirstTwoStagePadFireworks()"
Assert-Contains "Stale backing Snowmen are swept at startup and cleanup" $mazeFile "clearStaleMazeSnowmen()"
Assert-Contains "Bomb Lobbers has direct-hit ding hook" (Join-Path $repoRoot "Plugins\Nautilus.Game.Arcade\src\nautilus\game\arcade\game\games\lobbers\BombLobbers.java") "directHitDing"
Assert-Contains "Demo effects are registered" (Join-Path $repoRoot "Plugins\Nautilus.Game.Arcade\src\nautilus\game\arcade\Arcade.java") "new WinEffectArcadeDemo.MonsterMaze(gadgetManager)"
Assert-Contains "Bacon Brawl effect is clearly labelled a demo" (Join-Path $repoRoot "Plugins\Nautilus.Game.Arcade\src\nautilus\game\arcade\wineffect\WinEffectBaconBrawl.java") "Mighty Lava Arena Demo"

Assert-Contains "Speed has separate kit identities" $kitFile "MONSTER_MAZE_SPEED_JUMPER"
Assert-Contains "Modern has separate kit identities" $kitFile "MONSTER_MAZE_MODERN_JUMPER"

Write-Host ""
if ($failures.Count -gt 0) {
    Write-Host "$($failures.Count) source-level contract(s) failed:" -ForegroundColor Red
    foreach ($failure in $failures) { Write-Host " - $failure" -ForegroundColor Red }
    exit 1
}

Write-Host "All Monster Maze native source-level contracts passed." -ForegroundColor Green
if ($SkipBuild) {
    Write-Host "Note: source checks do not replace a Java 8 Maven build." -ForegroundColor Yellow
}
