# Arcade tester access

Grant a player `testplex.arcade.tester` to let them use the Game Panel, per-game debug controls and test-bot setup without making them an operator. The permission is declared by both Testplex Modern plugins and defaults to false.

With LuckPerms, run this from the server console or as an administrator:

```text
lp user <player> permission set testplex.arcade.tester true
```

To revoke access:

```text
lp user <player> permission unset testplex.arcade.tester
```

The server owner can also use **Game Panel → Give Co-Host** to grant and revoke the same panel/debug access through the lobby menu.
