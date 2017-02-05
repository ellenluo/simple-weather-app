package com.ellenluo.simpleweather;

/**
 * RecyclerView adapter used to populate forecast list.
 */

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ViewHolder> {

    private JSONObject json;
    private Typeface tfWeatherIcons;
    private SharedPreferences pref;

    private String unitWind;

    /**
     * Provide a reference to the views for each data item.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvDate;
        TextView tvTemperature;
        TextView tvIcon;
        TextView tvDetails;
        TextView tvMaxMin;
        TextView tvConditions;

        ViewHolder(View v) {
            super(v);

            // Initialize components
            tvDate = (TextView) v.findViewById(R.id.date_forecast);
            tvTemperature = (TextView) v.findViewById(R.id.temperature_forecast);
            tvIcon = (TextView) v.findViewById(R.id.icon_forecast);
            tvDetails = (TextView) v.findViewById(R.id.details_forecast);
            tvMaxMin = (TextView) v.findViewById(R.id.max_min_forecast);
            tvConditions = (TextView) v.findViewById(R.id.conditions_forecast);
        }
    }

    /**
     * Constructs adapter with JSON data.
     */
    ForecastAdapter(JSONObject json) {
        this.json = json;
    }

    /**
     * Creates new views (invoked by the layout manager).
     */
    @Override
    public ForecastAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.forecast_row, parent, false);

        // Set up fonts/preferences
        tfWeatherIcons = Typeface.createFromAsset(parent.getContext().getAssets(), "fonts/weathericons.ttf");
        pref = PreferenceManager.getDefaultSharedPreferences(parent.getContext());
        getUnits();

        return new ViewHolder(itemView);
    }

    /**
     * Replace the contents of a view.
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        updateForecast(holder, position);
    }

    /**
     * Return the size of data set.
     */
    @Override
    public int getItemCount() {
        return 40;
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
     * Updates forecast row.
     */
    private void updateForecast(ViewHolder holder, int position) {
        try {
            JSONObject current = json.getJSONArray("list").getJSONObject(position);
            JSONObject main = current.getJSONObject("main");
            JSONObject details = current.getJSONArray("weather").getJSONObject(0);

            // Set date
            SimpleDateFormat df = new SimpleDateFormat("EEE, MMMM d 'at' h:mm aa", Locale.US);
            String forecastDate = df.format(new Date(current.getLong("dt") * 1000));
            holder.tvDate.setText(forecastDate);

            // Set weather data
            holder.tvConditions.setText(details.getString("description").toUpperCase());
            holder.tvTemperature.setText(Math.round(main.getDouble("temp")) + "°");
            holder.tvMaxMin.setText("Max: " + Math.round(main.getDouble("temp_min")) + "° | Min: " + Math.round(main.getDouble("temp_max")) + "°");

            // Set weather details
            String humidity = holder.tvDate.getContext().getString(R.string.humidity) + " " + main.getString("humidity") + "%";
            String pressure = holder.tvDate.getContext().getString(R.string.pressure) + " " + Math.round(main.getDouble("pressure")) + " hPa";
            String windSpeed = holder.tvDate.getContext().getString(R.string.wind_speed) + " " + current.getJSONObject("wind").getString("speed") + " " + unitWind;
            holder.tvDetails.setText(humidity + "\n" + pressure + "\n" + windSpeed);

            // Set icon
            setWeatherIcon(holder, details.getInt("id"));
        } catch (Exception e) {
            // Display error message
            e.printStackTrace();
            Toast.makeText(holder.tvDate.getContext(), holder.tvDate.getContext().getString(R.string.error_unexpected), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sets appropriate weather icon.
     */
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

        // Set icon
        holder.tvIcon.setTypeface(tfWeatherIcons);
        holder.tvIcon.setText(icon);
    }

}
