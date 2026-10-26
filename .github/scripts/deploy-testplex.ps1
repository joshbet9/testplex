$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Get-RequiredEnvironmentValue {
    param([Parameter(Mandatory = $true)][string]$Name)
    $value = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Required GitHub Actions variable '$Name' is missing."
    }
    return $value.Trim()
}

function Resolve-ConfiguredScript {
    param([Parameter(Mandatory = $true)][string]$Name)
    $configuredPath = Get-RequiredEnvironmentValue -Name $Name
    if (-not (Test-Path -LiteralPath $configuredPath -PathType Leaf)) {
        throw "Configured script for $Name does not exist: $configuredPath"
    }
    return (Resolve-Path -LiteralPath $configuredPath).Path
}

function Get-TestServerProcesses {
    param([Parameter(Mandatory = $true)][string]$Match)
    return @(
        Get-CimInstance Win32_Process |
            Where-Object {
                ($_.Name -ieq "java.exe" -or $_.Name -ieq "javaw.exe") -and
                -not [string]::IsNullOrWhiteSpace($_.CommandLine) -and
                $_.CommandLine.IndexOf($Match, [StringComparison]::OrdinalIgnoreCase) -ge 0
            }
    )
}

function Wait-ForServerProcessState {
    param(
        [Parameter(Mandatory = $true)][string]$Match,
        [Parameter(Mandatory = $true)][bool]$ShouldBeRunning,
        [int]$TimeoutSeconds = 180
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        $running = (Get-TestServerProcesses -Match $Match).Count -gt 0
        if ($running -eq $ShouldBeRunning) {
            return
        }
        Start-Sleep -Seconds 2
    } while ([DateTime]::UtcNow -lt $deadline)

    $expected = if ($ShouldBeRunning) { "running" } else { "stopped" }
    throw "Timed out waiting for the isolated test server process to become $expected."
}

function Invoke-ConfiguredScript {
    param([Parameter(Mandatory = $true)][string]$Path)
    Write-Host "Running configured server script: $Path"
    & $Path
    if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) {
        throw ("Configured script failed with exit code " + $LASTEXITCODE + ": " + $Path)
    }
}

$serverRootInput = Get-RequiredEnvironmentValue -Name "TESTPLEX_SERVER_DIR"
if (-not (Test-Path -LiteralPath $serverRootInput -PathType Container)) {
    throw "TESTPLEX_SERVER_DIR does not exist or is not a directory: $serverRootInput"
}
$serverRoot = (Resolve-Path -LiteralPath $serverRootInput).Path
$markerPath = Join-Path $serverRoot ".testplex-sandbox"
$expectedMarker = "TESTPLEX ISOLATED TEST SERVER - NOT PRODUCTION"
if (-not (Test-Path -LiteralPath $markerPath -PathType Leaf)) {
    throw "Refusing to deploy: missing sandbox marker $markerPath"
}
$marker = (Get-Content -LiteralPath $markerPath -Raw).Trim()
if ($marker -cne $expectedMarker) {
    throw "Refusing to deploy: $markerPath must contain exactly '$expectedMarker'."
}

$pluginsDir = Join-Path $serverRoot "plugins"
if (-not (Test-Path -LiteralPath $pluginsDir -PathType Container)) {
    throw "Refusing to deploy: no plugins directory exists at $pluginsDir"
}
$artifactDirInput = Get-RequiredEnvironmentValue -Name "TESTPLEX_ARTIFACT_DIR"
if (-not (Test-Path -LiteralPath $artifactDirInput -PathType Container)) {
    throw "Downloaded plugin artifact directory is missing: $artifactDirInput"
}
$artifactDir = (Resolve-Path -LiteralPath $artifactDirInput).Path
$jarFiles = @(Get-ChildItem -LiteralPath $artifactDir -Filter "*.jar" -File)
if ($jarFiles.Count -eq 0 -or -not ($jarFiles | Where-Object { $_.Name -ieq "Arcade.jar" })) {
    throw "The build artifact must contain Arcade.jar and at least one JAR file."
}

$stopScript = Resolve-ConfiguredScript -Name "TESTPLEX_STOP_SCRIPT"
$startScript = Resolve-ConfiguredScript -Name "TESTPLEX_START_SCRIPT"
$processMatch = Get-RequiredEnvironmentValue -Name "TESTPLEX_PROCESS_MATCH"
if ($processMatch.Length -lt 8) {
    throw "TESTPLEX_PROCESS_MATCH must be a unique command-line substring of at least 8 characters."
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss-fff"
$backupRoot = Join-Path $serverRoot (Join-Path ".testplex-backups" $timestamp)
$copiedNames = @()
$deploymentStarted = $false

try {
    $before = Get-TestServerProcesses -Match $processMatch
    if ($before.Count -gt 0) {
        Invoke-ConfiguredScript -Path $stopScript
        Wait-ForServerProcessState -Match $processMatch -ShouldBeRunning $false -TimeoutSeconds 180
    }

    New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null
    foreach ($jar in $jarFiles) {
        $destination = Join-Path $pluginsDir $jar.Name
        if (Test-Path -LiteralPath $destination -PathType Leaf) {
            Copy-Item -LiteralPath $destination -Destination (Join-Path $backupRoot $jar.Name) -Force
        }
    }

    foreach ($jar in $jarFiles) {
        $destination = Join-Path $pluginsDir $jar.Name
        Copy-Item -LiteralPath $jar.FullName -Destination $destination -Force
        $copiedNames += $jar.Name
    }
    $deploymentStarted = $true

    Invoke-ConfiguredScript -Path $startScript
    Wait-ForServerProcessState -Match $processMatch -ShouldBeRunning $true -TimeoutSeconds 180

    Write-Host "Deployment successful. Installed $($jarFiles.Count) JAR files into $pluginsDir"
    Write-Host "Backup directory: $backupRoot"
}
catch {
    $deploymentError = $_
    Write-Error "Deployment failed: $deploymentError"

    if ($deploymentStarted -or $copiedNames.Count -gt 0) {
        try {
            $running = Get-TestServerProcesses -Match $processMatch
            if ($running.Count -gt 0) {
                Invoke-ConfiguredScript -Path $stopScript
                Wait-ForServerProcessState -Match $processMatch -ShouldBeRunning $false -TimeoutSeconds 180
            }

            foreach ($name in $copiedNames) {
                $destination = Join-Path $pluginsDir $name
                $backup = Join-Path $backupRoot $name
                if (Test-Path -LiteralPath $backup -PathType Leaf) {
                    Copy-Item -LiteralPath $backup -Destination $destination -Force
                }
                elseif (Test-Path -LiteralPath $destination -PathType Leaf) {
                    Remove-Item -LiteralPath $destination -Force
                }
            }

            Invoke-ConfiguredScript -Path $startScript
            Wait-ForServerProcessState -Match $processMatch -ShouldBeRunning $true -TimeoutSeconds 180
            Write-Warning "Rollback attempted. Previous JARs were restored from $backupRoot where available."
        }
        catch {
            Write-Error "Rollback or server restart also failed: $_"
        }
    }
    throw $deploymentError
}
