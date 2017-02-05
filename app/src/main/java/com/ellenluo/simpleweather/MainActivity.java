package com.ellenluo.simpleweather;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    LocationManager locationManager;
    SharedPreferences pref;

    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !(checkLocationPermission())) {
            requestLocationPermission();
        } else {
            getCurrentLocation();
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container, new MainFragment()).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.change_city) {
            showChangeLocationDialog();
        } else if (item.getItemId() == R.id.refresh) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new MainFragment()).commit();
        } else {
            showChangeUnitsDialog();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, getString(R.string.location_permission_error), Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isLocationGPSEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private boolean isLocationNetworkEnabled() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private boolean checkLocationPermission() {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    private void requestLocationPermission() {
        if (!checkLocationPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    private void getCurrentLocation() {
        if (isLocationGPSEnabled() || isLocationNetworkEnabled()) {
            LocationListener locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    Log.d("MainActivity", "called");
                    if (location != null) {
                        pref.edit().putFloat("lat", (float) location.getLatitude()).apply();
                        pref.edit().putFloat("lon", (float) location.getLongitude()).apply();
                        pref.edit().putBoolean("using_lat", true).apply();
                        Log.d("MainActivity", "location logged");
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

            // Register the listener with the Location Manager to receive location updates
            if (checkLocationPermission()) {
                Location lastKnownLocation = locationManager.getLastKnownLocation(provider);

                if (lastKnownLocation != null) {
                    pref.edit().putFloat("lat", (float) lastKnownLocation.getLatitude()).apply();
                    pref.edit().putFloat("lon", (float) lastKnownLocation.getLongitude()).apply();
                    pref.edit().putBoolean("using_lat", true).apply();

                    Log.d("MainActivity", "last known location logged");
                } else {
                    if (isLocationGPSEnabled()) {
                        Log.d("MainActivity", "using GPS");
                        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
                    } else {
                        Log.d("MainActivity", "using network");
                        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, null);
                    }
                }
            } else {
                Toast.makeText(this, getString(R.string.location_permission_error), Toast.LENGTH_LONG).show();
            }
        } else

        {
            Toast.makeText(this, getString(R.string.location_error), Toast.LENGTH_LONG).show();
        }
    }

    private void showChangeLocationDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_change_location);
        setDialogWidth(dialog);

        // initialize components
        final EditText etZip = (EditText) dialog.findViewById(R.id.change_location_zip);
        final Button btnZip = (Button) dialog.findViewById(R.id.change_location_search_zip);
        final Button btnGPS = (Button) dialog.findViewById(R.id.change_location_gps);
        final Button btnCancel = (Button) dialog.findViewById(R.id.change_location_cancel);

        btnZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int zip = Integer.parseInt(etZip.getText().toString());

                pref.edit().putInt("zip", Integer.parseInt(etZip.getText().toString())).apply();
                pref.edit().putBoolean("using_lat", false).apply();
                dialog.dismiss();
                getSupportFragmentManager().beginTransaction().replace(R.id.container, new MainFragment()).commit();
            }
        });

        btnGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !(checkLocationPermission())) {
                    requestLocationPermission();
                } else {
                    getCurrentLocation();
                }
                dialog.dismiss();
                getSupportFragmentManager().beginTransaction().replace(R.id.container, new MainFragment()).commit();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        dialog.show();
    }

    private void showChangeUnitsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_change_units);
        setDialogWidth(dialog);

        // initialize components
        final RadioGroup units = (RadioGroup) dialog.findViewById(R.id.change_units_temperature);
        final Button btnSave = (Button) dialog.findViewById(R.id.change_units_save);
        final Button btnCancel = (Button) dialog.findViewById(R.id.change_units_cancel);

        if (pref.getBoolean("metric", false)) {
            units.check(R.id.metric);
        } else {
            units.check(R.id.imperial);
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = units.getCheckedRadioButtonId();

                if (id == R.id.imperial) {
                    pref.edit().putBoolean("metric", false).apply();
                } else {
                    pref.edit().putBoolean("metric", true).apply();
                }

                dialog.dismiss();
                getSupportFragmentManager().beginTransaction().replace(R.id.container, new MainFragment()).commit();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        dialog.show();
    }

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

}
