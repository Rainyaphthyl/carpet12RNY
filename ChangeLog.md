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

- Improve help info of command `/spawn`.
- Fix the bug that the calculation of mob spawning height map uses chunk-based algorithm.

# Code Details

- Fix a Mojang typo in `SyntaxErrorException`.
