package world.anhgelus.khunegos.timer;

public abstract class CustomTickingException extends Exception {
    public CustomTickingException(String message, Exception cause) {
        super(message, cause);
    }
}
