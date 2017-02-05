package com.ellenluo.simpleweather;

/**
 * Fetches JSON data from Open Weather Map.
 */

import android.content.Context;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class RemoteFetch {

    private static final String OPEN_WEATHER_CURRENT = "http://api.openweathermap.org/data/2.5/weather?%s&units=%s";
    private static final String OPEN_WEATHER_FORECAST = "http://api.openweathermap.org/data/2.5/forecast?q=%s&units=%s";
    private static final String IMPERIAL = "imperial";
    private static final String METRIC = "metric";

    /**
     * Retrieves weather data from zip code.
     */
    static JSONObject getJSON(Context context, int zipCode, boolean forecast, boolean metric) {
        try {
            URL url;

            if (forecast) {
                url = new URL(String.format(OPEN_WEATHER_FORECAST, "zip=" + zipCode + ",us", getUnits(metric)));
            } else {
                url = new URL(String.format(OPEN_WEATHER_CURRENT, "zip=" + zipCode + ",us", getUnits(metric)));
            }

            return getData(context, url);
        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.error_location), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    /**
     * Retrieves weather data from latitude/longitude.
     */
    static JSONObject getJSON(Context context, float lat, float lon, boolean forecast, boolean metric) {
        try {
            URL url;

            if (forecast) {
                url = new URL(String.format(OPEN_WEATHER_FORECAST, "lat=" + lat + "&lon=" + lon, getUnits(metric)));
            } else {
                url = new URL(String.format(OPEN_WEATHER_CURRENT, "lat=" + lat + "&lon=" + lon, getUnits(metric)));
            }

            return getData(context, url);
        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.error_location), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    /**
     * Returns unit type.
     */
    private static String getUnits(boolean metric) {
        if (metric) {
            return METRIC;
        }
        return IMPERIAL;
    }

    /**
     * Gets data from Open Weather Map.
     */
    private static JSONObject getData(Context context, URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("x-api-key", context.getString(R.string.open_weather_maps_app_id));
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            StringBuilder json = new StringBuilder(1024);
            String temp = "";

            while ((temp = reader.readLine()) != null) {
                json.append(temp).append("\n");
            }

            reader.close();

            JSONObject data = new JSONObject(json.toString());

            // Display error message
            if (data.getInt("cod") != 200) {
                Toast.makeText(context, context.getString(R.string.error_location), Toast.LENGTH_LONG).show();
                return null;
            }

            return data;
        } catch (Exception e) {
            // Display error message
            Toast.makeText(context, context.getString(R.string.error_location), Toast.LENGTH_LONG).show();
            return null;
        }
    }

}
