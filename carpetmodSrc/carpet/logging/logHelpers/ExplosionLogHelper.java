package carpet.logging.logHelpers;

import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.List;

public class ExplosionLogHelper {

    // CARPET-SYLKOS
    // Some code yeeted from lntricarpet and gnembon 1.16+ fabric carpet

    // carpet12RNY
    // Modified by Naftalluvia

    private static long lastGametime = 0;
    private static long explosionCountInCurrentGT = 0;
    private static boolean newTick = false;
    public final Vec3d pos;
    public final Entity entity;
    private final float power;
    private final boolean createFire;
    // TODO: 2023/5/3,0003 To log the blocks with dropping rates
    private boolean affectBlocks = false;

    public ExplosionLogHelper(Entity entity, double x, double y, double z, float power, boolean createFire) { // blocks removed
        this.entity = entity;
        this.pos = new Vec3d(x, y, z);
        this.power = power;
        this.createFire = createFire;
    }

    public void setAffectBlocks(boolean affectBlocks) {
        this.affectBlocks = affectBlocks;
    }

    public void onExplosionDone(long gametime) {
        newTick = false;
        if (lastGametime != gametime) {
            explosionCountInCurrentGT = 0;
            lastGametime = gametime;
            newTick = true;
        }
        ++explosionCountInCurrentGT;
        LoggerRegistry.getLogger("explosions").log((option) -> {
            List<ITextComponent> messages = new ArrayList<>();
            if (newTick) {
                messages.add(Messenger.m(null, "wb tick : ", "d " + gametime));
            }
            switch (option) {
                case "brief":
                case "full":
                    messages.add(Messenger.m(null,
                            "d #" + explosionCountInCurrentGT,
                            "gb ->",
                            Messenger.dblt("l", pos.x, pos.y, pos.z),
                            (affectBlocks ? "m (affects blocks)" : "m  (doesn't affect blocks)")
                    ));
                    break;
            }
            return messages.toArray(new ITextComponent[0]);
        });
    }
}
