package carpet.utils.perimeter;

import carpet.CarpetServer;
import carpet.utils.Messenger;
import carpet.utils.SilentChunkReader;
import carpet.utils.perimeter.PerimeterResult.EnumDistLevel;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLiving.SpawnPlacementType;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.*;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@link net.minecraft.world.WorldEntitySpawner#findChunksForSpawning}
 */
public class PerimeterCalculator implements Runnable {
    /**
     * Default value: {@code <0.6F, 1.8F>}
     */
    private static final Map<Class<? extends EntityLiving>, Tuple<Float, Float>> CREATURE_SIZE_MAP = new HashMap<>();
    private static final int SECTION_UNIT = 16;
    private static final int SAMPLE_REPORT_NUM = 10;
    private static final int yMin = 0;

    static {
        // bosses
        CREATURE_SIZE_MAP.put(EntityDragon.class, new Tuple<>(16.0F, 8.0F));
        CREATURE_SIZE_MAP.put(EntityWither.class, new Tuple<>(0.9F, 3.5F));
        // monsters
        CREATURE_SIZE_MAP.put(EntitySkeleton.class, new Tuple<>(0.6F, 1.99F));
        CREATURE_SIZE_MAP.put(EntityStray.class, new Tuple<>(0.6F, 1.99F));
        CREATURE_SIZE_MAP.put(EntityCaveSpider.class, new Tuple<>(0.7F, 0.5F));
        CREATURE_SIZE_MAP.put(EntityCreeper.class, new Tuple<>(0.6F, 1.7F));
        CREATURE_SIZE_MAP.put(EntityEnderman.class, new Tuple<>(0.6F, 2.9F));
        CREATURE_SIZE_MAP.put(EntityEndermite.class, new Tuple<>(0.4F, 0.3F));
        CREATURE_SIZE_MAP.put(EntityEvoker.class, new Tuple<>(0.6F, 1.95F));
        CREATURE_SIZE_MAP.put(EntityGhast.class, new Tuple<>(4.0F, 4.0F));
        CREATURE_SIZE_MAP.put(EntityGuardian.class, new Tuple<>(0.85F, 0.85F));
        CREATURE_SIZE_MAP.put(EntityIllusionIllager.class, new Tuple<>(0.6F, 1.95F));
        CREATURE_SIZE_MAP.put(EntityIronGolem.class, new Tuple<>(1.4F, 2.7F));
        CREATURE_SIZE_MAP.put(EntityPolarBear.class, new Tuple<>(1.3F, 1.4F));
        CREATURE_SIZE_MAP.put(EntityShulker.class, new Tuple<>(1.0F, 1.0F));
        CREATURE_SIZE_MAP.put(EntitySilverfish.class, new Tuple<>(0.4F, 0.3F));
        CREATURE_SIZE_MAP.put(EntitySlime.class, new Tuple<>(0.51000005F, 0.51000005F));
        CREATURE_SIZE_MAP.put(EntityMagmaCube.class, new Tuple<>(0.51000005F, 0.51000005F));
        CREATURE_SIZE_MAP.put(EntitySnowman.class, new Tuple<>(0.7F, 1.9F));
        CREATURE_SIZE_MAP.put(EntitySpider.class, new Tuple<>(1.4F, 0.9F));
        CREATURE_SIZE_MAP.put(EntityVex.class, new Tuple<>(0.4F, 0.8F));
        CREATURE_SIZE_MAP.put(EntityVindicator.class, new Tuple<>(0.6F, 1.95F));
        CREATURE_SIZE_MAP.put(EntityWitch.class, new Tuple<>(0.6F, 1.95F));
        CREATURE_SIZE_MAP.put(EntityWitherSkeleton.class, new Tuple<>(0.7F, 2.4F));
        CREATURE_SIZE_MAP.put(EntityZombie.class, new Tuple<>(0.6F, 1.95F));
        CREATURE_SIZE_MAP.put(EntityHusk.class, new Tuple<>(0.6F, 1.95F));
        CREATURE_SIZE_MAP.put(EntityPigZombie.class, new Tuple<>(0.6F, 1.95F));
        CREATURE_SIZE_MAP.put(EntityZombieVillager.class, new Tuple<>(0.6F, 1.95F));
        // animals
        CREATURE_SIZE_MAP.put(EntityHorse.class, new Tuple<>(1.3964844F, 1.6F));
        CREATURE_SIZE_MAP.put(EntitySkeletonHorse.class, new Tuple<>(1.3964844F, 1.6F));
        CREATURE_SIZE_MAP.put(EntityZombieHorse.class, new Tuple<>(1.3964844F, 1.6F));
        CREATURE_SIZE_MAP.put(EntityDonkey.class, new Tuple<>(1.3964844F, 1.6F));
        CREATURE_SIZE_MAP.put(EntityMule.class, new Tuple<>(1.3964844F, 1.6F));
        CREATURE_SIZE_MAP.put(EntityBat.class, new Tuple<>(0.5F, 0.9F));
        CREATURE_SIZE_MAP.put(EntityChicken.class, new Tuple<>(0.4F, 0.7F));
        CREATURE_SIZE_MAP.put(EntityCow.class, new Tuple<>(0.9F, 1.4F));
        CREATURE_SIZE_MAP.put(EntityLlama.class, new Tuple<>(0.9F, 1.87F));
        CREATURE_SIZE_MAP.put(EntityMooshroom.class, new Tuple<>(0.9F, 1.4F));
        CREATURE_SIZE_MAP.put(EntityOcelot.class, new Tuple<>(0.6F, 0.7F));
        CREATURE_SIZE_MAP.put(EntityParrot.class, new Tuple<>(0.5F, 0.9F));
        CREATURE_SIZE_MAP.put(EntityPig.class, new Tuple<>(0.9F, 0.9F));
        CREATURE_SIZE_MAP.put(EntityRabbit.class, new Tuple<>(0.4F, 0.5F));
        CREATURE_SIZE_MAP.put(EntitySheep.class, new Tuple<>(0.9F, 1.3F));
        CREATURE_SIZE_MAP.put(EntitySquid.class, new Tuple<>(0.8F, 0.8F));
        CREATURE_SIZE_MAP.put(EntityVillager.class, new Tuple<>(0.6F, 1.95F));
        CREATURE_SIZE_MAP.put(EntityWolf.class, new Tuple<>(0.6F, 0.85F));
        // special mobs
        Tuple<Float, Float> tempPair = CREATURE_SIZE_MAP.get(EntityGuardian.class);
        CREATURE_SIZE_MAP.put(EntityElderGuardian.class, new Tuple<>(tempPair.getFirst() * 2.35F, tempPair.getSecond() * 2.35F));
        tempPair = CREATURE_SIZE_MAP.get(EntityZombie.class);
        CREATURE_SIZE_MAP.put(EntityGiantZombie.class, new Tuple<>(tempPair.getFirst() * 6.0F, tempPair.getSecond() * 6.0F));
    }

