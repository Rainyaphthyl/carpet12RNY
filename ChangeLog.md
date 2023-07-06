Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-build-5-20230616-plus`

# TO-DO List

- Bump to release versions after merging pathfinding-logger?
- Add another mode of mob-cap logger.
- Make command `/spawn list` smarter: parsing the block position above the floor.
- Add a spawning rate calculator.
- Define and throw `ChunkNotGeneratedException`.
- Make command `/perimeterinfo` check multiple entities.

# Feature Modifications

- Improved help info of command `/spawn`.
- More options added to command "profile": `/profile (health|entities) <ticks>`. Command `/tick` can be disabled in vanilla survival, totally replaced by `/profile` without `/tick (rate|warp)`.
- Fixed a bug causing crashes when `/log` is executed while an invalid null option subscribes players.
- Calculate the actual TPS when a single tick runs for more than 20 ms (by `1000/avg(max(50,millis))`). Thus, `TPS` and `MSPT` imply the different information.
- More options of MSPT | TPS logger:
    - `average`: (Default) The average within 100 gt, similar to the old logger.
    - `sample`: The average within `HUDUpdateInterval`.
    - `peak`: The worst tick time within `HUDUpdateInterval`.

# Code Details

- *None*
