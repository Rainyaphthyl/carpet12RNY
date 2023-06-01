package carpet.utils;

import carpet.CarpetServer;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Consumer;

/**
 * {@link net.minecraft.world.WorldEntitySpawner#findChunksForSpawning}
 */
public class PerimeterCalculator implements Runnable {
    private final Class<? extends EntityLiving> entityType;
    private final WorldServer worldServer;
    private final Vec3d center;
    private final Map<BlockPos, Boolean> searchedFlags = new HashMap<>();
    private final Map<BlockPos, Int2BooleanMap> wanderedFlags = new HashMap<>();
    private SilentChunkReader reader = null;
    private Set<ChunkPos> eligibleChunks = null;
    private PerimeterResult result = null;
    private BlockPos worldSpawnPoint = null;

    private PerimeterCalculator(WorldServer worldServer, Vec3d center, Class<? extends EntityLiving> entityType) {
        this.worldServer = worldServer;
        this.center = center;
        this.entityType = entityType;
    }

    public static void asyncSearch(World world, Vec3d center, Class<? extends EntityLiving> entityType) {
        if (world instanceof WorldServer) {
            Messenger.print_server_message(world.getMinecraftServer(), "Start checking perimeter ...");
            PerimeterCalculator calculator = new PerimeterCalculator((WorldServer) world, center, entityType);
            HttpUtil.DOWNLOADER_EXECUTOR.submit(calculator);
        }
    }

    private void setEmptyResult() {
        result = PerimeterResult.getEmptyResult();
    }