    private final Class<? extends EntityLiving> entityType;
    private final WorldServer worldServer;
    private final Vec3d center;
    private final ICommandSender sender;
    private final ICommand command;
    private int yMax = SECTION_UNIT - 1;
    private Long2BooleanMap biomeAllowanceCache = null;
    private Int2ObjectSortedMap<Long2ObjectMap<EnumDistLevel>> distanceCacheLayered = null;
    private EnumCreatureType creatureType = null;
    private SpawnPlacementType placementType = null;
    private SilentChunkReader reader = null;
    private PerimeterResult result = null;
    private BlockPos worldSpawnPoint = null;
    private boolean specific = false;
    private int xMin = 0;
    private int xMax = 0;
    private int zMin = 0;
    private int zMax = 0;

    private PerimeterCalculator(ICommandSender sender, ICommand command, WorldServer worldServer, Vec3d center, Class<? extends EntityLiving> entityType) {
        this.sender = sender == null ? CarpetServer.minecraft_server : sender;
        this.command = command;
        this.worldServer = worldServer;
        this.center = center;
        this.entityType = entityType;
    }

    public static void asyncSearch(ICommandSender sender, ICommand command, World world, Vec3d center, Class<? extends EntityLiving> entityType) throws CommandException {
        if (world instanceof WorldServer) {
            CommandBase.notifyCommandListener(sender, command, "Start checking perimeter ...");
            PerimeterCalculator calculator = new PerimeterCalculator(sender, command, (WorldServer) world, center, entityType);
            HttpUtil.DOWNLOADER_EXECUTOR.submit(calculator);
        } else {
            throw new CommandException("commands.compare.outOfWorld");
        }
    }

