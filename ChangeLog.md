Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-build-5-20230616-plus`

# TO-DO List

- Bump to release versions after merging pathfinding-logger?
- Add another mode of mob-cap logger.
- Make command `/spawn list` smarter: parsing the block position above the floor.
- Add a spawning rate calculator.
- Define and throw `ChunkNotGeneratedException`.
- Make command `/perimeterinfo` check multiple entities.
- Add modes of MSPT logger: `peak`, `average`, `moment`.
- Calculate the actual TPS when a single tick runs for more than 20 ms (`1 / max(50, ms)`).

# Feature Modifications

- Improved help info of command `/spawn`.
- More options added to command "profile": `/profile (health|entities) <ticks>`. Command `/tick` can be disabled in vanilla survival, totally replaced by `/profile` without `/tick (rate|warp)`.

# Code Details

- *None*
