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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.DividerItemDecoration;
import android.util.Log;
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

    /** Initialize handler. */
    public MainFragment() {
        handler = new Handler();
    }

    /** Initialize elements. */
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

    /** Display weather data. */
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

    /** Sets the appropriate wind speed units. */
    private void getUnits() {
        if (pref.getBoolean("metric", false)) {
            unitWind = "m/s";
        } else {
            unitWind = "mph";
        }
    }

    /** Sets up RecyclerView to display weather forecast. */
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
                            Toast.makeText(getActivity(), getActivity().getString(R.string.weather_data_error), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            updateCurrentWeather(current);
                        }
                    });
                }

                final JSONObject forecast = RemoteFetch.getJSON(getActivity(), zipCode, true, metric);
                if (forecast == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity(), getActivity().getString(R.string.weather_data_error), Toast.LENGTH_LONG).show();
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

    private void updateCurrentWeather(JSONObject json) {
        try {
            JSONObject details = json.getJSONArray("weather").getJSONObject(0);
            JSONObject main = json.getJSONObject("main");

            tvCityCurrent.setText(json.getString("name") + ", " + json.getJSONObject("sys").getString("country"));
            tvConditionsCurrent.setText(details.getString("description").toUpperCase());
            tvDetailsCurrent.setText("Humidity: " + main.getString("humidity") + "%" + "\n" + "Pressure: " + main.getString("pressure") + " hPa" + "\n" + "Wind Speed: " + json.getJSONObject("wind").getString("speed") + " " + unitWind);
            tvTemperatureCurrent.setText(Math.round(main.getDouble("temp")) + "°");

            setWeatherIcon(details.getInt("id"), json.getJSONObject("sys").getLong("sunrise") * 1000, json.getJSONObject("sys").getLong("sunset") * 1000);
        } catch (Exception e) {
            Log.e("MainFragment", "Error with json data");
        }
    }

    private void setWeatherIcon(int actualId, long sunrise, long sunset) {
        int id = actualId / 100;
        String icon = "";
        if (actualId == 800) {
            long currentTime = new Date().getTime();
            if (currentTime >= sunrise && currentTime < sunset) {
                icon = getActivity().getString(R.string.weather_sunny);
            } else {
                icon = getActivity().getString(R.string.weather_clear_night);
            }
        } else {
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
        tvIconCurrent.setTypeface(tfWeatherIcons);
        tvIconCurrent.setText(icon);
    }

}
