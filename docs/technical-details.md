# Technical details behind a khunegos

Important technical details for advanced players and servers' administrators.

## Players

A prey can disconnect during a khunegos.
The khunegos will continue and the hunter continue to get their coords.
In this condition, the hunter win if they go to their exact coords.

The first khunegos is started if they are a random amount of players between the minimum 
(set by the [configuration](/getting-started#configuration)) and minimum + 2.

If the first khunegos is already started, a new one has a probability of 1/2 to start when a new player joins the server.

When a player disconnect, the number of incoming khunegos is reduced if needed.

## Times

The duration of a khunegos is random. 
It is near the value set by the gamerule `khunegos:duration`.

The delay between two khunegos is also random.
It is near the value set by the gamerule `khunegos:delay`.

The delay before starting the first khunegos is, as always, random.
It is roughly between 0 and 5 minutes after the connexion of the last players.

## Restart

When the server restart, the current khunegos is not saved.