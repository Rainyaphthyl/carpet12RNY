Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-current-undefined`

## Rules

## Tools and Commands

## Features and Details

### Fix: Lifetime Tracker with 0.00-min

Previous feature: Lifetime tracker (`/lifetime`) always reports "tracked 0.00 min" when using mode of in-game timing, if game rule `doDaylightCycle` is `false`.

Fix: The tick counter works correctly even if `doDaylightCycle` is `false`.

### Item Logger Details

Displays the name, ID, metadata, and stacking size of the logged items.
