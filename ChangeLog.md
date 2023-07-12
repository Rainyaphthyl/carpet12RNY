Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-build-6-20230708`

# TO-DO List

1. Bump to release versions after merging pathfinding-logger?
2. Add another mode of mob-cap logger.
3. Make command `/spawn list` smarter: parsing the block position above the floor.
4. Add a spawning rate calculator.
5. Define and throw `ChunkNotGeneratedException`.
6. Make command `/perimeterinfo` check multiple entities.

# Feature Modifications

1. Fixed the bug that `updateSuppressionCrashFix` still fails under instant tile ticks ([issue#25](https://github.com/Rainyaphthyl/carpet12RNY/issues/25#issue-1759841478)).
2. Rule `updateSuppressionCrashFix` is enabled to deal with `ClassCastException` (the magic box).
3. Display the causes of potential server crashes under `updateSuppressionCrashFix`.

# Code Details

1. Remove method `validateInstantScheduling` and `validateInstantFallingFlag` in `CarpetSettings`.
2. Rewrite the usages of `instantScheduling` and `instantFallingFlag`.
3. Add more constructors of `ThrowableSuppression`, setting the init causes.
