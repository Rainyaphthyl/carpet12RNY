Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-build-3-********`

# TO-DO List

- Add command `/log copy <player>` or `/logCopy <player>`, to copy logger configurations from other players.
- Add field `NAME` for all the logHelpers.
- Display the actual option when setting logger with default options.
    - `Subscribed to <logger> \(<option>\)`
- Fix the bug that carpet rule `updateSuppressionCrashFix` does not take effect on instant tile tick loops:
  - Frost Ice (in darkness);

# Feature Modifications

- Add HUD logger `tickWarp` with options: `bar`, `value`.
- Add options `interrupt` and `status` for command `/tick warp`.
- Fix a unicode encoding bug of command `/palette <posInfo>`.
- Fix the bug that carpet rule `updateSuppressionCrashFix` does not take effect on instant tile tick loops:
  - Pressure Plates;
  - Tripwire;
  - Detector Rail;
