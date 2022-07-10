package com.example.arewethereyetvenkat34;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private LocationManager locationManager = null;

    private SharedPreferences settings = null;

    private double latitude = 0;
    private double longitude = 0;
    private boolean valid = false;

    private double toLatitude = 0;
    private double toLongitude = 0;

    private final static String TO = "to";
    private final static String TOLAT = "tolat";
    private final static String TOLONG = "tolong";
    Uri gmmIntentUri = Uri.parse("");
    private String to = "";
    private ActiveListener activeListener = new ActiveListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        to = settings.getString(TO, "2250 Engineering");
        toLatitude = Double.parseDouble(settings.getString(TOLAT, "42.724303"));
        toLongitude = Double.parseDouble(settings.getString(TOLONG, "-84.480507"));

        // Get the location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Force the screen to say on and bright
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Called when this application becomes foreground again.
     */
    @Override
    protected void onResume() {
        super.onResume();

        TextView viewProvider;
        viewProvider = (TextView) findViewById(R.id.textProvider);
        viewProvider.setText("");

        setUI();
        registerListeners();
    }

    /**
     * Called when this application is no longer the foreground application.
     */
    @Override
    protected void onPause() {
        unregisterListeners();
        super.onPause();
    }
    /**
     * Handle an options menu selection
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.itemSparty:
                newTo("Sparty", 42.731138, -84.487508);
                return true;

            case R.id.itemHome:
                newTo("Home", 42.732774, -84.466689);
                return true;
            case R.id.minskoff:
                newTo("Minskoff Pavillion", 42.7269258, -84.4733730);
                return true;
            case R.id.item2250:
                newTo("2250 Engineering", 42.724303, -84.480507);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void transportationMode(int id)
    {
        if(id==1)
        {
            gmmIntentUri = Uri.parse("geo:"+toLatitude+","+toLongitude+"?q=101+main+street&mode=d");
        }
        else if(id==2)
        {
            gmmIntentUri = Uri.parse("geo:"+toLatitude+","+toLongitude+"?q=101+main+street&mode=w");
        }
        else
        {
            gmmIntentUri = Uri.parse("geo:"+toLatitude+","+toLongitude+"?q=101+main+street&mode=b");
        }

    }

    public void redirectMap(View view)
    {
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    public void onDriving(View view) {transportationMode(1);}
    public void onWalking(View view) {transportationMode(2);}
    public void onCycling(View view) {transportationMode(3);}


    /**
     * Handle setting a new "to" location.
     * @param address Address to display
     * @param lat latitude
     * @param lon longitude
     */
    private void newTo(String address, double lat, double lon) {
        to = address;
        toLatitude = lat;
        toLongitude = lon;

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(TO,to);
        editor.putString(TOLAT,String.valueOf(toLatitude));
        editor.putString(TOLONG,String.valueOf(toLongitude));
        editor.apply();

        setUI();
    }

    public void onNew(View view) {
        EditText location = (EditText)findViewById(R.id.editLocation);
        final String address = location.getText().toString().trim();
        newAddress(address);
    }

    private void newAddress(final String address) {
        if (address.equals("")) {
            // Don't do anything if the address is blank
            return;
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                lookupAddress(address);

            }

        }).start();
    }
    /**
     * Look up the provided address. This works in a thread!
     * @param address Address we are looking up
     */
    private void lookupAddress(String address) {

        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.US);
        boolean exception = false;
        List<Address> locations;
        try {
            locations = geocoder.getFromLocationName(address, 1);
        } catch(IOException ex) {
            // Failed due to I/O exception
            locations = null;
            exception = true;
        }
        final boolean exception_final = exception;
        final List<Address> location_final = locations;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                newLocation(address, exception_final, location_final);
            }
        });
    }
    private void newLocation(String address, boolean exception, List<Address> locations) {

        if(exception) {
            Toast.makeText(MainActivity.this, R.string.exception, Toast.LENGTH_SHORT).show();
        } else {
            if(locations == null || locations.size() == 0) {
                Toast.makeText(this, R.string.couldnotfind, Toast.LENGTH_SHORT).show();
                return;
            }

            EditText location = (EditText)findViewById(R.id.editLocation);
            location.setText("");

            // We have a valid new location
            Address a = locations.get(0);
            newTo(address, a.getLatitude(), a.getLongitude());

        }
    }


    /**
     * Set all user interface components to the current state
     */
    private void setUI() {
        TextView viewText = (TextView) findViewById(R.id.textTo);
        TextView viewLatitude = (TextView) findViewById(R.id.textLatitude);
        TextView viewLongitude = (TextView) findViewById(R.id.textLongitude);
        TextView viewDistance = (TextView) findViewById(R.id.textDistance);
        viewText.setText(to);
        if (valid) {
            viewLatitude.setText(String.valueOf(latitude));
            viewLongitude.setText(String.valueOf(longitude));
            float distance[] = new float[1];
            Location.distanceBetween(latitude, longitude, toLatitude, toLongitude, distance);
            viewDistance.setText(String.format("%1$6.1fm", distance[0]));
        } else {
            viewLatitude.setText("");
            viewLongitude.setText("");
            viewDistance.setText("");
        }
    }

    private void registerListeners() {
        unregisterListeners();
        // Create a Criteria object
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setAltitudeRequired(true);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(false);
        String bestAvailable = locationManager.getBestProvider(criteria, true);
        if(bestAvailable != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(bestAvailable, 500, 1, activeListener);
            TextView viewProvider = (TextView)findViewById(R.id.textProvider);
            viewProvider.setText(bestAvailable);
            Location location = locationManager.getLastKnownLocation(bestAvailable);
            onLocation(location);
        }
    }

    private void unregisterListeners() {
        locationManager.removeUpdates(activeListener);
    }

    private void onLocation(Location location) {
        if(location == null) {
            return;
        }

        latitude = location.getLatitude();
        longitude = location.getLongitude();
        valid = true;

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(TO, to);
        editor.putString(TOLAT, String.valueOf(toLatitude));
        editor.putString(TOLONG, String.valueOf(toLongitude));

        editor.apply();

        setUI();
    }

    private class ActiveListener implements LocationListener {


        @Override
        public void onLocationChanged(Location location) {
            onLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            LocationListener.super.onStatusChanged(provider, status, extras);
        }

        @Override
        public void onProviderEnabled(String provider) {
            LocationListener.super.onProviderEnabled(provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            registerListeners();
        }
    };

}