    public static EnumCreatureType checkCreatureType(Class<? extends EntityLiving> entityType) {
        if (entityType == null) {
            return null;
        }
        EnumCreatureType creatureType = EnumCreatureType.MONSTER;
        if (EntityAnimal.class.isAssignableFrom(entityType)) {
            creatureType = EnumCreatureType.CREATURE;
        } else if (EntityWaterMob.class.isAssignableFrom(entityType)) {
            creatureType = EnumCreatureType.WATER_CREATURE;
        } else if (EntityAmbientCreature.class.isAssignableFrom(entityType)) {
            creatureType = EnumCreatureType.AMBIENT;
        }
        return creatureType;
    }

    public static boolean isImmuneToFire(Class<? extends EntityLiving> entityType) {
        return EntityDragon.class.isAssignableFrom(entityType)
                || EntityWither.class.isAssignableFrom(entityType)
                || EntityBlaze.class.isAssignableFrom(entityType)
                || EntityGhast.class.isAssignableFrom(entityType)
                || EntityMagmaCube.class.isAssignableFrom(entityType)
                || EntityPigZombie.class.isAssignableFrom(entityType)
                || EntityShulker.class.isAssignableFrom(entityType)
                || EntityVex.class.isAssignableFrom(entityType)
                || EntityWitherSkeleton.class.isAssignableFrom(entityType);
    }

    @Nullable
    public static Block getSpawnableBlock(Class<? extends EntityLiving> entityType) {
        if (!EntityAnimal.class.isAssignableFrom(entityType)) {
            return null;
        }
        return EntityMooshroom.class.isAssignableFrom(entityType) ? Blocks.MYCELIUM : Blocks.GRASS;
    }

    @Nullable
    @ParametersAreNonnullByDefault
    public static AxisAlignedBB getEntityBoundingBox(Class<? extends EntityLiving> entityClass, BlockPos blockPos) {
        if (!EntityLiving.class.isAssignableFrom(entityClass)) {
            return null;
        }
        double posX = (float) blockPos.getX() + 0.5F;
        double posY = blockPos.getY();
        double posZ = (float) blockPos.getZ() + 0.5F;
        float width = 0.6F;
        float height = 1.8F;
        Tuple<Float, Float> sizeTuple = CREATURE_SIZE_MAP.get(entityClass);
        if (sizeTuple != null) {
            width = sizeTuple.getFirst();
            height = sizeTuple.getSecond();
        }
        double radius = width / 2.0F;
        return new AxisAlignedBB(posX - radius, posY, posZ - radius,
                posX + radius, posY + (double) height, posZ + radius);
    }

    public static boolean canImmediatelyDespawn(Class<? extends EntityLiving> entityClass) {
        return !EntityAnimal.class.isAssignableFrom(entityClass)
                && !EntityGolem.class.isAssignableFrom(entityClass)
                && !EntityVillager.class.isAssignableFrom(entityClass);
    }

    @ParametersAreNonnullByDefault
    public static boolean isEntityPlaceable(SpawnPlacementType placementType, IBlockState stateDown, IBlockState stateTarget, IBlockState stateUp) {
        switch (placementType) {
            case IN_WATER:
                return stateTarget.getMaterial() == Material.WATER
                        && stateDown.getMaterial() == Material.WATER && !stateUp.isNormalCube();
            case ON_GROUND:
                if (!stateDown.isTopSolid()) {
                    return false;
                } else {
                    Block blockDown = stateDown.getBlock();
                    boolean flag = blockDown != Blocks.BEDROCK && blockDown != Blocks.BARRIER;
                    return flag && WorldEntitySpawner.isValidEmptySpawnBlock(stateTarget)
                            && WorldEntitySpawner.isValidEmptySpawnBlock(stateUp);
                }
            default:
                return false;
        }
    }

