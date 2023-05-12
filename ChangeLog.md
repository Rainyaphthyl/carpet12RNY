Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-build-1-20230512`

# Explosion Logger

## Option Modifications

- Remove option `compact`;
- Add option `full` and `harvest`;
- Other options to add (pending): `injure`, `motion`, etc;

Command usage: `log explosions (brief|full|harvest)`

- Option `brief` is kept same as the old one.

### Logger: "Full"

The explosion logger for all blocks and all entities, where the entity part is ported from fabric carpet with some modifications, and the block part is newly added.

### Logger: "Harvest"

The explosion logger for blocks and item entities, where Moving Pistons (block-36) and blocks with 100% dropping chance are specially marked as "harvested". TNT activations are ignored.

Format:
> #`<count_tnt>` => `<position>` (creates fire) or (doesn't create fire)
>
> blocks harvested: `<count_0>` , destroyed: `<count_1>` ; item stacks damaged: `<count_2>`

If `<count_i> == 0`, the respective info will be marked with a dark color.

## Other Details

- The logger uses the tick counter with the same output of the command `time query gametime` when displaying the "tick", instead of the wrong one.
- The "affects blocks" info can be displayed correctly.

# Player Light Check

Ported from [TISCarpet113](https://github.com/TISUnion/TISCarpet113), with some modifications.

## New Rules

### Player Light Check

Disable random light checks near players, or modify the frequency of checks.

- Name: `disablePlayerLightCheck`
- Type: `Enum`
- Options & Range: `vanilla`, `suppress`, `flood`
    - `vanilla`: No modifications.
    - `suppress`: Player light check disabled.
    - `flood`: Immediately updating lights at all possible positions.
- Default: `vanilla`

## New Loggers

### Player Light Check Logger

- Log Handler: `CHAT`
- Log Options:
    - `raw`: Displays the raw position where the light is updated.
    - `relative`: Displays the relative displacement from the nearby player, maybe useful to debug light suppression by RNG manipulation.
    - `verbose`: Combines `raw` and `relative`, displaying with 2 lines per log.
- Default Option: `raw`

# Miscellaneous

## Features

- Add checks for existing jar file in Carpet Updater.
- Fix the bug that lifetime tracker reports 0-gt when game rule `doMobSpawning` is `false`.
- Add command `logMenu` to instantly display the updated interactive logger menu.
- Fix the bug that command `blockinfo` can load chunks.

## Codes and Details

- Updates carpet server on `RNY-current-undefined` versions.
- Add class `SilentChunkReader`, with codes from `PortalSilentSearcher`.
- Begin to use "build" version numbers with format `RNY-build-<count>-<date>`.
