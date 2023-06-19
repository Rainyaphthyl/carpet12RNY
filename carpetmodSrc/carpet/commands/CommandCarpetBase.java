package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.Messenger;
import carpet.utils.SilentChunkReader;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.WorldServer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public abstract class CommandCarpetBase extends CommandBase {
    /**
     * Uses the block above the target, if the target is recognized as a floor block.
     */
    @Nonnull
    @ParametersAreNonnullByDefault
    public static List<String> getTabCompletionMobPlace(ICommandSender sender, String[] inputArgs, int index, @Nullable BlockPos target) {
        List<String> list = new ArrayList<>();
        Set<BlockPos> posList = new TreeSet<>();
        if (target == null) {
            Vec3d posBase = sender.getPositionVector();
            BlockPos posOutput = new BlockPos(posBase);
            posList.add(posOutput);
            list.add("~");
        } else {
            if (sender instanceof EntityPlayerMP) {
                EntityPlayerMP player = (EntityPlayerMP) sender;
                WorldServer world = player.getServerWorld();
                SilentChunkReader access = world.silentChunkReader;
                if (access.isChunkValid(target, true)) {
                    IBlockState stateTarget = access.getBlockState(target);
                    if (!access.isCreaturePlaceable(target, EntityLiving.SpawnPlacementType.ON_GROUND)
                            && stateTarget.isTopSolid() && target.getY() < world.getHeight() - 1) {
                        posList.add(target.up());
                    }
                }
                posList.add(target);
            }
        }
        int axis = inputArgs.length - 1 - index;
        for (BlockPos posOutput : posList) {
            switch (axis) {
                case 0:
                    list.add(Integer.toString(posOutput.getX()));
                    break;
                case 1:
                    list.add(Integer.toString(posOutput.getY()));
                    break;
                case 2:
                    list.add(Integer.toString(posOutput.getZ()));
                    break;
            }
        }
        return list;
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    public static List<String> getTabCompletionCoordinateExact(ICommandSender sender, String[] inputArgs, int index, @Nullable BlockPos target, int decimals) {
        Vec3d posBaseE = sender.getPositionVector();
        BlockPos posBaseB = sender.getPosition();
        List<String> list = new ArrayList<>();
        int i = inputArgs.length - 1;
        double posCurrE;
        int posCurrB;
        int posTarget;
        switch (i - index) {
            case 0:
                posCurrE = posBaseE.x;
                posCurrB = posBaseB.getX();
                if (target != null) {
                    posTarget = target.getX();
                } else {
                    posTarget = posCurrB;
                }
                break;
            case 1:
                posCurrE = posBaseE.y;
                posCurrB = posBaseB.getY();
                if (target != null) {
                    posTarget = target.getY();
                } else {
                    posTarget = posCurrB;
                }
                break;
            case 2:
                posCurrE = posBaseE.z;
                posCurrB = posBaseB.getZ();
                if (target != null) {
                    posTarget = target.getZ();
                } else {
                    posTarget = posCurrB;
                }
                break;
            default:
                return list;
        }
        list.add("~");
        if (decimals > 0 && decimals < 8) {
            String formatter = "%." + decimals + 'f';
            list.add(String.format(formatter, posCurrE));
        }
        list.add(Double.toString(posCurrE));
        list.add(Integer.toString(posCurrB));
        if (posTarget != posCurrB) {
            list.add(Integer.toString(posTarget));
        }
        return list;
    }

    @SuppressWarnings("unused")
    @Nonnull
    @ParametersAreNonnullByDefault
    public static List<String> getTabCompletionCoordinateExact(ICommandSender sender, String[] inputArgs, int index, @Nullable BlockPos target) {
        return CommandCarpetBase.getTabCompletionCoordinateExact(sender, inputArgs, index, target, -1);
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    public void msg(ICommandSender sender, @Nonnull List<ITextComponent> texts) {
        msg(sender, texts.toArray(new ITextComponent[0]));
    }

    public void msg(ICommandSender sender, ITextComponent... texts) {
        if (sender instanceof EntityPlayer) {
            for (ITextComponent t : texts) sender.sendMessage(t);
        } else {
            for (ITextComponent t : texts) notifyCommandListener(sender, this, t.getUnformattedText());
        }
    }

    public void msgFormatted(ICommandSender sender, List<ITextComponent> texts) {
        if (texts != null) {
            msgFormatted(sender, texts.toArray(new ITextComponent[0]));
        }
    }

    public void msgFormatted(ICommandSender sender, ITextComponent... texts) {
        if (sender != null) {
            for (ITextComponent t : texts) sender.sendMessage(t);
        }
    }

    public boolean command_enabled(String command_name, ICommandSender sender) {
        if (!CarpetSettings.get(command_name).equalsIgnoreCase("true")) {
            msg(sender, Messenger.m(null, "w Command is disabled in carpet settings"));
            if (!(sender instanceof EntityPlayer)) return false;
            if (CarpetSettings.locked) {
                Messenger.m(sender, "gi Ask your admin to enable it server config");
            } else {
                Messenger.m(sender,
                        "gi copy&pasta \"",
                        "gib /carpet " + command_name + " true", "/carpet " + command_name + " true",
                        "gi \"to enable it");
            }
            return false;
        }
        return true;
    }

    protected int parseChunkPosition(@Nonnull String arg, int base) throws NumberInvalidException {
        return arg.equals("~") ? base >> 4 : parseInt(arg);
    }
}
