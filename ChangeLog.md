Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-build-4-********`

# TO-DO List

- Bump to release versions after merging pathfinding-logger?
- Improve SpawnReporter: to report passive mob cap every tick.
- Add another mode of mob-cap logger.

# Feature Modifications

- Fix a display bug about symbol `%` of command `/spawn` on the server console.
- Add `pathFinding` logger, displaying entity AI paths with particle lines.
- Display the actual default option when setting logger with null options, i.e. `Subscribed to <logger> \(<option>\)`.
- Add command `/log copy <player>`, to copy logger configurations from other players.
- Fix a null-pointer bug of command `/log reset`.

# Code Details

- Add field `NAME` for logHelpers of `lightCheck` and `rngManip`.
