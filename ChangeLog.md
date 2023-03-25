Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-current-undefined`

## Rules

## Tools and Commands

### Spawn Tracker Restarting

Stops (if the tracker is running) and restart the spawn tracker, combining the commands `/spawn tracking stop` and `/spawn tracking start`.

- Command: `/spawn tracking restart [<X1 Y1 Z1 X2 Y2 Z2>]`

### Lifetime Tracker

Ported from [Fallen-Breath/carpetmod112](https://github.com/Fallen-Breath/carpetmod112), which has been ported from [Carpet TIS Addition](https://github.com/TISUnion/Carpet-TIS-Addition) v1.20.0 with code modified from [TISCarpet13](https://github.com/TISUnion/TISCarpet113).

Original links:
- [Pull Request](https://github.com/gnembon/carpetmod112/pull/156)
- [Document](https://github.com/TISUnion/Carpet-TIS-Addition/blob/master/docs/commands.md#lifetime)

## Features and Details

### Smarter Logger Switch

If the player is subscribed to another option of the log, it switches to the intended option **directly** by command `/log <logName> <new-option>`. No need to `/log <logName> clear`.

### Default Logger Crash Fix

Fixed the `/log` crash after `/log <logName>` with default options (e.g. `/log items` with `brief` by default).
