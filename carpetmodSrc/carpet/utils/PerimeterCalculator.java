package carpet.utils;

import carpet.CarpetServer;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
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
    private final Map<Class<? extends EntityLiving>, Biome.SpawnListEntry> entityEntryMap = new HashMap<>();
    private final Queue<Class<? extends EntityLiving>> constructBuffer = new LinkedList<>();
    private final WorldServer worldServer;
    private final Vec3d center;
    private Set<ChunkPos> eligibleChunks = null;

    public PerimeterCalculator(WorldServer worldServer, Vec3d center, Collection<Class<? extends EntityLiving>> entityTypes) {
        this.worldServer = worldServer;
        this.center = center;
        if (entityTypes != null) {
            constructBuffer.addAll(entityTypes);
        }
    }

    public PerimeterCalculator(WorldServer worldServer, Vec3d center, Class<? extends EntityLiving> entityType) {
        this.worldServer = worldServer;
        this.center = center;
        constructBuffer.add(entityType);
    }

    public static void asyncSearch(World world, Vec3d center, Collection<Class<? extends EntityLiving>> entityTypes) {
        if (world instanceof WorldServer) {
            PerimeterCalculator calculator = new PerimeterCalculator((WorldServer) world, center, entityTypes);
            Thread thread = new Thread(calculator);
            thread.start();
        }
    }

    public PerimeterResult getEmptyResult() {
        return PerimeterResult.getEmptyResult(entityEntryMap.keySet());
    }

    private synchronized Set<ChunkPos> getEligibleChunks() {
        if (eligibleChunks == null) {
            // eligible chunks for a virtual player at the perimeter center
            // ignoring the outermost circle (only used for mobCap count)
            final Set<ChunkPos> tempSet = new HashSet<>(15 * 15);
            int chunkX = MathHelper.floor(center.x / 16.0);
            int chunkZ = MathHelper.floor(center.z / 16.0);
            // checking player chunk map
            final int radius = Math.min(CarpetServer.minecraft_server.getPlayerList().getViewDistance(), 7);
            final WorldBorder worldBorder = worldServer.getWorldBorder();
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                    if (worldBorder.contains(chunkPos)) {
                        tempSet.add(chunkPos);
                    }
                }
            }
            eligibleChunks = tempSet;
        }
        return eligibleChunks;
    }

    public void searchChunkPositions(@Nonnull ChunkPos chunkPos, Consumer<BlockPos> consumer) {
        final SilentChunkReader reader = worldServer.silentChunkReader;
        int originX = chunkPos.x * 16;
        int originZ = chunkPos.z * 16;
        Chunk chunk = reader.getChunk(chunkPos);
        int height = MathHelper.roundUp(chunk.getHeight(new BlockPos(originX + 8, 0, originZ + 8)) + 1, 16);
        if (height <= 0) {
            height = chunk.getTopFilledSegment() + 15;
        }
        BlockPos.MutableBlockPos posIter = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; ++dx) {
            int x = originX + dx;
            for (int dz = 0; dz < 16; ++dz) {
                int z = originZ + dz;
                posIter.setPos(x, 0, z);
                for (int y = 0; y < height; ++y) {
                    posIter.setY(y);
                    consumer.accept(posIter);
                }
            }
        }
    }

    public PerimeterResult countSpots() {
        PerimeterResult result = getEmptyResult();
        final SilentChunkReader reader = worldServer.silentChunkReader;
        final Set<ChunkPos> eligibleChunks = getEligibleChunks();
        // spawning attempts
        for (EnumCreatureType enumcreaturetype : EnumCreatureType.values()) {
            BlockPos.MutableBlockPos posIter = new BlockPos.MutableBlockPos();
            for (ChunkPos chunkPos : eligibleChunks) {
                searchChunkPositions(chunkPos, pos -> {
                    Messenger.print_server_message(CarpetServer.minecraft_server, pos.toImmutable().toString());
                });
            }
        }

        //int j4 = 0;
        //BlockPos blockpos1 = worldServerIn.getSpawnPoint();
        //
        //for (EnumCreatureType enumcreaturetype : EnumCreatureType.values())
        //{
        //    if ((!enumcreaturetype.getPeacefulCreature() || spawnPeacefulMobs) && (enumcreaturetype.getPeacefulCreature() || spawnHostileMobs) && (!enumcreaturetype.getAnimal() || spawnOnSetTickRate))
        //    {
        //        int k4 = worldServerIn.countEntities(enumcreaturetype.getCreatureClass());
        //        int l4 = enumcreaturetype.getMaxNumberOfCreature() * i / MOB_COUNT_DIV;
        //
        //        if (k4 <= l4)
        //        {
        //            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        //            /* carpet mod -> extra indentation */
        //            for (int trie = 0; trie < tries; trie++)
        //            {
        //                long local_spawns = 0;
        //                /* end */
        //
        //                label134:
        //
        //                for (ChunkPos chunkpos1 : this.eligibleChunksForSpawning)
        //                {
        //                    BlockPos blockpos = getRandomChunkPosition(worldServerIn, chunkpos1.x, chunkpos1.z);
        //                    int k1 = blockpos.getX();
        //                    int l1 = blockpos.getY();
        //                    int i2 = blockpos.getZ();
        //                    IBlockState iblockstate = worldServerIn.getBlockState(blockpos);
        //
        //                    if (!iblockstate.isNormalCube())
        //                    {
        //                        int j2 = 0;
        //
        //                        for (int k2 = 0; k2 < 3; ++k2)
        //                        {
        //                            int l2 = k1;
        //                            int i3 = l1;
        //                            int j3 = i2;
        //                            int k3 = 6;
        //                            Biome.SpawnListEntry biome$spawnlistentry = null;
        //                            IEntityLivingData ientitylivingdata = null;
        //                            int l3 = MathHelper.ceil(Math.random() * 4.0D);
        //                            //CM fixed 4 mobs per pack
        //                            if (CarpetSettings._1_8Spawning) { l3 = 4; }
        //
        //                            for (int i4 = 0; i4 < l3; ++i4)
        //                            {
        //                                l2 += worldServerIn.rand.nextInt(6) - worldServerIn.rand.nextInt(6);
        //                                i3 += worldServerIn.rand.nextInt(1) - worldServerIn.rand.nextInt(1);
        //                                j3 += worldServerIn.rand.nextInt(6) - worldServerIn.rand.nextInt(6);
        //                                blockpos$mutableblockpos.setPos(l2, i3, j3);
        //                                float f = (float)l2 + 0.5F;
        //                                float f1 = (float)j3 + 0.5F;
        //
        //                                if (!worldServerIn.isAnyPlayerWithinRangeAt((double)f, (double)i3, (double)f1, 24.0D) && blockpos1.distanceSq((double)f, (double)i3, (double)f1) >= 576.0D)
        //                                {
        //                                    if (biome$spawnlistentry == null)
        //                                    {
        //                                        biome$spawnlistentry = worldServerIn.getSpawnListEntryForTypeAt(enumcreaturetype, blockpos$mutableblockpos);
        //
        //                                        if (biome$spawnlistentry == null)
        //                                        {
        //                                            break;
        //                                        }
        //                                    }
        //
        //                                    if (worldServerIn.canCreatureTypeSpawnHere(enumcreaturetype, biome$spawnlistentry, blockpos$mutableblockpos) && canCreatureTypeSpawnAtLocation(EntitySpawnPlacementRegistry.getPlacementForEntity(biome$spawnlistentry.entityClass), worldServerIn, blockpos$mutableblockpos))
        //                                    {
        //                                        EntityLiving entityliving;
        //
        //                                        try
        //                                        {
        //                                            entityliving = biome$spawnlistentry.entityClass.getConstructor(World.class).newInstance(worldServerIn);
        //                                        }
        //                                        catch (Exception exception)
        //                                        {
        //                                            exception.printStackTrace();
        //                                            return j4;
        //                                        }
        //
        //                                        entityliving.setLocationAndAngles((double)f, (double)i3, (double)f1, worldServerIn.rand.nextFloat() * 360.0F, 0.0F);
        //
        //                                        if (entityliving.getCanSpawnHere() && entityliving.isNotColliding())
        //                                        {
        //                                            ientitylivingdata = entityliving.onInitialSpawn(worldServerIn.getDifficultyForLocation(new BlockPos(entityliving)), ientitylivingdata);
        //
        //                                            if (entityliving.isNotColliding())
        //                                            {
        //                                                ++j2;
        //                                                worldServerIn.spawnEntityInWorld(entityliving);
        //                                            }
        //                                            else
        //                                            {
        //                                                entityliving.setDead();
        //                                            }
        //
        //                                            if (j2 >= entityliving.getMaxSpawnedInChunk())
        //                                            {
        //                                                continue label134;
        //                                            }
        //                                        }
        //
        //                                        j4 += j2;
        //                                    }
        //                                }
        //                            }
        //                        }
        //                    }
        //                }
        //        }
        //    }
        //}
        //
        //return j4;

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
        return result;
    }

    @Override
    public void run() {
        PerimeterResult result = countSpots();
    }

}