    private void setEligibleChunks() {
        if (eligibleChunks == null) {
            // eligible chunks for a virtual player at the perimeter center
            // ignoring the outermost circle (only used for mobCap count)
            Set<ChunkPos> tempSet = new LinkedHashSet<>();
            int chunkX = MathHelper.floor(center.x / 16.0);
            int chunkZ = MathHelper.floor(center.z / 16.0);
            // checking player chunk map
            int radius = Math.min(CarpetServer.minecraft_server.getPlayerList().getViewDistance(), 7);
            WorldBorder worldBorder = worldServer.getWorldBorder();
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    ChunkPos chunkPos = new ChunkPos(chunkX + dx, chunkZ + dz);
                    if (worldBorder.contains(chunkPos)) {
                        tempSet.add(chunkPos);
                    }
                }
            }
            eligibleChunks = tempSet;
        }
    }

    /**
     * Select a random block position in the chunk
     */
    private void forEachChunkPosition(@Nonnull ChunkPos chunkPos, Consumer<BlockPos> consumer) {
        final int originX = chunkPos.x * 16;
        final int originZ = chunkPos.z * 16;
        Chunk chunk = reader.getChunk(chunkPos);
        int height = MathHelper.roundUp(chunk.getHeight(new BlockPos(originX + 8, 0, originZ + 8)) + 1, 16);
        if (height <= 0) {
            height = chunk.getTopFilledSegment() + 15;
        }
        BlockPos.MutableBlockPos posIter = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; ++dx) {
            final int x = originX + dx;
            for (int dz = 0; dz < 16; ++dz) {
                final int z = originZ + dz;
                posIter.setPos(x, 0, z);
                for (int y = 0; y < height; ++y) {
                    posIter.setY(y);
                    consumer.accept(posIter);
                }
            }
        }
    }

    /**
     * Wandering from [-5, 0, -5] to [+5, 0, +5] for one round
     */
    private void forEachWanderingSpawn(@Nonnull BlockPos posBegin, int rounds, Consumer<BlockPos> consumer) {
        if (rounds <= 0) {
            return;
        }
        Int2BooleanMap partialFlags;
        if (wanderedFlags.containsKey(posBegin)) {
            partialFlags = wanderedFlags.get(posBegin);
            if (partialFlags.containsKey(rounds)) {
                return;
            }
        } else {
            partialFlags = new Int2BooleanOpenHashMap();
            wanderedFlags.put(posBegin.toImmutable(), partialFlags);
        }
        BlockPos.MutableBlockPos posTarget = new BlockPos.MutableBlockPos(posBegin);
        int originX = posBegin.getX();
        int originY = posBegin.getY();
        int originZ = posBegin.getZ();
        // positive & negative
        for (int dxp = 0; dxp < 6; ++dxp) {
            for (int dxn = 0; dxn < 6; ++dxn) {
                // dyp or dzp is always 0, ignored
                for (int dzp = 0; dzp < 6; ++dzp) {
                    for (int dzn = 0; dzn < 6; ++dzn) {
                        posTarget.setPos(originX + dxp - dxn, originY, originZ + dzp - dzn);
                        consumer.accept(posTarget);
                    }
                }
            }
        }
        partialFlags.put(rounds, true);
        if (rounds > 1) {
            --rounds;
            for (int dxp = 0; dxp < 6; ++dxp) {
                for (int dxn = 0; dxn < 6; ++dxn) {
                    // dyp or dzp is always 0, ignored
                    for (int dzp = 0; dzp < 6; ++dzp) {
                        for (int dzn = 0; dzn < 6; ++dzn) {
                            posTarget.setPos(originX + dxp - dxn, originY, originZ + dzp - dzn);
                            forEachWanderingSpawn(posBegin, rounds, consumer);
                        }
                    }
                }
            }
        }
    }

    /**
     * The 24-meter check for the world spawn point and the closest player
     */
    private boolean isSpawnAllowed(double mobX, double mobY, double mobZ) {
        boolean valid = center.squareDistanceTo(mobX, mobY, mobZ) >= 576.0;
        if (valid) {
            if (worldSpawnPoint == null) {
                worldSpawnPoint = reader.getSpawnPoint();
            }
            valid = worldSpawnPoint.distanceSq(mobX, mobY, mobZ) >= 576.0;
        }
        return valid;
    }

    private void countSpots() {
        // TODO: 2023/6/1,0001 Calculate the spawning rate / probability when going through the random choices
        setEmptyResult();
        setEligibleChunks();
        int chunksCounted = 0;
        int chunksTotal = eligibleChunks.size();
        // spawning attempts
        for (EnumCreatureType creatureType : EnumCreatureType.values()) {
            for (ChunkPos chunkPos : eligibleChunks) {
                forEachChunkPosition(chunkPos, posBegin -> {
                    IBlockState blockState = reader.getBlockState(posBegin);
                    if (!blockState.isNormalCube()) {
                        for (int i = 0; i < 3; ++i) {
                            //int wanders = MathHelper.ceil(Math.random() * 4.0);
                            int wanders = 4;
                            forEachWanderingSpawn(posBegin, wanders, posTarget -> {
                                if (searchedFlags.containsKey(posTarget)) {
                                    return;
                                }
                                float mobX = (float) posTarget.getX() + 0.5F;
                                float mobZ = (float) posTarget.getZ() + 0.5F;
                                int mobY = posTarget.getY();
                                boolean successful = false;
                                if (isSpawnAllowed(mobX, mobY, mobZ)) {
                                    // spawning probability calc - forEachEntityType
                                    List<Biome.SpawnListEntry> testedEntries = new ArrayList<>();
                                    for (Biome.SpawnListEntry entry : reader.getPossibleCreatures(creatureType, posTarget)) {
                                        if (entityType == entry.entityClass) {
                                            testedEntries.add(entry);
                                        }
                                    }
                                    for (Biome.SpawnListEntry entry : testedEntries) {
                                        successful = true;
                                    }
                                }
                                searchedFlags.put(posTarget.toImmutable(), successful);
                            });
                        }
                    }
                });
                ++chunksCounted;
                Messenger.print_server_message(CarpetServer.minecraft_server, "Checked spawning in chunk " + chunkPos + " (" + chunksCounted + '/' + chunksTotal + ')');
            }
        }
        //Messenger.print_server_message(CarpetServer.minecraft_server, pos.toImmutable().toString());
        //BlockPos blockpos = getRandomChunkPosition(worldServerIn, chunkpos1.x, chunkpos1.z);
        //int k1 = blockpos.getX();
        //int l1 = blockpos.getY();
        //int i2 = blockpos.getZ();
        //IBlockState iblockstate = worldServerIn.getBlockState(blockpos);
        //
        //if (!iblockstate.isNormalCube()) {
        //    int j2 = 0;
        //
        //    for (int k2 = 0; k2 < 3; ++k2) {
        //        int l2 = k1;
        //        int i3 = l1;
        //        int j3 = i2;
        //        int k3 = 6;
        //        Biome.SpawnListEntry biome$spawnlistentry = null;
        //        IEntityLivingData ientitylivingdata = null;
        //        int l3 = MathHelper.ceil(Math.random() * 4.0D);
        //        //CM fixed 4 mobs per pack
        //        if (CarpetSettings._1_8Spawning) {
        //            l3 = 4;
        //        }
        //
        //        for (int i4 = 0; i4 < l3; ++i4) {
        //            l2 += worldServerIn.rand.nextInt(6) - worldServerIn.rand.nextInt(6);
        //            i3 += worldServerIn.rand.nextInt(1) - worldServerIn.rand.nextInt(1);
        //            j3 += worldServerIn.rand.nextInt(6) - worldServerIn.rand.nextInt(6);
        //            blockpos$mutableblockpos.setPos(l2, i3, j3);
        //            float f = (float) l2 + 0.5F;
        //            float f1 = (float) j3 + 0.5F;
        //
        //            if (!worldServerIn.isAnyPlayerWithinRangeAt((double) f, (double) i3, (double) f1, 24.0D) && blockpos1.distanceSq((double) f, (double) i3, (double) f1) >= 576.0D) {
        //                if (biome$spawnlistentry == null) {
        //                    biome$spawnlistentry = worldServerIn.getSpawnListEntryForTypeAt(enumcreaturetype, blockpos$mutableblockpos);
        //
        //                    if (biome$spawnlistentry == null) {
        //                        break;
        //                    }
        //                }
        //
        //                if (worldServerIn.canCreatureTypeSpawnHere(enumcreaturetype, biome$spawnlistentry, blockpos$mutableblockpos) && canCreatureTypeSpawnAtLocation(EntitySpawnPlacementRegistry.getPlacementForEntity(biome$spawnlistentry.entityClass), worldServerIn, blockpos$mutableblockpos)) {
        //                    EntityLiving entityliving;
        //
        //                    try {
        //                        entityliving = biome$spawnlistentry.entityClass.getConstructor(World.class).newInstance(worldServerIn);
        //                    } catch (Exception exception) {
        //                        exception.printStackTrace();
        //                        return j4;
        //                    }
        //
        //                    entityliving.setLocationAndAngles((double) f, (double) i3, (double) f1, worldServerIn.rand.nextFloat() * 360.0F, 0.0F);
        //
        //                    if (entityliving.getCanSpawnHere() && entityliving.isNotColliding()) {
        //                        ientitylivingdata = entityliving.onInitialSpawn(worldServerIn.getDifficultyForLocation(new BlockPos(entityliving)), ientitylivingdata);
        //
        //                        if (entityliving.isNotColliding()) {
        //                            ++j2;
        //                            worldServerIn.spawnEntityInWorld(entityliving);
        //                        } else {
        //                            entityliving.setDead();
        //                        }
        //
        //                        if (j2 >= entityliving.getMaxSpawnedInChunk()) {
        //                            continue label134;
        //                        }
        //                    }
        //
        //                    j4 += j2;
        //                }
        //            }
        //        }
        //    }
        //}

        ////////////////////

        //for (Class<? extends EntityLiving> entityType = constructBuffer.poll();
        //     entityType != null; entityType = constructBuffer.poll()) {
        //    EnumCreatureType creatureType = EnumCreatureType.MONSTER;
        //    if (EntityAnimal.class.isAssignableFrom(entityType)) {
        //        creatureType = EnumCreatureType.CREATURE;
        //    } else if (EntityWaterMob.class.isAssignableFrom(entityType)) {
        //        creatureType = EnumCreatureType.WATER_CREATURE;
        //    } else if (EntityAmbientCreature.class.isAssignableFrom(entityType)) {
        //        creatureType = EnumCreatureType.AMBIENT;
        //    }
        //    for (Biome.SpawnListEntry entry : reader.getPossibleCreatures(creatureType, new BlockPos(center))) {
        //        if (entry.entityClass == entityType) {
        //            entityEntryMap.put(entityType, entry);
        //        }
        //    }
        //}
    }

    @Override
    public void run() {
        try {
            if (reader == null) {
                reader = worldServer.silentChunkReader;
            }
            countSpots();
            // print result
            Messenger.print_server_message(CarpetServer.minecraft_server, "Perimeter Info:");
        } catch (Exception e) {
            // failed
            Messenger.print_server_message(CarpetServer.minecraft_server, "Failed to check perimeter");
            e.printStackTrace();
        }
    }

}
