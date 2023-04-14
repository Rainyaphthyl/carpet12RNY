Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-current-undefined`

# 1. Better Item Counter

## 1.1. Fancier Display

The `/counter` command uses better reporting format with features:

- Displaying the total duration of item counting.
- Displaying the total and average of all items.
- Using bold or italic style on some important information.

(some of which ported from [fabric-carpet](https://github.com/gnembon/fabric-carpet))

## 1.2. Statistics and Error Analysis

Work in Progress...

Calculates the Variance, the Standard Deviation, and the Standard Error of the counted samples.

# 2. Better Item Logger

## 2.1. New Rule

### 2.1.1. Item Logger Ignoring Counters

Item Logger will not report the items killed by cactus when the Cactus Counter is on, etc.

- Name: `itemLoggerIgnoringCounters`
- Options: `true`, `false`
- Default: `true`

## 2.2. New Features

1. Displays the name, ID, metadata, and stacking size of the logged items.
2. The option `minimal` of `log items` is removed, and replaced with the `itemLoggerIgnoringCounters` rule.
3. Merges the records with repeated positions in the `full` logger, to avoid `DespawnTimer` spamming.

# 3. Ported Features

## 3.1. New Rules

All ported from TIS-CM by [Fallen-Breath](https://github.com/Fallen-Breath/carpetmod112).

### 3.1.1. Block Event Packet Range

Set the range where player will receive a block event packet after a block event fires successfully.

- Name: `blockEventPacketRange`
- Options: `0.0`, `16.0`, `64.0`, `128.0`
- Default: `64.0`

### 3.1.2. Explosion Packet Range

Set the range where player will receive an explosion packet when an explosion happens.

- Name: `explosionPacketRange`
- Options: `0.0`, `16.0`, `64.0`, `128.0`, `2048.0`
- Default: `64.0`

### 3.1.3. Entity Tracker Distance

The maximum horizontal chebyshev distance (in chunks) for the server to sync entities information to the client.

- Name: `entityTrackerDistance`
- Options: `-1`, `16`, `64`
- Default: `-1`

# 4. Miscellaneous

## 4.1. Bug-fixes

### 4.1.1. Lifetime Tracker with 0.00-min

Previous feature: Lifetime tracker (`/lifetime`) always reports "tracked 0.00 min" when using mode of in-game timing, if game rule `doDaylightCycle` is `false`.

Fix: The tick counter works correctly even if `doDaylightCycle` is `false`.

## 4.2. Codes and Styles

1. Methods of silent chunk loading have been separated from the vanilla codes, reserving the vanilla method parameters.
2. Color marks of default values can be displayed correctly on carpet rules with Double type.
