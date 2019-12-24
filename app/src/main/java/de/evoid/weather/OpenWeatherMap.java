package de.evoid.weather;

import android.content.Context;

import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.evoid.myhome.R;

import static com.android.volley.Request.Method.GET;

public class OpenWeatherMap extends BaseWeatherProvider {

    private final String apiKey;
    private final int forecastEntries;

    private final RequestQueue queue;

    private String location;
    private WeatherForecast forecast = null;

    public OpenWeatherMap(Context context) {
        queue = Volley.newRequestQueue(context);
        apiKey = context.getResources().getString(R.string.config_weather_api_key);
        forecastEntries = context.getResources().getInteger(R.integer.config_weather_forecast_entries);
    }

    @Override
    public void setLocation(String location) {
        this.location = location;
    }

    private void askApi(final String node) throws WeatherException {
        String url = null;
        try {
            url = "https://api.openweathermap.org/data/2.5/" + node + "?q=" + URLEncoder.encode(location, "UTF-8") + "&cnt=" + forecastEntries + "&APPID=" + apiKey;
        } catch (UnsupportedEncodingException ex) {
            throw new WeatherException(ex);
        }
        StringRequest request = new StringRequest(GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                handleResponse(node, response, null);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                handleResponse(node, null, error);
            }
        });
        queue.add(request);
    }

    @Override
    public void forecast() throws WeatherException {
        askApi("weather");
    }

    private WeatherForecast.Entry createForecastEntry(JSONObject data) throws WeatherException {
        WeatherForecast.Entry entry = new WeatherForecast.Entry();
        try {
            JSONObject main = data.getJSONObject("main");
            entry.temperatureMinor = getKelvin(main.getDouble("temp"));
            entry.temperatureMajor = getKelvin(main.getDouble("feels_like"));
            if (data.has("clouds")) {
                entry.cloudiness = data.getJSONObject("clouds").getInt("all") / 100.;
            }
            if (data.has("rain")) {
                JSONObject rainData = data.getJSONObject("rain");
                Iterator<String> rainDataKeyIterator = rainData.keys();
                while (rainDataKeyIterator.hasNext()) {
                    String rainDataKey = rainDataKeyIterator.next();
                    if (rainDataKey.charAt(rainDataKey.length() - 1) == 'h') {
                        int hours = Integer.parseInt(rainDataKey.substring(0, rainDataKey.length() - 1));
                        entry.precipRain = rainData.getDouble(rainDataKey) / hours;
                        break;
                    }
                }
            }
            if (data.has("snow")) {
                JSONObject snowData = data.getJSONObject("snow");
                Iterator<String> snowDataKeyIterator = snowData.keys();
                while (snowDataKeyIterator.hasNext()) {
                    String snowDataKey = snowDataKeyIterator.next();
                    if (snowDataKey.charAt(snowDataKey.length() - 1) == 'h') {
                        int hours = Integer.parseInt(snowDataKey.substring(0, snowDataKey.length() - 1));
                        entry.precipSnow = snowData.getDouble(snowDataKey) / hours;
                        break;
                    }
                }
            }
            if (data.has("wind")) {
                JSONObject windData = data.getJSONObject("wind");
                entry.windSpeed = windData.getDouble("speed") * 3.6;
            }
            return entry;
        } catch (JSONException ex) {
            throw new WeatherException(ex);
        }
    }

    private void handleResponse(String node, @Nullable String response, @Nullable VolleyError error) {
        final Logger logger = Logger.getLogger(OpenWeatherMap.class.getName());
        try {
            if (error != null) throw error;
            JSONObject data = new JSONObject(response);
            switch(node) {
                case "weather":
                    forecast = new WeatherForecast();
                    forecast.now = createForecastEntry(data);
                    askApi("forecast");
                    break;
                case "forecast":
                    JSONArray dataEntries = data.getJSONArray("list");
                    for (int i = 0; i < dataEntries.length(); ++i) {
                        JSONObject dataEntry = dataEntries.getJSONObject(i);
                        long utcTimestamp0 = System.currentTimeMillis() / 1000;
                        long utcTimestamp1 = dataEntry.getLong("dt");
                        int seconds = (int)(utcTimestamp1 - utcTimestamp0);
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.SECOND, seconds);
                        forecast.items.put(calendar.getTime(), createForecastEntry(dataEntry));
                    }
                    notifyListeners(forecast);
                    forecast = null;
                    break;
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            notifyListeners(null);
        }
    }

}
