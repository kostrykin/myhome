package de.evoid.weather;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class WeatherForecast {
    public Entry now = null;
    public final Map<Date, Entry> items = new TreeMap<Date, Entry>();

    public enum Cloudiness {
        CLEAR, SCATTERED, BROKEN, COVERED
    }

    public enum WindSpeed {
        NONE, LIGHT, STRONG
    }

    public enum Precip {
        NONE, LIGHT, MEDIUM, HEAVY
    }

    public static class Entry {
        public Temperature temperatureMajor;
        public Temperature temperatureMinor;
        double cloudiness = 0; // percent
        double precipRain = 0; // mm per hour (liters per square meter)
        double precipSnow = 0; // mm per hour
        double windSpeed  = 0; // km per hour

        public Cloudiness accountCloudiness() {
            if (cloudiness <= 0.25) return Cloudiness.CLEAR;
            if (cloudiness <= 0.5 ) return Cloudiness.SCATTERED;
            if (cloudiness <= 0.75) return Cloudiness.BROKEN;
            if (cloudiness <= 1   ) return Cloudiness.COVERED;
            throw new RuntimeException("Illegal cloudiness");
        }

        public WindSpeed accountWindSpeed() {
            if (windSpeed < 30) return WindSpeed.NONE;
            if (windSpeed < 50) return WindSpeed.LIGHT;
            else return WindSpeed.STRONG;
        }

        public Precip accountRain() {
            if (precipRain < 0.1) return Precip.NONE;
            if (precipRain < 5  ) return Precip.LIGHT;
            if (precipRain < 30 ) return Precip.MEDIUM;
            else return Precip.HEAVY;
        }

        public Precip accountSnow() {
            if (precipSnow < 0.1) return Precip.NONE;
            if (precipSnow < 1  ) return Precip.LIGHT;
            if (precipSnow < 5  ) return Precip.MEDIUM;
            else return Precip.HEAVY;
        }
    }
}
