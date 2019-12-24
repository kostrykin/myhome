package de.evoid.weather;

import static java.lang.Math.round;

public class Temperature {
    public enum Unit {FAHRENHEIT, CELSIUS}

    public double value;
    public Unit unit;

    public Temperature(double value, Unit unit) {
        this.value = value;
        this.unit = unit;
    }

    @Override
    public String toString() {
        String unitSuffix;
        switch(unit) {
            case CELSIUS:
                unitSuffix = "°C";
                break;
            case FAHRENHEIT:
                unitSuffix = "°F";
                break;
            default:
                throw new RuntimeException("Unknown unit");
        }
        return "" + round(value) + " " + unitSuffix;
    }
}
