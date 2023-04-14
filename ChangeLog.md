Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-current-undefined`

## Rules

### Item Logger Ignoring Counters

Item Logger will not report the items killed by cactus when the Cactus Counter is on, etc.

- Name: `itemLoggerIgnoringCounters`
- Options: `true`, `false`
- Default: `true`

### Block Event Packet Range *

Set the range where player will receive a block event packet after a block event fires successfully.

- Name: `blockEventPacketRange`
- Options: `0.0`, `16.0`, `64.0`, `128.0`
- Default: `64.0`

Ported from TISCM by [Fallen-Breath](https://github.com/Fallen-Breath/carpetmod112).

### Explosion Packet Range *

Set the range where player will receive an explosion packet when an explosion happens.

- Name: `explosionPacketRange`
- Options: `0.0`, `16.0`, `64.0`, `128.0`, `2048.0`
- Default: `64.0`

Ported from TISCM by [Fallen-Breath](https://github.com/Fallen-Breath/carpetmod112).

### Entity Tracker Distance *

The maximum horizontal chebyshev distance (in chunks) for the server to sync entities information to the client.

- Name: `entityTrackerDistance`
- Options: `-1`, `16`, `64`
- Default: `-1`

Ported from TISCM by [Fallen-Breath](https://github.com/Fallen-Breath/carpetmod112).

## Tools and Commands

## Features and Details

### Fix: Lifetime Tracker with 0.00-min

Previous feature: Lifetime tracker (`/lifetime`) always reports "tracked 0.00 min" when using mode of in-game timing, if game rule `doDaylightCycle` is `false`.

Fix: The tick counter works correctly even if `doDaylightCycle` is `false`.

### Item Logger Details

Displays the name, ID, metadata, and stacking size of the logged items.

The option `minimal` of `log items` is removed, and replaced with the `itemLoggerIgnoringCounters` rule.

Merge the records with repeated positions in the `full` logger, to avoid `DespawnTimer` spamming.

### Better Counter Report

The `/counter` command uses better reporting format, (some of which ported from [fabric-carpet](https://github.com/gnembon/fabric-carpet)) with features:

- Displaying the total duration of item counting.
- Displaying the total and average of all items.
- Using bold or italic style on some important information.

## Miscellaneous

1. Methods of silent chunk loading have been separated from the vanilla codes, reserving the vanilla method parameters.
2. Color marks of default values can be displayed correctly on carpet rules with Double type.
