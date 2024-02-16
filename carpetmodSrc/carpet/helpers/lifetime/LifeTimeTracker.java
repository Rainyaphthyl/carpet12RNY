package carpet.helpers.lifetime;

import carpet.CarpetSettings;
import carpet.helpers.lifetime.utils.LifeTimeTrackerUtil;
import carpet.helpers.lifetime.utils.SpecificDetailMode;
import carpet.helpers.lifetime.utils.TextUtil;
import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class LifeTimeTracker extends AbstractTracker
{
    private static final LifeTimeTracker INSTANCE = new LifeTimeTracker();
    private static boolean attachedServer = false;
    private final Map<WorldServer, LifeTimeWorldTracker> trackers = new Reference2ObjectArrayMap<>();
    private boolean trackingBySize = false;
    private boolean containingVariance = false;
    private int currentTrackId = 0;

    public LifeTimeTracker()
    {
        super("LifeTime");
    }

    public static LifeTimeTracker getInstance()
    {
        return INSTANCE;
    }

    public static void attachServer(MinecraftServer minecraftServer)
    {
        attachedServer = true;
        INSTANCE.trackers.clear();
        for (WorldServer world : minecraftServer.worlds)
        {
            INSTANCE.trackers.put(world, world.getLifeTimeWorldTracker());
        }
    }

    public static boolean isActivated()
    {
        return attachedServer && INSTANCE.isTracking();
    }

    //    public static void detachServer()
    //    {
    //        attachedServer = false;
    //        INSTANCE.stop();
    //    }

    public LifeTimeWorldTracker getTracker(World world)
    {
        return world instanceof WorldServer ? this.trackers.get(world) : null;
    }

    public boolean willTrackEntity(Entity entity)
    {
        return isActivated() &&
                entity.getTrackId() == this.getCurrentTrackId() &&
                LifeTimeTrackerUtil.isTrackedEntity(entity);
    }

    public Stream<String> getAvailableEntityType()
    {
        if (!isActivated())
        {
            return Stream.empty();
        }
        return this.trackers.values().stream().
                flatMap(
                        tracker -> tracker.getDataMap().keySet().
                                stream().map(LifeTimeTrackerUtil::getEntityTypeDescriptor)
                ).
                distinct();
    }

    public int getCurrentTrackId()
    {
        return this.currentTrackId;
    }

    @Override
    protected void initTracker()
    {
        this.currentTrackId++;
        this.trackers.values().forEach(LifeTimeWorldTracker::initTracker);
    }

    @Override
    protected void printTrackingResult(ICommandSender source, boolean realtime)
    {
        try
        {
            long ticks = this.sendTrackedTime(source, realtime);
            int count = this.trackers.values().stream().
                    mapToInt(tracker -> tracker.print(source, ticks, null, null)).
                    sum();
            if (count == 0)
            {
                Messenger.m(source, Messenger.s(null, "No result yet"));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void sendUnknownEntity(ICommandSender source, String entityTypeString)
    {
        Messenger.s(source, String.format("Unknown entity type \"%s\"", entityTypeString), "r");
    }

    private void printTrackingResultSpecificInner(ICommandSender source, String entityTypeString, String detailModeString, boolean realtime)
    {
        Optional<Class<? extends Entity>> entityTypeOptional = LifeTimeTrackerUtil.getEntityTypeFromName(entityTypeString);
        if (entityTypeOptional.isPresent())
        {
            SpecificDetailMode detailMode = null;
            if (detailModeString != null)
            {
                try
                {
                    detailMode = SpecificDetailMode.fromString(detailModeString);
                }
                catch (IllegalArgumentException e)
                {
                    Messenger.s(source, String.format("Invalid statistic detail \"%s\"", detailModeString), "r");
                    return;
                }
            }

            long ticks = this.sendTrackedTime(source, realtime);
            Class<? extends Entity> entityType = entityTypeOptional.get();
            Messenger.s(source, "Life time result for " + LifeTimeTrackerUtil.getEntityTypeDescriptor(entityType));
            SpecificDetailMode finalDetailMode = detailMode;
            int count = this.trackers.values().stream().
                    mapToInt(tracker -> tracker.print(source, ticks, entityType, finalDetailMode)).
                    sum();
            if (count == 0)
            {
                Messenger.s(source, "No result yet");
            }
        }
        else
        {
            this.sendUnknownEntity(source, entityTypeString);
        }
    }

    public int printTrackingResultSpecific(ICommandSender source, String entityTypeString, String detailModeString, boolean realtime)
    {
        return this.doWhenTracking(source, () -> this.printTrackingResultSpecificInner(source, entityTypeString, detailModeString, realtime));
    }

    public int showHelp(ICommandSender source)
    {
        String docLink = "https://github.com/TISUnion/TISCarpet113/blob/TIS-Server/docs/Features.md#lifetime";
        source.sendMessage(Messenger.c(
                String.format("wb %s\n", this.getTranslatedNameFull()),
                "w A tracker to track lifetime and spawn / removal reasons from all newly spawned and removed entities\n",
                "w Complete doc ",
                TextUtil.getFancyText(
                        null,
                        Messenger.s(null, "here", "ut"),
                        Messenger.s(null, docLink, "t"),
                        new ClickEvent(ClickEvent.Action.OPEN_URL, docLink)
                )
        ));
        return 1;
    }

    @Override
    public int startTracking(ICommandSender source, boolean showFeedback)
    {
        int result = super.startTracking(source, showFeedback);
        trackingBySize = CarpetSettings.lifetimeTrackBySize;
        containingVariance = CarpetSettings.lifetimeTrackerStandardError;
        return result;
    }

    public boolean isTrackingBySize()
    {
        return trackingBySize;
    }

    public boolean isContainingVariance()
    {
        return containingVariance;
    }
}
