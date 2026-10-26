# mineplex-reborn

## Credits:
- nekoli, for the maps!

## Before compiling
Some feature might be stripped from the original source due to lack of resources (multi protocol support, missing resources...)
Feel free to add them back by checking git diff or even better recompile everything from [**original source**](https://git.crepe.moe/timing/mineplex-original)

## Requirements
1. [Java 8](https://adoptium.net/temurin/releases/)
2. [Redis](https://redis.io/docs/install/install-redis/)
3. [MySQL](https://www.mysql.com/)
4. API Server (You can use [fakeplex-api](https://github.com/KyleS1872/fakeplex-api) or [MineplexMonitor](https://github.com/timing1337/MineplexMonitor))
5. [ViaVersion](https://ci.viaversion.com/job/ViaVersion/)

## Compiling
1. Clone the repo
2. Change spigot path to your ``libs\spigot-1.8.8-1.9-SNAPSHOT.jar`` and ``libs\bungeecord-bootstrap-1.8-SNAPSHOT.jar`` in pom.xml

```
<dependency>
    <groupId>com.mineplex</groupId>
    <artifactId>spigot</artifactId>
    <version>1.8.8-1.9-SNAPSHOT</version>
    <scope>system</scope>
    <systemPath>path\file\libs\spigot-1.8.8-1.9-SNAPSHOT.jar</systemPath>
</dependency>
```

```
<dependency>
    <groupId>net.md-5</groupId>
    <artifactId>bungeecord-bootstrap</artifactId>
    <version>1.8-SNAPSHOT</version>
    <scope>system</scope>
    <systemPath>path\file\libs\bungeecord-bootstrap-1.8-SNAPSHOT.jar</systemPath>
</dependency>
```

3. ``mvn install``

## Arcade testbed (experimental)

The `Testplex Java 8 build` GitHub Actions workflow compiles the Maven reactor, validates the native gameplay/cosmetic hooks, and uploads the resulting `Plugins/out/*.jar` plugin set as a versioned artifact.

- [In-game acceptance checklist](docs/IN-GAME-TEST-PLAN.md)
- [Isolated Windows test-server setup and gated deployment](docs/testbed-deployment.md)
- [Current testbed pull request](https://github.com/joshbet9/testplex/pull/1)

The workflow does **not** change a live server by default. Automatic deployment requires an isolated Windows self-hosted runner and the server-specific variables/scripts described in the setup guide. Build success is not a substitute for running the in-game checklist on that isolated server.


### Monster Maze native variants

The experimental branch also registers `MonsterMazeSpeed` and `MonsterMazeModern` in Reborn's existing Arcade schema. Both reuse the native map pool, game lifecycle and kit infrastructure; the original `MonsterMaze` remains registered. Do **not** install the standalone Monster Maze plugin on this server. See [the native Monster Maze test section](docs/IN-GAME-TEST-PLAN.md) for the exact matrix, pattern setting and PB checks.


When GitHub Actions minutes are unavailable, run the local Java 8 build and source checks with `powershell -ExecutionPolicy Bypass -File .github/scripts/validate-monster-maze.ps1`. Use `-SkipBuild` only for a quick source-contract check; it does not replace compiling `Arcade.jar`.
