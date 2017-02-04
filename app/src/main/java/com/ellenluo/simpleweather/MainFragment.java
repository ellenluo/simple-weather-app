package com.ellenluo.simpleweather;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
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

import java.text.DateFormat;
import java.util.Date;

public class MainFragment extends Fragment {

    public Typeface tfWeatherIcons;

    private TextView tvCityCurrent;
    private TextView tvTemperatureCurrent;
    private TextView tvIconCurrent;
    private TextView tvDetailsCurrent;
    private TextView tvLastUpdate;
    private TextView tvConditionsCurrent;

    RecyclerView rvForecast;

    Handler handler;

    public MainFragment() {
        handler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);

        tvCityCurrent = (TextView) v.findViewById(R.id.city_current);
        tvTemperatureCurrent = (TextView) v.findViewById(R.id.temperature_current);
        tvIconCurrent = (TextView) v.findViewById(R.id.icon_current);
        tvDetailsCurrent = (TextView) v.findViewById(R.id.details_current);
        tvLastUpdate = (TextView) v.findViewById(R.id.updated);
        tvConditionsCurrent = (TextView) v.findViewById(R.id.conditions_current);
        rvForecast = (RecyclerView) v.findViewById(R.id.forecast_list);

        setUpForecast();

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tfWeatherIcons = Typeface.createFromAsset(getActivity().getAssets(), "fonts/weathericons.ttf");

        SharedPreferences pref = getActivity().getPreferences(Activity.MODE_PRIVATE);
        updateWeatherData(pref.getString("city", "Berkeley, US"));
    }

    private void setUpForecast() {
        rvForecast.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        rvForecast.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvForecast.getContext(), DividerItemDecoration.VERTICAL);
        rvForecast.addItemDecoration(dividerItemDecoration);
    }

    private void updateWeatherData(final String city) {
        new Thread() {
            public void run() {
                final JSONObject current = RemoteFetch.getJSON(getActivity(), city, false);
                if (current == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity(), getActivity().getString(R.string.place_not_found), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            updateCurrentWeather(current);
                        }
                    });
                }

                final JSONObject forecast = RemoteFetch.getJSON(getActivity(), city, true);
                if (forecast == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity(), getActivity().getString(R.string.place_not_found), Toast.LENGTH_LONG).show();
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
            tvDetailsCurrent.setText("Humidity: " + main.getString("humidity") + "%" + "\n" + "Pressure: " + main.getString("pressure") + " hPa" + "\n" + "Wind Speed: " + json.getJSONObject("wind").getString("speed") + " m/s");
            tvTemperatureCurrent.setText(Math.round(main.getDouble("temp")) + "℃");

            DateFormat df = DateFormat.getDateTimeInstance();
            String updatedOn = df.format(new Date(json.getLong("dt") * 1000));
            tvLastUpdate.setText("Last update: " + updatedOn);

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
        Log.d("MainFragment", "icon is " + icon);
        tvIconCurrent.setTypeface(tfWeatherIcons);
        tvIconCurrent.setText(icon);
    }

}
