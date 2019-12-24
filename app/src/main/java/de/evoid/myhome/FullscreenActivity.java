package de.evoid.myhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.evoid.tradfri.LightBulb;
import de.evoid.tradfri.Room;
import de.evoid.tradfri.Scene;
import de.evoid.tradfri.TradfriException;
import de.evoid.tradfri.TradfriGateway;
import de.evoid.weather.OpenWeatherMap;
import de.evoid.weather.WeatherException;
import de.evoid.weather.WeatherForecast;
import de.evoid.weather.WeatherListener;
import de.evoid.weather.WeatherProvider;

import static java.text.DateFormat.getTimeInstance;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity implements WeatherListener {

    private View mContentView;
    private TextView timeView;
    private LinearLayout roomsView;
    private TradfriGateway tradfri;
    private BroadcastReceiver clock;
    private WeatherProvider weather;
    private View currentWeatherView;
    private TextView currentWeatherTempMajor;
    private TextView currentWeatherTempMinor;
    private LinearLayout weatherForecastView;
    private TextView lastWeatherUpdateView;
    private long lastWeatherUpdateTimestamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mContentView = findViewById(R.id.fullscreen);
        roomsView = mContentView.findViewById(R.id.rooms);
        timeView = mContentView.findViewById(R.id.time);

        currentWeatherView = mContentView.findViewById(R.id.weather_current);
        currentWeatherTempMajor = mContentView.findViewById(R.id.current_weather_temp_major);
        currentWeatherTempMinor = mContentView.findViewById(R.id.current_weather_temp_minor);
        currentWeatherView.setVisibility(View.INVISIBLE);
        weatherForecastView = mContentView.findViewById(R.id.weather_forecasts);
        lastWeatherUpdateView = mContentView.findViewById(R.id.last_weather_update);

        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        weather = new OpenWeatherMap(this);
        weather.setLocation(getResources().getString(R.string.config_weather_location));
        weather.setCelsiusPreference(true);
        weather.addWeatherListener(this);

        populateRooms();
    }

    private void requestWeatherForecast() {
        try {
            weather.forecast();
        } catch (WeatherException ex) {
            reportError(ex);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        final int weatherUpdateInterval = getResources().getInteger(R.integer.config_weather_update_interval);
        clock = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                    updateTimeView();
                    long currentTimestamp = System.currentTimeMillis() / 1000;
                    if (currentTimestamp - lastWeatherUpdateTimestamp > 60 * weatherUpdateInterval) {
                        requestWeatherForecast();
                        lastWeatherUpdateTimestamp = currentTimestamp;
                    }
                }
            }
        };
        registerReceiver(clock, new IntentFilter(Intent.ACTION_TIME_TICK));

        requestWeatherForecast();
        updateTimeView();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (clock != null) {
            unregisterReceiver(clock);
            clock = null;
        }
    }

    private void updateTimeView() {
        DateFormat timeFormat = getTimeInstance(DateFormat.SHORT);
        timeView.setText(timeFormat.format(new Date()));
    }

    private void reportError(Exception ex) {
        final Logger logger = Logger.getLogger(FullscreenActivity.class.getName());
        logger.log(Level.SEVERE, ex.getMessage(), ex);
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(ex.getMessage())
                .show();
    }

    private abstract static class TradfriTask extends AsyncTask<Void, Void, Void> {
        private Exception ex = null;

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                doTradfriTask();
            } catch (TradfriException ex) {
                this.ex = ex;
            }
            return null;
        }

        protected abstract void doTradfriTask() throws TradfriException;

        protected void onPostExecute(Void result) {
            if (this.ex != null) throw new RuntimeException(ex);
        }
    }

    private static class ActivateSceneTask extends TradfriTask {
        private final Scene scene;

        public ActivateSceneTask(Scene scene) {
            this.scene = scene;
        }
        @Override
        protected void doTradfriTask() throws TradfriException {
            scene.activate();
        }
    }

    private static class AllOffTask extends TradfriTask {
        private final TradfriGateway tradfri;

        public AllOffTask(TradfriGateway tradfri) {
            this.tradfri = tradfri;
        }
        @Override
        protected void doTradfriTask() throws TradfriException {
            List<LightBulb> bulbs = tradfri.dicoverBulbs();
            for (LightBulb bulb : bulbs) {
                bulb.setOn(false);
            }
        }
    }

    private void populateRooms() {
        roomsView.removeAllViews();
        final FullscreenActivity context = this;
        ContextThemeWrapper sceneButtonStyle = new ContextThemeWrapper(this, R.style.SceneButton);
        try {
            String gatewayIp = getResources().getString(R.string.config_tradfri_gateway_address);
            String securityKey = getResources().getString(R.string.config_tradfri_security_key);
            tradfri = new TradfriGateway(gatewayIp, securityKey);

            List<Room> rooms = tradfri.discoverRooms();
            for (Room room : rooms) {
                View roomView = LayoutInflater.from(this).inflate(R.layout.layout_room, null);
                TextView roomName = (TextView) roomView.findViewById(R.id.room_name);
                LinearLayout scenesView = (LinearLayout) roomView.findViewById(R.id.room_scenes);
                scenesView.removeAllViews();

                roomName.setText(room.name);

                List<Scene> scenes = room.discoverScenes();
                for (final Scene scene : scenes) {
                    if (scene.predefined) continue;
                    Button sceneButton = new Button(sceneButtonStyle);
                    sceneButton.setText(scene.name);
                    scenesView.addView(sceneButton);

                    sceneButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new ActivateSceneTask(scene).execute();
                        }
                    });
                }

                roomsView.addView(roomView);
            }
        } catch (TradfriException ex) {
            reportError(ex);
        }
    }

    public void turnAllOff(@Nullable View v) {
        if (tradfri != null) {
            new AllOffTask(tradfri).execute();
        }
    }

    private void setupWeatherIcon(View icon, WeatherForecast.Entry entry) {
        ImageView currentWeatherClouds = icon.findViewById(R.id.weather_clouds);
        ImageView currentWeatherRain = icon.findViewById(R.id.weather_rain);
        ImageView currentWeatherSnow = icon.findViewById(R.id.weather_snow);
        ImageView currentWeatherWind = icon.findViewById(R.id.weather_wind);

        switch (entry.accountCloudiness()) {
            case CLEAR:
                currentWeatherClouds.setImageDrawable(getDrawable(R.drawable.ic_weather_clouds_clear));
                break;
            case SCATTERED:
                currentWeatherClouds.setImageDrawable(getDrawable(R.drawable.ic_weather_clouds_scattered));
                break;
            case BROKEN:
                currentWeatherClouds.setImageDrawable(getDrawable(R.drawable.ic_weather_clouds_broken));
                break;
            case COVERED:
                currentWeatherClouds.setImageDrawable(getDrawable(R.drawable.ic_weather_clouds_covered));
                break;
        }

        switch (entry.accountWindSpeed()) {
            case NONE:
                currentWeatherWind.setVisibility(View.INVISIBLE);
                break;
            case LIGHT:
                currentWeatherWind.setImageDrawable(getDrawable(R.drawable.ic_weather_wind_light));
                currentWeatherWind.setVisibility(View.VISIBLE);
                break;
            case STRONG:
                currentWeatherWind.setImageDrawable(getDrawable(R.drawable.ic_weather_wind_strong));
                currentWeatherWind.setVisibility(View.VISIBLE);
                break;
        }

        switch (entry.accountRain()) {
            case NONE:
                currentWeatherRain.setVisibility(View.INVISIBLE);
                break;
            case LIGHT:
                currentWeatherRain.setImageDrawable(getDrawable(R.drawable.ic_weather_rain_light));
                currentWeatherRain.setVisibility(View.VISIBLE);
                break;
            case MEDIUM:
                currentWeatherRain.setImageDrawable(getDrawable(R.drawable.ic_weather_rain_medium));
                currentWeatherRain.setVisibility(View.VISIBLE);
                break;
            case HEAVY:
                currentWeatherRain.setImageDrawable(getDrawable(R.drawable.ic_weather_rain_heavy));
                currentWeatherRain.setVisibility(View.VISIBLE);
                break;
        }

        switch (entry.accountSnow()) {
            case NONE:
                currentWeatherSnow.setVisibility(View.INVISIBLE);
                break;
            case LIGHT:
                currentWeatherSnow.setImageDrawable(getDrawable(R.drawable.ic_weather_snow_light));
                currentWeatherSnow.setVisibility(View.VISIBLE);
                break;
            case MEDIUM:
                currentWeatherSnow.setImageDrawable(getDrawable(R.drawable.ic_weather_snow_medium));
                currentWeatherSnow.setVisibility(View.VISIBLE);
                break;
            case HEAVY:
                currentWeatherSnow.setImageDrawable(getDrawable(R.drawable.ic_weather_snow_heavy));
                currentWeatherSnow.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onWeatherForecast(WeatherForecast forecast) {
        currentWeatherTempMajor.setText(forecast.now.temperatureMajor.toString());
        currentWeatherTempMinor.setText(forecast.now.temperatureMinor.toString());
        setupWeatherIcon(currentWeatherView, forecast.now);
        currentWeatherView.setVisibility(View.VISIBLE);

        weatherForecastView.removeAllViews();
        DateFormat timeFormat = getTimeInstance(DateFormat.SHORT);
        for (Map.Entry<Date, WeatherForecast.Entry> item : forecast.items.entrySet()) {
            View entryView = LayoutInflater.from(this).inflate(R.layout.layout_weather_forecast, null);
            TextView weatherTempMajor = entryView.findViewById(R.id.forecast_weather_temp_major);
            TextView weatherTempMinor = entryView.findViewById(R.id.forecast_weather_temp_minor);
            TextView timeView = entryView.findViewById(R.id.time);

            WeatherForecast.Entry entry = item.getValue();
            weatherTempMajor.setText(entry.temperatureMajor.toString());
            weatherTempMinor.setText(entry.temperatureMinor.toString());
            setupWeatherIcon(entryView, entry);
            timeView.setText(timeFormat.format(item.getKey()));
            weatherForecastView.addView(entryView);
        }

        lastWeatherUpdateView.setText(getResources().getString(R.string.last_weather_update).replace("TIME", timeFormat.format(new Date())));
    }
}
