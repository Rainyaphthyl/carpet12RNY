Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-build-3-20230524`

# TO-DO List

- Add command `/log copy <player>` or `/logCopy <player>`, to copy logger configurations from other players.
- Add field `NAME` for all the logHelpers.
- Display the actual option when setting logger with default options.
    - `Subscribed to <logger> \(<option>\)`
- Bump to release versions after merging pathfinding-logger?

# Feature Modifications

- Add HUD logger `tickWarp` with options: `bar`, `value`.
- Add options `interrupt` and `status` for command `/tick warp`.
- Fix a unicode encoding bug of command `/palette <posInfo>`.
- Fix the bug that carpet rule `updateSuppressionCrashFix` fails to protect the server from crashes caused by *Instant Tile Tick* loops of Pressure Plates, Tripwire, Detector Rail, and Frosted Ice.
