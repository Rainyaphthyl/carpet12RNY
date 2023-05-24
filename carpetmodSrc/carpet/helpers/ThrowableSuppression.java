package carpet.helpers;

public class ThrowableSuppression extends RuntimeException{
    public ThrowableSuppression(String message) {
        super(message);
    }

    /**
     * {@code message = "Update Suppression"} by default
     */
    public ThrowableSuppression(){
        this("Update Suppression");
    }
}
