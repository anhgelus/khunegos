# Getting started

Khunegos is a simple server-side [Fabric](https://fabricmc.net/) mod requiring [Fabric API](https://modrinth.com/mod/fabric-api/).
It will never work on a client.

## Installation

Like every other mods, put the jar in the `mods` folder.
If you have not installed *Fabric API* yet, add it to the same folder.

## Launch

Before launching the server, you must give the timezone to use to java.
You can do it with the parameter `-Duser.timezone=Europe/Paris` for the timezone Europe/Paris for example.
If you are on Linux, you can also use environment variable `TZ` to set it.

## Configuration

The mod does not have a configuration file.

You can configure all important things with gamerules.
Their prefix is `khunegos:`.

To enable/disable khunegos, set to true/false the gamerule `khunegos:enable` 
(e.g., `/gamerule khunegos:enable true` to enable it).
By default, it is set to false.

To modify the minimum number of players to start the first khunegos, modify `khunegos:minPlayers`.
For your information, the first khunegos is started when a random amount between the minimum and the minimum + 2 is 
reached.
It must be larger or equal to **2**.
By default, it is set to 3.

To modify the minimum health and the maximum health, modify `khunegos:minHealth` and `khunegos:maxHealth`.
By default, the minimum is 5 and the maximum is 15.