    @Override
    public void run() {
        try {
            initialize();
            countSpots();
            printResult();
        } catch (Exception e) {
            CommandBase.notifyCommandListener(sender, command, "Failed to check perimeter");
            e.printStackTrace();
        }
    }

    /**
     * Some non-final fields should not be null
     */
    private void initialize() {
        reader = worldServer.silentChunkReader;
        result = PerimeterResult.createEmptyResult();
        if (entityType != null) {
            specific = true;
            creatureType = checkCreatureType(entityType);
            placementType = EntitySpawnPlacementRegistry.getPlacementForEntity(entityType);
        } else {
            specific = false;
        }
        initSpawningRange();
        distanceCacheLayered = new Int2ObjectAVLTreeMap<>();
        biomeAllowanceCache = new Long2BooleanOpenHashMap();
    }

    /**
     * {@link net.minecraft.world.WorldEntitySpawner#findChunksForSpawning}
     */
    private void countSpots() {
        // Only count the possible spots. Do NOT calculate the rates.
        // The spawning rates can be 0 while the spot is counted as spawn-able.
        BlockPos.MutableBlockPos posTarget = new BlockPos.MutableBlockPos();
        for (int y = yMin; y <= yMax; ++y) {
            for (int x = xMin; x <= xMax; ++x) {
                for (int z = zMin; z <= zMax; ++z) {
                    posTarget.setPos(x, y, z);
                    IBlockState stateTarget = reader.getBlockState(posTarget);
                    IBlockState stateDown = reader.getBlockState(x, y - 1, z);
                    IBlockState stateUp = reader.getBlockState(x, y + 1, z);
                    // check placement in liquid
                    boolean flagLiquid = stateTarget.getMaterial() == Material.WATER
                            && stateDown.getMaterial() == Material.WATER && !stateUp.isNormalCube();
                    boolean flagGround;
                    if (!stateDown.isTopSolid()) {
                        flagGround = false;
                    } else {
                        Block blockDown = stateDown.getBlock();
                        boolean flag = blockDown != Blocks.BEDROCK && blockDown != Blocks.BARRIER;
                        flagGround = flag && WorldEntitySpawner.isValidEmptySpawnBlock(stateTarget)
                                && WorldEntitySpawner.isValidEmptySpawnBlock(stateUp);
                    }
                    if (flagLiquid) {
                        result.addGeneralSpot(SpawnPlacementType.IN_WATER, getDistLevelOf(posTarget));
                    }
                    if (flagGround) {
                        result.addGeneralSpot(SpawnPlacementType.ON_GROUND, getDistLevelOf(posTarget));
                    }
                    if (specific && isBiomeAllowing(posTarget)) {
                        boolean placeable = false;
                        switch (placementType) {
                            case ON_GROUND:
                                placeable = flagGround;
                                break;
                            case IN_WATER:
                                placeable = flagLiquid;
                                break;
                        }
                        if (placeable && isPositionAllowing(entityType, posTarget) && isNotColliding(entityType, posTarget)) {
                            result.addSpecificSpot(entityType, getDistLevelOf(posTarget), posTarget);
                        }
                    }
                }
            }
            distanceCacheLayered.remove(y);
        }
    }

