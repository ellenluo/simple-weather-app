package com.ellenluo.simpleweather;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class MainFragment extends Fragment {

    Typeface tfWeatherIcons;

    TextView tvCityCurrent;
    TextView tvTemperatureCurrent;
    TextView tvIconCurrent;
    TextView tvDetailsCurrent;
    TextView tvLastUpdate;
    TextView tvConditionsCurrent;

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

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tfWeatherIcons = Typeface.createFromAsset(getActivity().getAssets(), "fonts/weathericons.ttf");

        SharedPreferences pref = getActivity().getPreferences(Activity.MODE_PRIVATE);
        updateWeatherData(pref.getString("city", "Berkeley, US"));
    }

    private void updateWeatherData(final String city) {
        new Thread() {
            public void run() {
                final JSONObject json = RemoteFetch.getJSON(getActivity(), city);
                if (json == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(getActivity(), getActivity().getString(R.string.place_not_found), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            updateCurrentWeather(json);
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
            tvDetailsCurrent.setText("Humidity: " + main.getString("humidity") + "%" + "\n" + "Pressure: " + main.getString("pressure") + " hPa");
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
