Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-dev-20230407-0119`

## Rules

### Command Switches

- `commandPortal`, default: `true`
- `commandLifeTime`, default: `true`

## Tools and Commands

### Spawn Tracker Restarting

Stops (if the tracker is running) and restart the spawn tracker, combining the commands `/spawn tracking stop` and `/spawn tracking start`.

- Command: `/spawn tracking restart [<X1 Y1 Z1 X2 Y2 Z2>]`

### Lifetime Tracker

Ported from [Fallen-Breath/carpetmod112](https://github.com/Fallen-Breath/carpetmod112), which has been ported from [Carpet TIS Addition](https://github.com/TISUnion/Carpet-TIS-Addition) v1.20.0 with code modified from [TISCarpet13](https://github.com/TISUnion/TISCarpet113).

Original links:
- [Pull Request](https://github.com/gnembon/carpetmod112/pull/156)
- [Document](https://github.com/TISUnion/Carpet-TIS-Addition/blob/master/docs/commands.md#lifetime)

### Portal Calculator

Searches and calculates the Nether Portal (pattern) position mapped from the given `<x> <y> <z>` coordinates.

- Partial Command currently available: `/portal from point <x> <y> <z> [dimension]`
- *Full Command (WIP): "/portal (from|to) (point|range) <x> <y> <z> [dimension]"*

## Features and Details

### Smarter Logger Switch

If the player is subscribed to another option of the log, it switches to the intended option **directly** by command `/log <logName> <new-option>`. No need to `/log <logName> clear`.

### Default Logger Crash Fix

Fixed the crash of `/log` after `/log <logName>` with default options (e.g. `/log items` with `brief` by default).

### Silent Chunk Loader

The command `/portal` only reads data from the chunks currently generated, without making other chunks generated. The generated chunks will be loads silently, without being made active.

It helps to keep the game VANILLA and can be used for other carpet commands.