    private void printResult() {
        CommandBase.notifyCommandListener(sender, command, "Finish checking perimeter info");
        Messenger.m(sender, "g format: <normal> + <despawning> = <spawning> ; <banned>...");
        Messenger.m(sender, "w Spawning spaces around ", Messenger.tpa("y", center.x, center.y, center.z, 2));
        Messenger.m(sender, result.createStandardReport(SpawnPlacementType.IN_WATER));
        Messenger.m(sender, result.createStandardReport(SpawnPlacementType.ON_GROUND));
        if (specific) {
            Messenger.m(sender, result.createStandardReport(entityType));
            ITextComponent[] bannedSampleList = result.createBannedSampleReports(entityType, SAMPLE_REPORT_NUM);
            if (bannedSampleList.length > 0) {
                if (worldSpawnPoint == null) {
                    worldSpawnPoint = reader.getSpawnPoint();
                }
                Messenger.m(sender, "w Current ", "w WSP", "^g World Spawn Point", "w  in use: ",
                        Messenger.tpa("m", (double) worldSpawnPoint.getX(), worldSpawnPoint.getY(), worldSpawnPoint.getZ()));
                Messenger.m(sender, "w Forbidden locations around the World Spawn Point:");
                for (ITextComponent sample : bannedSampleList) {
                    Messenger.m(sender, sample);
                }
            }
            ITextComponent[] normalSampleList = result.createSpawningSampleReports(entityType, SAMPLE_REPORT_NUM);
            for (ITextComponent sample : normalSampleList) {
                Messenger.m(sender, sample);
            }
        }
        Messenger.m(sender, "g ------------------------");
    }

    private boolean isLightValid(Class<? extends EntityLiving> mobClass, BlockPos pos) {
        if (!EntityMob.class.isAssignableFrom(mobClass)) {
            return false;
        }
        if (EntityBlaze.class.isAssignableFrom(mobClass) || EntityEndermite.class.isAssignableFrom(mobClass) || EntityGuardian.class.isAssignableFrom(mobClass) || EntitySilverfish.class.isAssignableFrom(mobClass)) {
            return true;
        }
        int lightLevel = reader.getLightFromNeighbors(pos, true);
        return lightLevel < 8;
    }

    private void initSpawningRange() {
        int chunkX = MathHelper.floor(center.x / 16.0);
        int chunkZ = MathHelper.floor(center.z / 16.0);
        // checking player chunk map
        int radius = Math.min(CarpetServer.minecraft_server.getPlayerList().getViewDistance(), 7);
        for (int cx = -radius; cx <= radius; ++cx) {
            for (int cz = -radius; cz <= radius; ++cz) {
                ChunkPos chunkPos = new ChunkPos(chunkX + cx, chunkZ + cz);
                int height = checkChunkHeight(chunkPos);
                if (height > yMax) {
                    yMax = height;
                }
            }
        }
        int expand = 20;
        WorldBorder worldBorder = worldServer.getWorldBorder();
        int worldLimit = worldBorder.getSize();
        xMin = MathHelper.clamp(((chunkX - radius) << 4) - expand, -worldLimit, worldLimit);
        xMax = MathHelper.clamp(((chunkX + radius) << 4) + 15 + expand, -worldLimit, worldLimit);
        zMin = MathHelper.clamp(((chunkZ - radius) << 4) - expand, -worldLimit, worldLimit);
        zMax = MathHelper.clamp(((chunkZ + radius) << 4) + 15 + expand, -worldLimit, worldLimit);
    }

    private int checkChunkHeight(@Nonnull ChunkPos chunkPos) {
        final int originX = chunkPos.x * 16;
        final int originZ = chunkPos.z * 16;
        Chunk chunk = reader.getChunk(chunkPos);
        if (chunk == null) {
            return SECTION_UNIT - 1;
        } else {
            int height = MathHelper.roundUp(chunk.getHeight(new BlockPos(originX + 8, 0, originZ + 8)) + 1, SECTION_UNIT);
            if (height <= 0) {
                height = chunk.getTopFilledSegment() + (SECTION_UNIT - 1);
            }
            return height;
        }
    }

