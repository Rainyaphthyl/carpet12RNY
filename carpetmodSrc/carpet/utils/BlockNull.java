package carpet.utils;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;

public class BlockNull extends Block {
    public static final BlockNull INSTANCE = new BlockNull(Material.AIR, MapColor.AIR);
    public static final IBlockState STATE = INSTANCE.getDefaultState();

    public BlockNull(Material blockMaterialIn, MapColor blockMapColorIn) {
        super(blockMaterialIn, blockMapColorIn);
    }
}
