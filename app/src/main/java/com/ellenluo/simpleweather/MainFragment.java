package com.ellenluo.simpleweather;

/**
 * Fragment used to display current weather conditions and forecast.
 */

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Date;

public class MainFragment extends Fragment {

    private Typeface tfWeatherIcons;

    private TextView tvCityCurrent;
    private TextView tvTemperatureCurrent;
    private TextView tvIconCurrent;
    private TextView tvDetailsCurrent;
    private TextView tvConditionsCurrent;

    private RecyclerView rvForecast;

    private Handler handler;

    private SharedPreferences pref;

    private String unitWind;

    /**
     * Initializes handler.
     */
    public MainFragment() {
        handler = new Handler();
    }

    /**
     * Initializes elements.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);

        tvCityCurrent = (TextView) v.findViewById(R.id.city_current);
        tvTemperatureCurrent = (TextView) v.findViewById(R.id.temperature_current);
        tvIconCurrent = (TextView) v.findViewById(R.id.icon_current);
        tvDetailsCurrent = (TextView) v.findViewById(R.id.details_current);
        tvConditionsCurrent = (TextView) v.findViewById(R.id.conditions_current);
        rvForecast = (RecyclerView) v.findViewById(R.id.forecast_list);

        setUpForecast();

        return v;
    }

    /**
     * Displays weather data.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tfWeatherIcons = Typeface.createFromAsset(getActivity().getAssets(), "fonts/weathericons.ttf");

        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        getUnits();

        // Update weather data with either GPS coordinates or zip code
        if (pref.getBoolean("using_lat", false)) {
            updateWeatherData(0, pref.getFloat("lat", 0), pref.getFloat("lon", 0), true, pref.getBoolean("metric", false));
        } else {
            updateWeatherData(pref.getInt("zip", 94720), 0, 0, false, pref.getBoolean("metric", false));
        }
    }

    /**
     * Sets the appropriate wind speed units.
     */
    private void getUnits() {
        if (pref.getBoolean("metric", false)) {
            unitWind = "m/s";
        } else {
            unitWind = "mph";
        }
    }

    /**
     * Sets up RecyclerView to display weather forecast.
     */
    private void setUpForecast() {
        rvForecast.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        rvForecast.setLayoutManager(layoutManager);

        // Dividers between elements
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvForecast.getContext(), DividerItemDecoration.VERTICAL);
        rvForecast.addItemDecoration(dividerItemDecoration);
    }

    /* Updates weather data asynchronously by fetching JSON from Open Weather Map. */
    public void updateWeatherData(final int zipCode, final float lat, final float lon, final boolean usingLat, final boolean metric) {
        new Thread() {
            public void run() {
                // Get current conditions
                final JSONObject current;

                if (usingLat) {
                    current = RemoteFetch.getJSON(getActivity(), lat, lon, false, metric);
                } else {
                    current = RemoteFetch.getJSON(getActivity(), zipCode, false, metric);
                }

                if (current == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity(), getActivity().getString(R.string.error_location), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            updateCurrentWeather(current);
                        }
                    });
                }

                // Get forecast
                final JSONObject forecast;

                if (usingLat) {
                    forecast = RemoteFetch.getJSON(getActivity(), lat, lon, true, metric);
                } else {
                    forecast = RemoteFetch.getJSON(getActivity(), zipCode, true, metric);
                }

                if (forecast == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity(), getActivity().getString(R.string.error_location), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            RecyclerView.Adapter listAdapter = new ForecastAdapter(forecast);
                            rvForecast.setAdapter(listAdapter);
                        }
                    });
                }
            }
        }.start();
    }

    /**
     * Updates current weather fields.
     */
    private void updateCurrentWeather(JSONObject json) {
        try {
            JSONObject details = json.getJSONArray("weather").getJSONObject(0);
            JSONObject main = json.getJSONObject("main");

            tvCityCurrent.setText(json.getString("name") + ", " + json.getJSONObject("sys").getString("country"));
            tvConditionsCurrent.setText(details.getString("description").toUpperCase());
            tvTemperatureCurrent.setText(Math.round(main.getDouble("temp")) + "°");

            // Set details
            String humidity = getString(R.string.humidity) + " " + main.getString("humidity") + "%";
            String pressure = getString(R.string.pressure) + " " + main.getString("pressure") + " hPa";
            String windSpeed = getString(R.string.wind_speed) + " " + json.getJSONObject("wind").getString("speed") + " " + unitWind;
            tvDetailsCurrent.setText(humidity + "\n" + pressure + "\n" + windSpeed);

            // Set icon
            setWeatherIcon(details.getInt("id"), json.getJSONObject("sys").getLong("sunrise") * 1000, json.getJSONObject("sys").getLong("sunset") * 1000);
        } catch (Exception e) {
            // Display error message
            Toast.makeText(getActivity(), getString(R.string.error_unexpected), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sets appropriate weather icon.
     */
    private void setWeatherIcon(int actualId, long sunrise, long sunset) {
        int id = actualId / 100;
        String icon = "";
        if (actualId == 800) {
            // Night/day icons
            long currentTime = new Date().getTime();
            if (currentTime >= sunrise && currentTime < sunset) {
                icon = getActivity().getString(R.string.weather_sunny);
            } else {
                icon = getActivity().getString(R.string.weather_clear_night);
            }
        } else {
            // Weather condition icons
            switch (id) {
                case 2:
                    icon = getActivity().getString(R.string.weather_thunder);
                    break;
                case 3:
                    icon = getActivity().getString(R.string.weather_drizzle);
                    break;
                case 7:
                    icon = getActivity().getString(R.string.weather_foggy);
                    break;
                case 8:
                    icon = getActivity().getString(R.string.weather_cloudy);
                    break;
                case 6:
                    icon = getActivity().getString(R.string.weather_snowy);
                    break;
                case 5:
                    icon = getActivity().getString(R.string.weather_rainy);
                    break;
            }
        }

        // Set icon
        tvIconCurrent.setTypeface(tfWeatherIcons);
        tvIconCurrent.setText(icon);
    }

}
