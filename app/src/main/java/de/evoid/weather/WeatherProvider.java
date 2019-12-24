package de.evoid.weather;

public interface WeatherProvider {
    public void setCelsiusPreference(boolean preferCelsius);
    public void setLocation(String location);
    public void forecast() throws WeatherException;
    public void addWeatherListener(WeatherListener l);
    public void removeWeatherListener(WeatherListener l);
}
