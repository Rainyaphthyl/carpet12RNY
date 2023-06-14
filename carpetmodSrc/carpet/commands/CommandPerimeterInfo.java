package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.perimeter.PerimeterCalculator;
import net.minecraft.command.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandPerimeterInfo extends CommandCarpetBase {
    private static final String USAGE = "/perimeterinfo <x> <y> <z> [<dimension> [<target_entity>]]";

    @Nonnull
    @ParametersAreNonnullByDefault
    public static List<String> getTabCompletionCoordinateExact(ICommandSender sender, String[] inputArgs, int index, @Nullable BlockPos target) {
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
                posTarget = posCurrB;
                if (target != null) {
                    posTarget = target.getX();
                }
                break;
            case 1:
                posCurrE = posBaseE.y;
                posCurrB = posBaseB.getY();
                posTarget = posCurrB;
                if (target != null) {
                    posTarget = target.getY();
                }
                break;
            case 2:
                posCurrE = posBaseE.z;
                posCurrB = posBaseB.getZ();
                posTarget = posCurrB;
                if (target != null) {
                    posTarget = target.getZ();
                }
                break;
            default:
                return list;
        }
        list.add("~");
        list.add(Double.toString(posCurrE));
        list.add(Integer.toString(posCurrB));
        if (posTarget != posCurrB) {
            list.add(Integer.toString(posTarget));
        }
        return list;
    }

    @Override
    @Nonnull
    public String getName() {
        return "perimeterinfo";
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public String getUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!command_enabled("commandPerimeterInfo", sender)) {
            return;
        }
        if (args.length < 3) {
            throw new WrongUsageException(USAGE);
        } else {
            Vec3d posBase = sender.getPositionVector();
            double posX = parseDouble(posBase.x, args[0], true);
            double posY = parseDouble(posBase.y, args[1], false);
            double posZ = parseDouble(posBase.z, args[2], true);
            Vec3d posCenter = new Vec3d(posX, posY, posZ);
            World world = null;
            Class<? extends EntityLiving> entityType = null;
            if (args.length > 3) {
                DimensionType dimension;
                try {
                    if ("overworld".equalsIgnoreCase(args[3])) {
                        dimension = DimensionType.OVERWORLD;
                    } else if ("nether".equalsIgnoreCase(args[3])) {
                        dimension = DimensionType.NETHER;
                    } else if ("end".equalsIgnoreCase(args[3])) {
                        dimension = DimensionType.THE_END;
                    } else {
                        dimension = DimensionType.getById(parseInt(args[5], -1, 1));
                    }
                } catch (NumberInvalidException e) {
                    throw new WrongUsageException(USAGE);
                }
                world = server.getWorld(dimension.getId());
                if (args.length > 4) {
                    ResourceLocation resourcelocation = new ResourceLocation(args[4]);
                    Class<? extends Entity> rawClass = EntityList.REGISTRY.getObject(resourcelocation);
                    if (rawClass != null && EntityLiving.class.isAssignableFrom(rawClass)) {
                        entityType = rawClass.asSubclass(EntityLiving.class);
                    } else {
                        throw new EntityNotFoundException(args[4] + " is not a valid creature class");
                    }
                }
            } else {
                world = sender.getEntityWorld();
            }
            PerimeterCalculator.asyncSearch(world, posCenter, entityType);
        }
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (!CarpetSettings.commandPerimeterInfo) {
            return Collections.emptyList();
        } else {
            switch (args.length) {
                case 1:
                case 2:
                case 3:
                    return getTabCompletionCoordinateExact(sender, args, 0, targetPos);
                case 4:
                    return getListOfStringsMatchingLastWord(args, "nether", "overworld", "end");
                case 5:
                    return getListOfStringsMatchingLastWord(args, EntityList.getEntityNameList());
                default:
                    return Collections.emptyList();
            }
        }
    }
}
