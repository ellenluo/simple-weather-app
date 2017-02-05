package com.ellenluo.simpleweather;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RemoteFetch {

    private static final String OPEN_WEATHER_CURRENT = "http://api.openweathermap.org/data/2.5/weather?%s&units=%s";
    private static final String OPEN_WEATHER_FORECAST = "http://api.openweathermap.org/data/2.5/forecast?q=%s&units=%s";
    private static final String IMPERIAL = "imperial";
    private static final String METRIC = "metric";


    public static JSONObject getJSON(Context context, int zipCode, boolean forecast, boolean metric) {
        try {
            URL url;

            if (forecast) {
                url = new URL(String.format(OPEN_WEATHER_FORECAST, "zip=" + zipCode + ",us", getUnits(metric)));
            } else {
                url = new URL(String.format(OPEN_WEATHER_CURRENT, "zip=" + zipCode + ",us", getUnits(metric)));
            }

            return getData(context, url);
        } catch (Exception e) {
            Log.d("RemoteFetch", "unsuccessful");
            return null;
        }
    }

    public static JSONObject getJSON(Context context, float lat, float lon, boolean forecast, boolean metric) {
        try {
            URL url;

            if (forecast) {
                url = new URL(String.format(OPEN_WEATHER_FORECAST, "lat=" + lat + "&lon=" + lon, getUnits(metric)));
            } else {
                url = new URL(String.format(OPEN_WEATHER_CURRENT, "lat=" + lat + "&lon=" + lon, getUnits(metric)));
            }

            return getData(context, url);
        } catch (Exception e) {
            Log.d("RemoteFetch", "unsuccessful");
            return null;
        }
    }

    private static String getUnits(boolean metric) {
        if (metric) {
            return METRIC;
        }
        return IMPERIAL;
    }

    private static JSONObject getData(Context context, URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("x-api-key", context.getString(R.string.open_weather_maps_app_id));
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            StringBuilder json = new StringBuilder(1024);
            String tmp = "";

            while ((tmp = reader.readLine()) != null) {
                json.append(tmp).append("\n");
            }

            reader.close();

            JSONObject data = new JSONObject(json.toString());

            if (data.getInt("cod") != 200) {
                Log.d("RemoteFetch", "unsuccessful");
                return null;
            }

            return data;
        } catch (Exception e) {
            Log.d("RemoteFetch", "unsuccessful");
            return null;
        }
    }

}
