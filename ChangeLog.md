Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

[version](src/carpet/CarpetSettings.java): `RNY-current-undefined`

# Explosion Logger

## Option Modifications

- Removed option `compact`;
- Added option `full` and `harvest`;
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

# Miscellaneous

- Add checks for existing jar file in Carpet Updater.
