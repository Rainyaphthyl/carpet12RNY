Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-build-6-20230708`

# TO-DO List

1. Bump to release versions after merging pathfinding-logger?
2. Add another mode of mob-cap logger.
3. Make command `/spawn list` smarter: parsing the block position above the floor.
4. Add a spawning rate calculator.
5. Define and throw `ChunkNotGeneratedException`.
6. Make command `/perimeterinfo` check multiple entities.
7. Make rule `updateSuppressionCrashFix` fix the crashes caused by ClassCastException.
8. Display the causes of potential server crashes under `updateSuppressionCrashFix`.
9. Fix the bug that `updateSuppressionCrashFix` still fails under instant tile ticks.

# Feature Modifications

1. *None*

# Code Details

1. Remove method `validateInstantScheduling` and `validateInstantFallingFlag` in `CarpetSettings`.
2. Rewrite the usages of `instantScheduling` and `instantFallingFlag`.
