package carpet.utils;

import carpet.CarpetSettings;
import carpet.helpers.HopperCounter;
import net.minecraft.block.BlockColored;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WoolTool {
    public static final PropertyEnum<EnumDyeColor> COLOR = PropertyEnum.create("color", EnumDyeColor.class);

    public static void carpetPlacedAction(EnumDyeColor color, EntityPlayer placer, BlockPos pos, World worldIn) {
        if (!CarpetSettings.carpets) {
            return;
        }
        switch (color) {
            case PINK:
                if (CarpetSettings.commandSpawn)
                    Messenger.send(placer, SpawnReporter.report(pos, worldIn));

                break;
            case BLACK:
                if (CarpetSettings.commandSpawn)
                    Messenger.send(placer, SpawnReporter.show_mobcaps(pos, worldIn));
                break;
            case BROWN:
                if (CarpetSettings.commandDistance) {
                    DistanceCalculator.report_distance(placer, pos);
                }
                break;
            case GRAY:
                if (CarpetSettings.commandBlockInfo)
                    Messenger.send(placer, BlockInfo.blockInfo(pos.down(), worldIn));
                break;
            case YELLOW:
                if (CarpetSettings.commandEntityInfo)
                    EntityInfo.issue_entity_info(placer);
                break;
            case GREEN:
                if (CarpetSettings.hopperCounters == CarpetSettings.HopperCounters.wool) {
                    EnumDyeColor under = getWoolColorAtPosition(worldIn, pos.down());
                    if (under == null) return;
                    Messenger.send(placer, HopperCounter.COUNTERS.get(under.getName()).format(false, false, true));
                } else if (CarpetSettings.hopperCounters == CarpetSettings.HopperCounters.all) {
                    Messenger.send(placer, HopperCounter.COUNTERS.get("all").format(false, false, true));
                }
                break;
            case RED:
                if (CarpetSettings.hopperCounters == CarpetSettings.HopperCounters.wool) {
                    EnumDyeColor under = getWoolColorAtPosition(worldIn, pos.down());
                    if (under == null) return;
                    HopperCounter.COUNTERS.get(under.getName()).reset(true);
                    Messenger.s(placer, String.format("%s counter reset", under));
                } else if (CarpetSettings.hopperCounters == CarpetSettings.HopperCounters.all) {
                    HopperCounter.COUNTERS.get("all").reset(true);
                    Messenger.s(placer, "Reset hopper counters");
                }
                break;
        }
    }

    public static EnumDyeColor getWoolColorAtPosition(World worldIn, BlockPos pos) {
        IBlockState state = worldIn.getBlockState(pos);
        if (state.getBlock() != Blocks.WOOL) return null;
        return state.getValue(BlockColored.COLOR);
    }
}
