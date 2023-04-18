Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-current-undefined`

# 1. Better Item Counter

## 1.1. Fancier Display

The `/counter` command uses better reporting format with features:

- Displaying the total duration of item counting.
- Displaying the total and average of all items.
- Using bold or italic style on some important information.
- Displaying IDs and metadata (if having subtypes) of items, to help distinguish different items with same name.

The dark red button `[X]` in the reports uses command `counter stop` instead of `counter reset`.

## 1.2. Command Modifications

### 1.2.1. Counter Stop

New command, partially different from `/counter [<color>] reset`.

- Usage: `/counter [<color>] stop`
- Effect: Stops the item counter, and set it to the initial state with `tick = 0`, waiting for items to start counting.

The argument `<color>` includes `cactus`, `all`, and Dye Colors.

### 1.2.2. Counter Raw

New command.

- Usage: `/counter [<color>] raw`
- Effect: Displays the raw data with the format of the old `/counter` command, while the `/counter` command as been modified.

### 1.2.3. Counter

Modified command, with effect different from the old command with same name.

- Usage: `/counter [<color>]`
- Effect: Displays the reliable data with error analysis and appropriate rounding of significant figures.

## 1.3. Statistics and Error Analysis

### 1.3.1. Functions

Item rates will be displayed in the format of `<average>\(<error>\)<unit>, E: <relative-error>`. The unit will be automatically chosen from `(items)/h`, `k/h`, `M/h`, and `G/h`.

The component `error` is the Standard Error calculated from $\frac{s^2}{n}$ .

For example, the raw value of rate of a Mob Farm is `183789.4/h` with standard error `238.1/h`. The relative error is `238.1 / 183789.4 = 0.130%`. The rate will be displayed as `183.79(0.24)k/h`.

Text components will be marked with different colors according to the level of relative error.

### 1.3.2. Details

To implement the distribution recording and the variance calculation, item counting has been separated into two parts:

- The first is called by hoppers and items (for the cactus counter), adding items into a temporary map instantly;
- The second runs at the phase `CarperServer.tick()`, collecting the temporary data into long-term maps.

*The estimation algorithm needs further optimization.*

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
