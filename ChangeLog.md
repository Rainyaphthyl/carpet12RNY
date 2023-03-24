Carpet 1.12 with [RNY](https://github.com/Rainyaphthyl)'s Addition

RNY-dev-20230324-1812

## Rules

### rngTrackingRange

The range of previous gameticks to track the world RNG seeds, applied for `/log rngManip`.

Uses the value of `HUDUpdateInterval` when `rngTrackingRange` is set to `0`.

- Name: `rngTrackingRange`
- Options: `0`, `4`, `8`, `20`, `40`, `80`
- Default: `0` (using `HUDUpdateInterval`)

### HUDUpdateInterval

Ported from [CrazyHPi/carpetmod112](https://github.com/CrazyHPi/carpetmod112).

- Name: `HUDUpdateInterval`
- Options: `1`, `5`, `20`, `100`
- Default: `20`

## Tools and Commands

### RNG Logger

Tracks the world RNG seeds and checks the correctness of the RNG manipulation.

- Command: `/log rngManip <RNG-App-Name>`

It shows two lines in the HUD, first of which shows the RNG seed. The second line shows the RNG fault rate if the RNG seed has been registered; otherwise showing the RNG instability or variability.

### RNG Registry

Automatically registers a "correct" RNG seed for the RNG Logger to track.

- Command: `/rng register [clear] <RNG-App-Name>`

To register an RNG seed, you need to
1. Turn on your RNG manipulating machine (if it's off);
2. Execute the command `/rng register <RNG-App-Name>`;
3. Wait for the register report: success or failure;

Execute `/rng register clear <RNG-App-Name>` to remove the registered seed.

## Features and Details

### RNG Manipulator Tracker

`<RNG-App-Name>` includes `raw`, `fortune`, `mobSpawn`, `ironFarm`, `lightCheck`, `chunkTick`, `farmer`.

- `raw`: The raw seed set by the Woodland Mansion codes.
- `fortune`: The player-mining "Ore-NG".
- `mobSpawn`: RNG Mob Farms.
- `ironFarm`: RNG Iron Golem Farms.
- `lightCheck`: Light Update Suppressors.
- `chunkTick`: Lightning Controllers, etc.
- `farmer`: Crop harvesting by farmers.
