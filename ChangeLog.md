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

1. Improved help info of command `/spawn`.
2. More options added to command "profile": `/profile (health|entities) <ticks>`. Command `/tick` can be disabled in vanilla survival, totally replaced by `/profile` without `/tick (rate|warp)`.
3. Fixed a bug causing crashes when `/log` is executed while an invalid null option subscribes players.
4. Calculate the actual TPS when a single tick runs for more than 20 ms (by `1000/avg(max(50,millis))`). Thus, `TPS` and `MSPT` imply the different information.
5. More options of MSPT | TPS logger:
    1. `average`: (Default) The average within 100 gt, similar to the old logger.
    2. `sample`: The average within `HUDUpdateInterval`.
    3. `peak`: `MSPT` for the worst tick time, `TPS` for the average, both within `HUDUpdateInterval`.
6. Modifications of carpet profiling terms:
    1. Add `carpet`: The carpet server ticking.
    2. Add `<dim>`: Total tick time of each dimension.
    3. Add `<dim>.tile_ticks`, `<dim>.chunk_ticks`, `<dim>.block_events`, `<dim>.rest`.
    4. Remove `<dim>.blocks`.

# Code Details

- *None*