    /**
     * The 24-meter check for the world spawn point and the closest player
     */
    private EnumDistLevel getDistLevelOf(@Nonnull BlockPos posTarget) {
        int posY = posTarget.getY();
        Long2ObjectMap<EnumDistLevel> cacheLayer;
        if (distanceCacheLayered.containsKey(posY)) {
            cacheLayer = distanceCacheLayered.get(posY);
        } else {
            cacheLayer = new Long2ObjectOpenHashMap<>();
            cacheLayer.defaultReturnValue(null);
            distanceCacheLayered.put(posY, cacheLayer);
        }
        long index = SilentChunkReader.blockHorizonLong(posTarget);
        if (cacheLayer.containsKey(index)) {
            return cacheLayer.get(index);
        }
        float mobX = (float) posTarget.getX() + 0.5F;
        float mobZ = (float) posTarget.getZ() + 0.5F;
        EnumDistLevel level;
        if (worldSpawnPoint == null) {
            worldSpawnPoint = reader.getSpawnPoint();
        }
        if (worldSpawnPoint.distanceSq(mobX, posY, mobZ) >= 576.0) {
            double distSq = center.squareDistanceTo(mobX, posY, mobZ);
            level = EnumDistLevel.getLevelOfDistSq(distSq);
        } else {
            level = EnumDistLevel.BANNED;
        }
        cacheLayer.put(index, level);
        return level;
    }

    private boolean isBiomeAllowing(@Nonnull BlockPos posTarget) {
        long index = SilentChunkReader.blockHorizonLong(posTarget);
        if (biomeAllowanceCache.containsKey(index)) {
            return biomeAllowanceCache.get(index);
        }
        boolean allowing = false;
        for (Biome.SpawnListEntry entryOption : reader.getPossibleCreatures(creatureType, posTarget)) {
            if (Objects.equals(entryOption.entityClass, entityType)) {
                allowing = true;
                break;
            }
        }
        biomeAllowanceCache.put(index, allowing);
        return allowing;
    }

    private boolean isPositionAllowing(Class<? extends EntityLiving> entityClass, @Nonnull BlockPos posTarget) {
        if (entityClass == null) {
            return false;
        }
        int blockX = posTarget.getX();
        int blockZ = posTarget.getZ();
        float mobX = (float) blockX + 0.5F;
        float mobZ = (float) blockZ + 0.5F;
        int mobY = posTarget.getY();
        IBlockState stateDown = reader.getBlockState(blockX, mobY - 1, blockZ);
        Block blockDown = stateDown.getBlock();
        if (EntityBat.class.isAssignableFrom(entityClass)) {
            if (mobY >= worldServer.getSeaLevel()) {
                return false;
            } else {
                int lightLevel = reader.getLightFromNeighbors(posTarget, true);
                int limit = 7;
                return lightLevel <= limit && isPositionAllowing(EntityLiving.class, posTarget);
            }
        } else if (EntityCreature.class.isAssignableFrom(entityClass)) {
            if (EntityAnimal.class.isAssignableFrom(entityClass)) {
                if (EntityOcelot.class.isAssignableFrom(entityClass)) {
                    return true;
                } else if (EntityParrot.class.isAssignableFrom(entityClass)) {
                    return blockDown instanceof BlockLeaves || blockDown == Blocks.GRASS
                            || blockDown instanceof BlockLog || blockDown == Blocks.AIR
                            && reader.getLight(posTarget) > 8
                            && isPositionAllowing(EntityAnimal.class, posTarget);
                } else {
                    return blockDown == getSpawnableBlock(entityClass)
                            && reader.getLight(posTarget) > 8
                            && isPositionAllowing(EntityCreature.class, posTarget);
                }
            } else if (EntityMob.class.isAssignableFrom(entityClass)) {
                if (EntityEndermite.class.isAssignableFrom(entityClass)) {
                    return isPositionAllowing(EntityMob.class, posTarget)
                            && center.squareDistanceTo(mobX, mobY, mobZ) >= 25.0;
                } else if (EntityGuardian.class.isAssignableFrom(entityClass)) {
                    return reader.canBlockSeeSky(posTarget) && isPositionAllowing(EntityMob.class, posTarget);
                } else if (EntitySilverfish.class.isAssignableFrom(entityClass)) {
                    return isPositionAllowing(EntityMob.class, posTarget)
                            && center.squareDistanceTo(mobX, mobY, mobZ) >= 25.0;
                } else if (EntityHusk.class.isAssignableFrom(entityClass)) {
                    return isPositionAllowing(EntityMob.class, posTarget) && reader.canSeeSky(posTarget);
                } else if (EntityPigZombie.class.isAssignableFrom(entityClass)) {
                    return true;
                } else if (EntityStray.class.isAssignableFrom(entityClass)) {
                    return isPositionAllowing(EntityMob.class, posTarget) && reader.canSeeSky(posTarget);
                } else {
                    return isLightValid(entityClass, posTarget) && isPositionAllowing(EntityCreature.class, posTarget);
                }
            } else {
                return isPositionAllowing(EntityLiving.class, posTarget)
                        && getBlockPathWeight(entityClass, posTarget) >= 0.0F;
            }
        } else if (EntityGhast.class.isAssignableFrom(entityClass)) {
            return isPositionAllowing(EntityLiving.class, posTarget);
        } else if (EntitySlime.class.isAssignableFrom(entityClass)) {
            if (EntityMagmaCube.class.isAssignableFrom(entityClass)) {
                return true;
            } else {
                Chunk chunk = reader.getChunk(posTarget);
                Biome biome = reader.getBiome(posTarget);
                if (biome == Biomes.SWAMPLAND && mobY > 50 && mobY < 70
                        && reader.getLightFromNeighbors(posTarget, true) < 8) {
                    return isPositionAllowing(EntityLiving.class, posTarget);
                } else if (chunk != null && chunk.getRandomWithSeed(987234911L).nextInt(10) == 0 && mobY < 40) {
                    return isPositionAllowing(EntityLiving.class, posTarget);
                } else {
                    return false;
                }
            }
        } else if (EntityWaterMob.class.isAssignableFrom(entityClass)) {
            if (EntitySquid.class.isAssignableFrom(entityClass)) {
                return mobY > 45 && mobY < worldServer.getSeaLevel() && isPositionAllowing(EntityWaterMob.class, posTarget);
            } else {
                return true;
            }
        } else {
            return blockDown != Blocks.MAGMA || isImmuneToFire(entityClass);
        }
    }

