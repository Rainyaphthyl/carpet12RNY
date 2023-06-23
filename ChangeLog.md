Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-build-5-20230616-plus`

# TO-DO List

- Bump to release versions after merging pathfinding-logger?
- Add another mode of mob-cap logger.
- Add a spawning rate calculator.
- Define and throw `ChunkNotGeneratedException`.
- Make command `/perimeterinfo` check multiple entities.

# Feature Modifications

- Improve help info of command `/spawn`.
- Fix the bug that the calculation of mob spawning height map uses chunk-based algorithm.
- Command `/spawn list` becomes smarter: tab-complete the block position above the floor. This is also effective on command `/spawn predict`.

# Code Details

- Fix a Mojang typo in `SyntaxErrorException`.
