package de.evoid.weather;

import java.util.Map;
import java.util.WeakHashMap;

public abstract class BaseWeatherProvider implements WeatherProvider {
    private Map<WeatherListener, Boolean> listeners = new WeakHashMap<WeatherListener, Boolean>();
    private boolean preferCelsius = false;

    @Override
    public void addWeatherListener(WeatherListener l) {
        listeners.put(l, true);
    }

    @Override
    public void removeWeatherListener(WeatherListener l) {
        listeners.remove(l);
    }

    protected void notifyListeners(WeatherForecast forecast) {
        for (WeatherListener l : listeners.keySet()) {
            l.onWeatherForecast(forecast);
        }
    }

    @Override
    public void setCelsiusPreference(boolean preferCelsius) {
        this.preferCelsius = preferCelsius;
    }

    public Temperature getFahrenheit(double fahrenheit) {
        if (preferCelsius) {
            return new Temperature((fahrenheit - 32) * 5. / 9, Temperature.Unit.CELSIUS);
        } else {
            return new Temperature(fahrenheit, Temperature.Unit.FAHRENHEIT);
        }
    }

    public Temperature getCelsius(double celsius) {
        if (preferCelsius) {
            return new Temperature(celsius, Temperature.Unit.CELSIUS);
        } else {
            return new Temperature(celsius * 9. / 5 + 32, Temperature.Unit.FAHRENHEIT);
        }
    }

    public Temperature getKelvin(double kelvin) {
        return getCelsius(kelvin - 273.15);
    }
}
