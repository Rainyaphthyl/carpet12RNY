package carpet.helpers;

import carpet.utils.Messenger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;

public class ThrowableSuppression extends RuntimeException {
    private BlockPos blockPos = null;

    private ThrowableSuppression(String message) {
        super(message);
    }

    /**
     * {@code message = "Update Suppression"} by default
     */
    private ThrowableSuppression() {
        this("Update Suppression");
    }

    private ThrowableSuppression(Throwable cause) {
        this();
        initCause(cause);
    }

    public ThrowableSuppression(Throwable cause, BlockPos blockPos) {
        this(cause);
        this.blockPos = blockPos == null ? null : blockPos.toImmutable();
    }

    public static void printServerWarning(Throwable throwable, MinecraftServer server, String phase) {
        if (throwable instanceof ThrowableSuppression) {
            try {
                ((ThrowableSuppression) throwable).printServerWarning(server, phase);
            } catch (Throwable e) {
                Messenger.print_server_message(server, "A server crash in " + phase + " is failed to be reported.");
            }
        } else {
            Messenger.print_server_message(server, "You just caused a server crash in " + phase + ".");
        }
    }

    private void printServerWarning(MinecraftServer server, String phase) {
        Throwable cause = getCause();
        ITextComponent position = blockPos == null ? Messenger.c("d unknown position")
                : Messenger.tpa("d", blockPos.getX(), blockPos.getY(), blockPos.getZ());
        ITextComponent reason;
        if (cause == null) {
            reason = Messenger.c("r unknown reasons");
        } else {
            String fullName = cause.getClass().getTypeName();
            int iPostDot = fullName.lastIndexOf('.') + 1;
            String packName = iPostDot <= 0 ? "" : fullName.substring(0, iPostDot);
            String clzName = fullName.substring(iPostDot);
            String message = cause.getMessage();
            if (message == null) {
                message = getMessage();
            }
            reason = Messenger.c("n  - ", "r " + packName, "c " + clzName);
            if (message != null) {
                reason.appendSibling(Messenger.c("n \n - ", "r " + message));
            }
        }
        ITextComponent report = Messenger.c("r Server crashed in ", "m " + phase,
                "r  at ", position, "n \n", reason);
        Messenger.print_server_message(server, report);
    }
}
