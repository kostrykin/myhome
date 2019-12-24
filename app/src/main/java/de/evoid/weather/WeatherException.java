package de.evoid.weather;

public class WeatherException extends Exception {

    public WeatherException(String message, Exception cause) {
        super(message, cause);
    }

    public WeatherException(String message) {
        super(message);
    }

    public WeatherException(Exception cause) {
        super(cause);
    }
}
