Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-build-8-20231123`

# TO-DO List

1. Bump to release versions after merging pathfinding-logger?
2. Add another mode of mob-cap logger.
3. Make command `/spawn list` smarter: parsing the block position above the floor.
4. Add a spawning rate calculator.
5. Define and throw `ChunkNotGeneratedException`.
6. Make command `/perimeterinfo` check multiple entities.
7. Better hopper counters.

# Feature Modifications

## Better Hopper Counters

1. Ported rule `hopperCountersUnlimitedSpeed`.
2. Ported rule `hopperNoItemCost`.

## Lifetime Tracker

1. Add rule `lifetimeTrackBySize`.
2. Chicken jockeys and spider jockeys will be tracked.

## Miscellaneous

1. Fix a bug of "Nether >> Overworld" coordinate display of the Portal Cache Logger: the range was `1.0` and should be `0.125`.

# Code Details
