Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-current-undefined`

## Rules

### Item Logger Ignoring Counters

Item Logger will not report the items killed by cactus when the Cactus Counter is on, etc.
- Name: `itemLoggerIgnoringCounters`
- Options: `true`, `false`
- Default: `true`

## Tools and Commands

## Features and Details

### Fix: Lifetime Tracker with 0.00-min

Previous feature: Lifetime tracker (`/lifetime`) always reports "tracked 0.00 min" when using mode of in-game timing, if game rule `doDaylightCycle` is `false`.

Fix: The tick counter works correctly even if `doDaylightCycle` is `false`.

### Item Logger Details

Displays the name, ID, metadata, and stacking size of the logged items.

The option `minimal` of `log items` is removed, and replaced with the `itemLoggerIgnoringCounters` rule.

[//]: # (Merge the repeated positions and motions in the `full` logger to avoid `DespawnTimer` spamming.)

[//]: # (### Better Counter Report)

[//]: # ()
[//]: # (The `/counter` command uses better reporting format ported from [fabric-carpet]&#40;https://github.com/gnembon/fabric-carpet&#41;, with features:)

[//]: # (- Displaying the total counting time;)

[//]: # (- Marking items with colors.)
