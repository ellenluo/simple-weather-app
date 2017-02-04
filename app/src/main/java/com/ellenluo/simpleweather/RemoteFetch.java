package com.ellenluo.simpleweather;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RemoteFetch {

    private static final String OPEN_WEATHER_CURRENT = "http://api.openweathermap.org/data/2.5/weather?q=%s&units=metric";
    private static final String OPEN_WEATHER_FORECAST = "http://api.openweathermap.org/data/2.5/forecast?q=%s&units=metric";


    public static JSONObject getJSON(Context context, String city, boolean forecast){
        try {
            URL url;

            if (forecast) {
                url = new URL(String.format(OPEN_WEATHER_FORECAST, city));
            } else {
                url = new URL(String.format(OPEN_WEATHER_CURRENT, city));
            }

            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.addRequestProperty("x-api-key", context.getString(R.string.open_weather_maps_app_id));
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            StringBuilder json = new StringBuilder(1024);
            String tmp="";

            while ((tmp=reader.readLine())!= null) {
                json.append(tmp).append("\n");
            }

            reader.close();

            JSONObject data = new JSONObject(json.toString());

            if(data.getInt("cod") != 200){
                Log.d("RemoteFetch", "unsuccessful");
                return null;
            }

            return data;
        } catch(Exception e) {
            Log.d("RemoteFetch", "unsuccessful");
            return null;
        }
    }

}
