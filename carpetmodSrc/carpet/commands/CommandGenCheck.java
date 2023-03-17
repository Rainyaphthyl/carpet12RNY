package carpet.commands;

import carpet.CarpetSettings;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CommandGenCheck extends CommandCarpetBase {

    /**
     * Calling {@link net.minecraft.world.gen.structure.MapGenStructure} {@code canSpawnStructureAtCoords}
     */
    @Nonnull
    public static ChunkPos getPosInCurrRegion(@Nonnull World world, @Nonnull StructureType structureType, int chunkX, int chunkZ) {
        int aimX = 0, aimZ = 0;
        int spacing = 0, separation = 0;
        switch (structureType) {
            case MONUMENT:
                spacing = 32;
                separation = 5;
                if (chunkX < 0) {
                    chunkX -= spacing - 1;
                }
                if (chunkZ < 0) {
                    chunkZ -= spacing - 1;
                }
                int k = chunkX / spacing;
                int l = chunkZ / spacing;
                Random random = getTempRandom(world.getSeed(), k, l, 10387313);
                k *= spacing;
                l *= separation;
                k += (random.nextInt(spacing - separation) + random.nextInt(spacing - separation)) / 2;
                l += (random.nextInt(spacing - separation) + random.nextInt(spacing - separation)) / 2;
                break;
            case TEMPLE:
            case MANSION:
            default:
        }
        return new ChunkPos(aimX, aimZ);
    }

    @Nonnull
    private static Random getTempRandom(long worldSeed, int seedK, int seedL, int seedC) {
        long localSeed = (long) seedK * 341873128712L + (long) seedL * 132897987541L + worldSeed + (long) seedC;
        Random random = new Random();
        random.setSeed(localSeed);
        return random;
    }

    @Override
    @Nonnull
    public String getName() {
        return "genCheck";
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public String getUsage(ICommandSender sender) {
        return "Usage: \"/genCheck <structure> <monument|temple|hut|pyramid|igloo> [<chunk-X> <chunk-Z>]\"";
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!command_enabled("commandGenCheck", sender)) {
            return;
        }
        if (args.length >= 2) {
            if ("structure".equalsIgnoreCase(args[0])) {
                StructureType aimedType = null;
                for (StructureType structureType : StructureType.values()) {
                    if (structureType.name().equalsIgnoreCase(args[1])) {
                        aimedType = structureType;
                        break;
                    }
                }
                int chunkX = 0, chunkZ = 0;
                if (aimedType != null) {
                    if (args.length >= 4) {
                        chunkX = parseChunkPosition(args[2], sender.getPosition().getX());
                        chunkZ = parseChunkPosition(args[3], sender.getPosition().getZ());
                    } else {
                        chunkX = sender.getPosition().getX() >> 4;
                        chunkZ = sender.getPosition().getZ() >> 4;
                    }
                    ChunkPos chunkPos = getPosInCurrRegion(sender.getEntityWorld(), aimedType, chunkX, chunkZ);
                }
            } else {
                throw new CommandException(getUsage(sender));
            }
        } else {
            throw new CommandException(getUsage(sender));
        }
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (!CarpetSettings.commandGenCheck) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "structure");
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, "monument", "temple");
        }
        return Collections.emptyList();
    }

    enum StructureType {
        MONUMENT, TEMPLE, MANSION,
    }
}
