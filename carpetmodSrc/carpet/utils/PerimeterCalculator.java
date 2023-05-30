package carpet.utils;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * {@link net.minecraft.world.WorldEntitySpawner#findChunksForSpawning}
 */
public class PerimeterCalculator implements Runnable {
    private final Map<Class<? extends EntityLiving>, Biome.SpawnListEntry> entityEntryMap = new HashMap<>();
    private final Queue<Class<? extends EntityLiving>> constructBuffer = new LinkedBlockingQueue<>();
    private final WorldServer worldServer;
    private final Vec3d center;

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

    @Override
    public void run() {
        SilentChunkReader reader = worldServer.silentChunkReader;
        while (true) {
            Class<? extends EntityLiving> entityType = constructBuffer.peek();
            if (entityType == null) {
                break;
            } else {
                EnumCreatureType creatureType = EnumCreatureType.MONSTER;
                if (EntityAnimal.class.isAssignableFrom(entityType)) {
                    creatureType = EnumCreatureType.CREATURE;
                } else if (EntityWaterMob.class.isAssignableFrom(entityType)) {
                    creatureType = EnumCreatureType.WATER_CREATURE;
                } else if (EntityAmbientCreature.class.isAssignableFrom(entityType)) {
                    creatureType = EnumCreatureType.AMBIENT;
                }
                Collection<Biome.SpawnListEntry> list = reader.getPossibleCreatures(creatureType, new BlockPos(center));
            }
        }
    }
}
