package com.ellenluo.simpleweather;

/**
 * Activity to display weather fragment and check permissions/network state.
 */

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    LocationManager locationManager;

    SharedPreferences pref;

    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 0;

    /**
     * Initializes application.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check for network connection
        checkNetwork();
    }

    /**
     * Inflates action bar menu.
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar, menu);
        return true;
    }

    /**
     * Action bar menu options.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.change_location) {
            showChangeLocationDialog();
        } else if (item.getItemId() == R.id.refresh) {
            refreshData();
        } else {
            showChangeUnitsDialog();
        }
        return false;
    }

    /**
     * Called when location permission granted (or denied).
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Get location data
                getCurrentLocation();
            } else {
                // Display warning if permission denied
                Toast.makeText(this, getString(R.string.error_location_permission), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Checks if network connected.
     */
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * Displays a warning if network not connected, or gets weather data.
     */
    private void checkNetwork() {
        if (!isNetworkConnected()) {
            displayNetworkWarning();
        } else {
            displayWeatherData();
        }
    }

    /**
     * Shows alert dialog if network not connected.
     */
    private void displayNetworkWarning() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setTitle(getString(R.string.error_no_network));
        dialog.setMessage(getString(R.string.error_no_network_details));
        dialog.setCancelable(false);

        dialog.setPositiveButton(getString(R.string.retry), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                checkNetwork();
            }
        });

        dialog.show();
    }

    /**
     * Sets up weather data.
     */
    private void displayWeatherData() {
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        getSupportFragmentManager().beginTransaction().add(R.id.container, new MainFragment()).commit();
    }

    /**
     * Checks if GPS enabled.
     */
    private boolean isLocationGPSEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Checks if network location enabled.
     */
    private boolean isLocationNetworkEnabled() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Checks if location permission granted.
     */
    private boolean checkLocationPermission() {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * Requests location permission from user.
     */
    private void requestLocationPermission() {
        if (!checkLocationPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    /**
     * Gets current user location using GPS or network.
     */
    private void getCurrentLocation() {
        if (isLocationGPSEnabled() || isLocationNetworkEnabled()) {
            // Location listener
            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        // Store current location with latitude/longitude
                        pref.edit().putFloat("lat", (float) location.getLatitude()).apply();
                        pref.edit().putFloat("lon", (float) location.getLongitude()).apply();
                        pref.edit().putBoolean("using_lat", true).apply();
                        refreshData();
                    }
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onProviderDisabled(String provider) {
                }
            };

            String provider = locationManager.getBestProvider(new Criteria(), false);

            if (checkLocationPermission()) {
                // Check last know location before collecting location data
                Location lastKnownLocation = locationManager.getLastKnownLocation(provider);

                if (lastKnownLocation != null) {
                    // Store current location with latitude/longitude
                    pref.edit().putFloat("lat", (float) lastKnownLocation.getLatitude()).apply();
                    pref.edit().putFloat("lon", (float) lastKnownLocation.getLongitude()).apply();
                    pref.edit().putBoolean("using_lat", true).apply();
                    refreshData();
                } else {
                    if (isLocationGPSEnabled()) {
                        // Request location data from GPS if possible
                        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
                    } else {
                        // Request location data from network
                        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);
                    }
                }
            } else {
                // Unexpected error
                Toast.makeText(this, getString(R.string.error_unexpected), Toast.LENGTH_LONG).show();
            }
        } else {
            // Display location services off error
            Toast.makeText(this, getString(R.string.error_location_off), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Refreshes weather data if network connection found.
     */
    private void refreshData() {
        if (isNetworkConnected()) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new MainFragment()).commit();
        } else {
            // Unable to update error message
            Toast.makeText(MainActivity.this, getString(R.string.error_no_network_update), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sets proper dialog width.
     */
    private void setDialogWidth(Dialog dialog) {
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = width;
        dialog.getWindow().setAttributes(lp);
    }

    /**
     * Displays dialog prompting user to change location.
     */
    private void showChangeLocationDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_change_location);
        setDialogWidth(dialog);

        // Initialize components
        final EditText etZip = (EditText) dialog.findViewById(R.id.change_location_zip);
        final Button btnZip = (Button) dialog.findViewById(R.id.change_location_search_zip);
        final Button btnGPS = (Button) dialog.findViewById(R.id.change_location_gps);
        final Button btnCancel = (Button) dialog.findViewById(R.id.change_location_cancel);

        // Refresh data using zip code
        btnZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int zip = Integer.parseInt(etZip.getText().toString());
                    pref.edit().putInt("zip", zip).apply();
                    pref.edit().putBoolean("using_lat", false).apply();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, getString(R.string.error_location), Toast.LENGTH_LONG).show();
                }

                dialog.dismiss();
                refreshData();
            }
        });

        // Refresh data using current location
        btnGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get current location
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !(checkLocationPermission())) {
                    requestLocationPermission();
                } else {
                    getCurrentLocation();
                }

                // Refresh weather data
                dialog.dismiss();
            }
        });

        // Cancel button
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        dialog.show();
    }

    /**
     * Displays dialog prompting user to change units.
     */
    private void showChangeUnitsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_change_units);
        setDialogWidth(dialog);

        // Initialize components
        final RadioGroup units = (RadioGroup) dialog.findViewById(R.id.change_units_temperature);
        final Button btnSave = (Button) dialog.findViewById(R.id.change_units_save);
        final Button btnCancel = (Button) dialog.findViewById(R.id.change_units_cancel);

        // Set checked radio button
        if (pref.getBoolean("metric", false)) {
            units.check(R.id.metric);
        } else {
            units.check(R.id.imperial);
        }

        // Save button
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = units.getCheckedRadioButtonId();

                // Store preference
                if (id == R.id.imperial) {
                    pref.edit().putBoolean("metric", false).apply();
                } else {
                    pref.edit().putBoolean("metric", true).apply();
                }

                dialog.dismiss();
                refreshData();
            }
        });

        // Cancel button
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        dialog.show();
    }

}
