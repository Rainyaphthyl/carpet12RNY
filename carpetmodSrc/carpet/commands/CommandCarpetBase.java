package carpet.commands;

import carpet.CarpetSettings;
import carpet.utils.Messenger;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

public abstract class CommandCarpetBase extends CommandBase
{
    @Nonnull
    @ParametersAreNonnullByDefault
    public static List<String> getTabCompletionCoordinateExact(ICommandSender sender, String[] inputArgs, int index, @Nullable BlockPos target, int decimals) {
        Vec3d posBaseE = sender.getPositionVector();
        List<String> list = new ArrayList<>();
        int i = inputArgs.length - 1;
        double posCurrE;
        int posCurrB;
        int posTarget;
        switch (i - index) {
            case 0:
                posCurrE = posBaseE.x;
                posCurrB = MathHelper.floor(posCurrE);
                if (target != null) {
                    posTarget = target.getX();
                } else {
                    posTarget = posCurrB;
                }
                break;
            case 1:
                posCurrE = posBaseE.y;
                posCurrB = MathHelper.floor(posCurrE);
                if (target != null) {
                    posTarget = target.getY();
                } else {
                    posTarget = posCurrB;
                }
                break;
            case 2:
                posCurrE = posBaseE.z;
                posCurrB = MathHelper.floor(posCurrE);
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
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    public void msg(ICommandSender sender, List<ITextComponent> texts) { msg(sender, texts.toArray(new ITextComponent[0])); }
    public void msg(ICommandSender sender, ITextComponent ... texts)
    {
        if (sender instanceof EntityPlayer)
        {
            for (ITextComponent t: texts) sender.sendMessage(t);
        }
        else
        {
            for (ITextComponent t: texts) notifyCommandListener(sender, this, t.getUnformattedText());
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

    public boolean command_enabled(String command_name, ICommandSender sender)
    {
        if (!CarpetSettings.get(command_name).equalsIgnoreCase("true"))
        {
            msg(sender, Messenger.m(null, "w Command is disabled in carpet settings"));
            if (!(sender instanceof EntityPlayer)) return false;
            if (CarpetSettings.locked)
            {
                Messenger.m((EntityPlayer)sender, "gi Ask your admin to enable it server config");
            }
            else
            {
                Messenger.m((EntityPlayer)sender,
                        "gi copy&pasta \"",
                        "gib /carpet "+command_name+" true", "/carpet "+command_name+" true",
                        "gi \"to enable it");
            }
            return false;
        }
        return true;
    }

    protected int parseChunkPosition(String arg, int base) throws NumberInvalidException {
        return arg.equals("~") ? base >> 4 : parseInt(arg);
    }

}