    private float getBlockPathWeight(Class<? extends EntityLiving> entityClass, BlockPos posTarget) {
        if (EntityAnimal.class.isAssignableFrom(entityClass)) {
            Block blockDown = reader.getBlockState(posTarget.getX(), posTarget.getY() - 1, posTarget.getZ()).getBlock();
            return blockDown == getSpawnableBlock(entityClass) ? 10.0F : reader.getLightBrightness(posTarget) - 0.5F;
        } else if (EntityGiantZombie.class.isAssignableFrom(entityClass)) {
            return reader.getLightBrightness(posTarget) - 0.5F;
        } else if (EntityGuardian.class.isAssignableFrom(entityClass)) {
            return reader.getBlockState(posTarget).getMaterial() == Material.WATER
                    ? 10.0F + reader.getLightBrightness(posTarget) - 0.5F
                    : getBlockPathWeight(EntityMob.class, posTarget);
        } else if (EntityMob.class.isAssignableFrom(entityClass)) {
            return 0.5F - reader.getLightBrightness(posTarget);
        } else if (EntitySilverfish.class.isAssignableFrom(entityClass)) {
            Block blockDown = reader.getBlockState(posTarget.getX(), posTarget.getY() - 1, posTarget.getZ()).getBlock();
            return blockDown == Blocks.STONE ? 10.0F : getBlockPathWeight(EntityMob.class, posTarget);
        } else {
            return 0.0F;
        }
    }

    private boolean isNotColliding(Class<? extends EntityLiving> entityClass, BlockPos.MutableBlockPos posTarget) {
        AxisAlignedBB boundingBox = getEntityBoundingBox(entityClass, posTarget);
        if (boundingBox == null) {
            return false;
        }
        boolean liquidColliding = reader.containsAnyLiquid(boundingBox);
        boolean blockColliding = reader.optimizedGetCollisionBoxes(boundingBox, false, null);
        // entity collision check always returns "Not Colliding" in 1.12.2
        boolean entityColliding = false;
        return !liquidColliding && !blockColliding && !entityColliding;
    }

}
