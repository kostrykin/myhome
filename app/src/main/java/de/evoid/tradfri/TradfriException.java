package de.evoid.tradfri;

public class TradfriException extends Exception {

    public TradfriException(String message, Exception cause) {
        super(message, cause);
    }

    public TradfriException(String message) {
        super(message);
    }

    public TradfriException(Exception cause) {
        super(cause);
    }
}
