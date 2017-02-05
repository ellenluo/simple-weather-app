package com.ellenluo.simpleweather;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {

    private JSONObject json;
    private Typeface tfWeatherIcons;
    private SharedPreferences pref;

    private String unitWind;

    // Provide a reference to the views for each data item
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvDate;
        TextView tvTemperature;
        TextView tvIcon;
        TextView tvDetails;
        TextView tvMaxMin;
        TextView tvConditions;

        public ViewHolder(View v) {
            super(v);
            tvDate = (TextView) v.findViewById(R.id.date_forecast);
            tvTemperature = (TextView) v.findViewById(R.id.temperature_forecast);
            tvIcon = (TextView) v.findViewById(R.id.icon_forecast);
            tvDetails = (TextView) v.findViewById(R.id.details_forecast);
            tvMaxMin = (TextView) v.findViewById(R.id.max_min_forecast);
            tvConditions = (TextView) v.findViewById(R.id.conditions_forecast);
        }
    }

    public ForecastAdapter(JSONObject json) {
        this.json = json;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ForecastAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.forecast_row, parent, false);
        tfWeatherIcons = Typeface.createFromAsset(parent.getContext().getAssets(), "fonts/weathericons.ttf");
        pref = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
        getUnits();
        return new ViewHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        updateForecast(holder, position);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return 40;
    }

    private void updateForecast(ViewHolder holder, int position) {
        try {
            JSONObject current = json.getJSONArray("list").getJSONObject(position);
            JSONObject main = current.getJSONObject("main");
            JSONObject details = current.getJSONArray("weather").getJSONObject(0);

            SimpleDateFormat df = new SimpleDateFormat("EEE, MMMM d 'at' h:mm aa", Locale.US);
            String forecastDate = df.format(new Date(current.getLong("dt") * 1000));
            holder.tvDate.setText(forecastDate);

            holder.tvConditions.setText(details.getString("description").toUpperCase());
            holder.tvDetails.setText("Humidity: " + main.getString("humidity") + "%" + "\n" + "Pressure: " + Math.round(main.getDouble("pressure")) + " hPa" + "\n" + "Wind Speed: " + current.getJSONObject("wind").getString("speed") + " " + unitWind);
            holder.tvTemperature.setText(Math.round(main.getDouble("temp")) + "°");
            holder.tvMaxMin.setText("Max: " + Math.round(main.getDouble("temp_min")) + "° | Min: " + Math.round(main.getDouble("temp_max")) + "°");

            setWeatherIcon(holder, details.getInt("id"));
        } catch (Exception e) {
            Log.e("MainFragment", "Error with json data");
        }
    }

    private void setWeatherIcon(ViewHolder holder, int actualId) {
        int id = actualId / 100;
        String icon = "";

        switch (id) {
            case 2:
                icon = holder.tvDate.getContext().getString(R.string.weather_thunder);
                break;
            case 3:
                icon = holder.tvDate.getContext().getString(R.string.weather_drizzle);
                break;
            case 7:
                icon = holder.tvDate.getContext().getString(R.string.weather_foggy);
                break;
            case 8:
                icon = holder.tvDate.getContext().getString(R.string.weather_cloudy);
                break;
            case 6:
                icon = holder.tvDate.getContext().getString(R.string.weather_snowy);
                break;
            case 5:
                icon = holder.tvDate.getContext().getString(R.string.weather_rainy);
                break;
        }

        holder.tvIcon.setTypeface(tfWeatherIcons);
        holder.tvIcon.setText(icon);
    }

    private void getUnits() {
        if (pref.getBoolean("metric", false)) {
            unitWind = "m/s";
        } else {
            unitWind = "mph";
        }
    }

}
