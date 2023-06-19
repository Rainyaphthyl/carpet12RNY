package carpet.utils.perimeter;

import carpet.utils.SilentChunkReader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.*;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.*;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SpawnChecker {
    public static final float MIN_WIDTH;
    public static final float MIN_HEIGHT;
    public static final float DEFAULT_WIDTH = 0.6F;
    public static final float DEFAULT_HEIGHT = 1.8F;
    public static final int SECTION_UNIT = 16;
    /**
     * Default value: {@code <0.6F, 1.8F>}
     */
    private static final Map<Class<? extends EntityLiving>, Tuple<Float, Float>> CREATURE_SIZE_MAP = new HashMap<>();

    static {
        // bosses
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityDragon.class, new Tuple<>(16.0F, 8.0F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityWither.class, new Tuple<>(0.9F, 3.5F));
        // monsters
        SpawnChecker.CREATURE_SIZE_MAP.put(EntitySkeleton.class, new Tuple<>(0.6F, 1.99F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityStray.class, new Tuple<>(0.6F, 1.99F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityCaveSpider.class, new Tuple<>(0.7F, 0.5F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityCreeper.class, new Tuple<>(0.6F, 1.7F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityEnderman.class, new Tuple<>(0.6F, 2.9F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityEndermite.class, new Tuple<>(0.4F, 0.3F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityEvoker.class, new Tuple<>(0.6F, 1.95F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityGhast.class, new Tuple<>(4.0F, 4.0F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityGuardian.class, new Tuple<>(0.85F, 0.85F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityIllusionIllager.class, new Tuple<>(0.6F, 1.95F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityIronGolem.class, new Tuple<>(1.4F, 2.7F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityPolarBear.class, new Tuple<>(1.3F, 1.4F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityShulker.class, new Tuple<>(1.0F, 1.0F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntitySilverfish.class, new Tuple<>(0.4F, 0.3F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntitySlime.class, new Tuple<>(0.51000005F, 0.51000005F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityMagmaCube.class, new Tuple<>(0.51000005F, 0.51000005F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntitySnowman.class, new Tuple<>(0.7F, 1.9F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntitySpider.class, new Tuple<>(1.4F, 0.9F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityVex.class, new Tuple<>(0.4F, 0.8F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityVindicator.class, new Tuple<>(0.6F, 1.95F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityWitch.class, new Tuple<>(0.6F, 1.95F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityWitherSkeleton.class, new Tuple<>(0.7F, 2.4F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityZombie.class, new Tuple<>(0.6F, 1.95F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityHusk.class, new Tuple<>(0.6F, 1.95F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityPigZombie.class, new Tuple<>(0.6F, 1.95F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityZombieVillager.class, new Tuple<>(0.6F, 1.95F));
        // animals
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityHorse.class, new Tuple<>(1.3964844F, 1.6F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntitySkeletonHorse.class, new Tuple<>(1.3964844F, 1.6F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityZombieHorse.class, new Tuple<>(1.3964844F, 1.6F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityDonkey.class, new Tuple<>(1.3964844F, 1.6F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityMule.class, new Tuple<>(1.3964844F, 1.6F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityBat.class, new Tuple<>(0.5F, 0.9F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityChicken.class, new Tuple<>(0.4F, 0.7F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityCow.class, new Tuple<>(0.9F, 1.4F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityLlama.class, new Tuple<>(0.9F, 1.87F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityMooshroom.class, new Tuple<>(0.9F, 1.4F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityOcelot.class, new Tuple<>(0.6F, 0.7F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityParrot.class, new Tuple<>(0.5F, 0.9F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityPig.class, new Tuple<>(0.9F, 0.9F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityRabbit.class, new Tuple<>(0.4F, 0.5F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntitySheep.class, new Tuple<>(0.9F, 1.3F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntitySquid.class, new Tuple<>(0.8F, 0.8F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityVillager.class, new Tuple<>(0.6F, 1.95F));
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityWolf.class, new Tuple<>(0.6F, 0.85F));
        // special mobs
        Tuple<Float, Float> tempPair = SpawnChecker.CREATURE_SIZE_MAP.get(EntityGuardian.class);
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityElderGuardian.class, new Tuple<>(tempPair.getFirst() * 2.35F, tempPair.getSecond() * 2.35F));
        tempPair = SpawnChecker.CREATURE_SIZE_MAP.get(EntityZombie.class);
        SpawnChecker.CREATURE_SIZE_MAP.put(EntityGiantZombie.class, new Tuple<>(tempPair.getFirst() * 6.0F, tempPair.getSecond() * 6.0F));
        // minimum size for default check
        float[] minSizes = new float[2];
        minSizes[0] = DEFAULT_WIDTH;
        minSizes[1] = DEFAULT_HEIGHT;
        CREATURE_SIZE_MAP.forEach((et, tuple) -> {
            float temp = tuple.getFirst();
            if (temp < minSizes[0]) {
                minSizes[0] = temp;
            }
            temp = tuple.getSecond();
            if (temp < minSizes[1]) {
                minSizes[1] = temp;
            }
        });
        MIN_WIDTH = minSizes[0];
        MIN_HEIGHT = minSizes[1];
    }

    private final WorldServer world;
    private final SilentChunkReader access;
    private final Vec3d posPeriCenter;

    public SpawnChecker(WorldServer world, @Nullable Vec3d posPeriCenter) throws NullPointerException {
        this.world = Objects.requireNonNull(world);
        access = Objects.requireNonNull(world.silentChunkReader);
        this.posPeriCenter = posPeriCenter;
    }

    @Nullable
    @ParametersAreNonnullByDefault
    public static AxisAlignedBB getEntityBoundingBox(Class<? extends EntityLiving> entityClass, BlockPos blockPos) {
        if (!EntityLiving.class.isAssignableFrom(entityClass)) {
            return null;
        }
        float width = DEFAULT_WIDTH;
        float height = DEFAULT_HEIGHT;
        Tuple<Float, Float> sizeTuple = CREATURE_SIZE_MAP.get(entityClass);
        if (sizeTuple != null) {
            width = sizeTuple.getFirst();
            height = sizeTuple.getSecond();
        }
        return createBoundingBox(blockPos, width, height);
    }

    @Nonnull
    public static AxisAlignedBB createBoundingBox(@Nonnull BlockPos blockPos, float width, float height) {
        double posX = (float) blockPos.getX() + 0.5F;
        double posY = blockPos.getY();
        double posZ = (float) blockPos.getZ() + 0.5F;
        double radius = width / 2.0F;
        return new AxisAlignedBB(posX - radius, posY, posZ - radius,
                posX + radius, posY + (double) height, posZ + radius);
    }

    @Nonnull
    public static AxisAlignedBB getMinimumBoundingBox(BlockPos blockPos) {
        return createBoundingBox(blockPos, MIN_WIDTH, MIN_HEIGHT);
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

    public static boolean canImmediatelyDespawn(Class<? extends EntityLiving> entityClass) {
        return !EntityAnimal.class.isAssignableFrom(entityClass)
                && !EntityGolem.class.isAssignableFrom(entityClass)
                && !EntityVillager.class.isAssignableFrom(entityClass);
    }

    @ParametersAreNonnullByDefault
    public static boolean isEntityPlaceable(EntityLiving.SpawnPlacementType placementType, IBlockState stateDown, IBlockState stateTarget, IBlockState stateUp) {
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

    public boolean isNotColliding(Class<? extends EntityLiving> entityClass, BlockPos posTarget) {
        AxisAlignedBB boundingBox = getEntityBoundingBox(entityClass, posTarget);
        return isNotColliding(boundingBox);
    }

    public boolean isNotColliding(AxisAlignedBB boundingBox) {
        if (boundingBox == null) {
            return false;
        }
        boolean liquidColliding = access.containsAnyLiquid(boundingBox);
        boolean blockColliding = access.optimizedGetCollisionBoxes(boundingBox, false, null);
        // entity collision check always returns "Not Colliding" in 1.12.2
        boolean entityColliding = false;
        return !liquidColliding && !blockColliding && !entityColliding;
    }

    public boolean isPositionAllowing(Class<? extends EntityLiving> entityClass, @Nonnull BlockPos posTarget) {
        if (entityClass == null) {
            return false;
        }
        int blockX = posTarget.getX();
        int blockZ = posTarget.getZ();
        float mobX = (float) blockX + 0.5F;
        float mobZ = (float) blockZ + 0.5F;
        int mobY = posTarget.getY();
        IBlockState stateDown = access.getBlockState(blockX, mobY - 1, blockZ);
        Block blockDown = stateDown.getBlock();
        if (EntityBat.class.isAssignableFrom(entityClass)) {
            if (mobY >= world.getSeaLevel()) {
                return false;
            } else {
                int lightLevel = access.getLightFromNeighbors(posTarget, true);
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
                            && access.getLight(posTarget) > 8
                            && isPositionAllowing(EntityAnimal.class, posTarget);
                } else {
                    return blockDown == SpawnChecker.getSpawnableBlock(entityClass)
                            && access.getLight(posTarget) > 8
                            && isPositionAllowing(EntityCreature.class, posTarget);
                }
            } else if (EntityMob.class.isAssignableFrom(entityClass)) {
                if (EntityEndermite.class.isAssignableFrom(entityClass)) {
                    return isPositionAllowing(EntityMob.class, posTarget)
                            && (posPeriCenter == null || posPeriCenter.squareDistanceTo(mobX, mobY, mobZ) >= 25.0);
                } else if (EntityGuardian.class.isAssignableFrom(entityClass)) {
                    return access.canBlockSeeSky(posTarget) && isPositionAllowing(EntityMob.class, posTarget);
                } else if (EntitySilverfish.class.isAssignableFrom(entityClass)) {
                    return isPositionAllowing(EntityMob.class, posTarget)
                            && (posPeriCenter == null || posPeriCenter.squareDistanceTo(mobX, mobY, mobZ) >= 25.0);
                } else if (EntityHusk.class.isAssignableFrom(entityClass)) {
                    return isPositionAllowing(EntityMob.class, posTarget) && access.canSeeSky(posTarget);
                } else if (EntityPigZombie.class.isAssignableFrom(entityClass)) {
                    return true;
                } else if (EntityStray.class.isAssignableFrom(entityClass)) {
                    return isPositionAllowing(EntityMob.class, posTarget) && access.canSeeSky(posTarget);
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
                Chunk chunk = access.getChunk(posTarget);
                Biome biome = access.getBiome(posTarget);
                if (biome == Biomes.SWAMPLAND && mobY > 50 && mobY < 70
                        && access.getLightFromNeighbors(posTarget, true) < 8) {
                    return isPositionAllowing(EntityLiving.class, posTarget);
                } else if (chunk != null && chunk.getRandomWithSeed(987234911L).nextInt(10) == 0 && mobY < 40) {
                    return isPositionAllowing(EntityLiving.class, posTarget);
                } else {
                    return false;
                }
            }
        } else if (EntityWaterMob.class.isAssignableFrom(entityClass)) {
            if (EntitySquid.class.isAssignableFrom(entityClass)) {
                return mobY > 45 && mobY < world.getSeaLevel() && isPositionAllowing(EntityWaterMob.class, posTarget);
            } else {
                return true;
            }
        } else {
            return blockDown != Blocks.MAGMA || SpawnChecker.isImmuneToFire(entityClass);
        }
    }

    public float getBlockPathWeight(Class<? extends EntityLiving> entityClass, BlockPos posTarget) {
        if (EntityAnimal.class.isAssignableFrom(entityClass)) {
            Block blockDown = access.getBlockState(posTarget.getX(), posTarget.getY() - 1, posTarget.getZ()).getBlock();
            return blockDown == SpawnChecker.getSpawnableBlock(entityClass) ? 10.0F : access.getLightBrightness(posTarget) - 0.5F;
        } else if (EntityGiantZombie.class.isAssignableFrom(entityClass)) {
            return access.getLightBrightness(posTarget) - 0.5F;
        } else if (EntityGuardian.class.isAssignableFrom(entityClass)) {
            return access.getBlockState(posTarget).getMaterial() == Material.WATER
                    ? 10.0F + access.getLightBrightness(posTarget) - 0.5F
                    : getBlockPathWeight(EntityMob.class, posTarget);
        } else if (EntityMob.class.isAssignableFrom(entityClass)) {
            return 0.5F - access.getLightBrightness(posTarget);
        } else if (EntitySilverfish.class.isAssignableFrom(entityClass)) {
            Block blockDown = access.getBlockState(posTarget.getX(), posTarget.getY() - 1, posTarget.getZ()).getBlock();
            return blockDown == Blocks.STONE ? 10.0F : getBlockPathWeight(EntityMob.class, posTarget);
        } else {
            return 0.0F;
        }
    }

    public boolean isLightValid(Class<? extends EntityLiving> mobClass, BlockPos pos) {
        if (!EntityMob.class.isAssignableFrom(mobClass)) {
            return false;
        }
        if (EntityBlaze.class.isAssignableFrom(mobClass) || EntityEndermite.class.isAssignableFrom(mobClass) || EntityGuardian.class.isAssignableFrom(mobClass) || EntitySilverfish.class.isAssignableFrom(mobClass)) {
            return true;
        }
        int lightLevel = access.getLightFromNeighbors(pos, true);
        return lightLevel < 8;
    }
}
