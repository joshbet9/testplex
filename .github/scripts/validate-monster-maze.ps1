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
        throw "$Name: source file not found: $File"
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

$failures = [System.Collections.Generic.List[string]]::new()
$mazeDir = Join-Path $repoRoot "Plugins\Nautilus.Game.Arcade\src\nautilus\game\arcade\game\games\monstermaze"
$coreDir = Join-Path $repoRoot "Plugins\Mineplex.Core\src\mineplex\core\game"
$gameTypeFile = Join-Path $repoRoot "Plugins\Nautilus.Game.Arcade\src\nautilus\game\arcade\GameType.java"
$customDataFile = Join-Path $repoRoot "Plugins\Mineplex.Core\src\mineplex\core\customdata\CustomDataManager.java"
$displayFile = Join-Path $coreDir "GameDisplay.java"
$kitFile = Join-Path $coreDir "kit\GameKit.java"
$mazeFile = Join-Path $mazeDir "Maze.java"
$mazeCoreFile = Join-Path $mazeDir "MonsterMaze.java"
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
Assert-Contains "Both variants reuse the original map pool" $gameTypeFile "new GameType[]{GameType.MonsterMaze}, false"
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
Assert-Contains "Random maze selection returns the selected non-repeat map" $layoutFile "return selected;"
Assert-Contains "Fixed pattern selection is available" $layoutFile "getPatternMapPreset"
Assert-Contains "Pattern selection is configurable" $mazeCoreFile "getPatternMapPreset(_center, getMazeBlockData(), requestedPattern)"
Assert-Contains "Personal bests use Mineplex custom data" $pbFile "customData.setData(player, key, bestToSave)"
Assert-Contains "Personal best keys distinguish game mode, pattern and kit" $pbFile 'displayPrefix + "pb.pattern" + pattern + "." + slug(kitName)'
Assert-Contains "Personal bests can be viewed with /game pb" $pbFile "public static void show("
Assert-Contains "Custom data setter saves through Mineplex storage" $customDataFile "public void setData(Player player, String key, int value)"
Assert-Contains "Speed and Modern have native display entries" $displayFile "MonsterMazeSpeed("
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
