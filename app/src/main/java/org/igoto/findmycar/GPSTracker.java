package org.igoto.findmycar;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

public class GPSTracker extends Service {

    private static final String TAG = "GPSTracker";
    private final Context mContext;

    // flag for GPS status
    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for GPS status
    boolean canGetLocation = false;

    Location location; // location
    Location locationOld; // location

    double latitude; // latitude
    double longitude; // longitude
    Double altitude; //altitude

    // Declaring a Location Manager
    protected LocationManager mlocManager;
    private Location mLastLocation;
    private LocationListener mlocListener;

    private long mLastLocationMillis = 0;
    private int isGPSFix = 0;
    // The minimum time between updates in milliseconds
    protected static long GPS_UPDATE_TIME_INTERVAL = 3000;  //millis
    // The minimum distance to change Updates in meters
    protected static float GPS_UPDATE_DISTANCE_INTERVAL = 0; //meters



    private MyGPSListener mGpsListener;
    private ChangedFix changedFix;
    private ChangedLocation changedLocation;

    public GPSTracker(Context context) {
        this.mContext = context;
        //getLocation(GPSTracker.GPS_UPDATE_TIME_INTERVAL, GPSTracker.GPS_UPDATE_DISTANCE_INTERVAL);
    }

    public void addFixListner(ChangedFix changedFix){
        this.changedFix = changedFix;
    }
    public void addLocationListner(ChangedLocation changedLocation){
        this.changedLocation = changedLocation;
    }

    public Location getLocation(long GPS_UPDATE_TIME_INTERVAL, float GPS_UPDATE_DISTANCE_INTERVAL) {
        GPSTracker.GPS_UPDATE_TIME_INTERVAL = GPS_UPDATE_TIME_INTERVAL;
        GPSTracker.GPS_UPDATE_DISTANCE_INTERVAL = GPS_UPDATE_DISTANCE_INTERVAL;

        try {
            mlocManager = (LocationManager) mContext
                    .getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = mlocManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            mlocListener = new MyLocationListener();
            mGpsListener = new MyGPSListener();
            mlocManager.addGpsStatusListener(mGpsListener);
            //mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATE_TIME_INTERVAL, GPS_UPDATE_DISTANCE_INTERVAL, mlocListener);

            // getting network status
            isNetworkEnabled = mlocManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                this.canGetLocation = true;
                if (isNetworkEnabled) {
                    mlocManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            GPS_UPDATE_TIME_INTERVAL,
                            GPS_UPDATE_DISTANCE_INTERVAL, mlocListener);
                    Log.d("Network", "Network");
                    if (mlocManager != null) {
                        location = mlocManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        mlocManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                GPS_UPDATE_TIME_INTERVAL,
                                GPS_UPDATE_DISTANCE_INTERVAL, mlocListener);
                        Log.d("GPS Enabled", "GPS Enabled");
                        if (mlocManager != null) {
                            location = mlocManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return location;
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     */
    public void startUsingGPS() {
        if (mlocManager != null) {
            mlocManager.removeUpdates(mlocListener);
        }
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     */
    public void stopUsingGPS() {
        if (mlocManager != null) {
            mlocManager.removeUpdates(mlocListener);
            mlocManager.removeGpsStatusListener(mGpsListener);
            changedFix = null;
        }
    }

    /**
     * Function to get latitude
     */
    public double getLatitude() {
        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
        }

        // return latitude
        return latitude;
    }

    /**
     * Function to get longitude
     */
    public double getLongitude() {
        if (mLastLocation != null) {
            longitude = mLastLocation.getLongitude();
        }

        // return longitude
        return longitude;
    }

    /**
     * Function to get altitude
     */
    public Double getAltitude() {
        if (mLastLocation != null && mLastLocation.hasAltitude()) {
            altitude = mLastLocation.getAltitude();
        } else {
            altitude = null;
        }

        // return altitude
        return altitude;
    }

    /**
     * Function to check GPS/wifi enabled
     *
     * @return boolean
     */
    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    /**
     * Function to show settings alert dialog
     * On pressing Settings button will lauch Settings Options
     */
    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // Setting Dialog Title
        alertDialog.setTitle("Location is settings");

        // Setting Dialog Message
        alertDialog.setMessage("Location service is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public class MyLocationListener implements LocationListener
    {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                mLastLocationMillis = SystemClock.elapsedRealtime();
                // do some things here
                mLastLocation = location;

            }
            if (changedLocation != null) {
                changedLocation.locationChanged(location, isGPSFix);
            }
        }
        @Override
        public void onProviderDisabled(String provider)
        {
            isGPSFix = 1;
            //imgGpsState.setImageDrawable(ctx.getResources().getDrawable(R.drawable.gps_on_red));
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            isGPSFix = 2;
            //imgGpsState.setImageDrawable(ctx.getResources().getDrawable(R.drawable.gps_on_orange));
        }

        //this doesn't trigger on Android 2.x users say
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
        }
    }

    private class MyGPSListener implements GpsStatus.Listener {
        public void onGpsStatusChanged(int event) {
            boolean isGPSFix1 = false;
            int satellites = 0;
            int satellitesInFix = 0;
            switch (event) {
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    if (mLastLocation != null)
                        if ((SystemClock.elapsedRealtime() - mLastLocationMillis) < GPS_UPDATE_TIME_INTERVAL * 2) {
                            isGPSFix1 = true;
                        }

                    if (isGPSFix1) { // A fix has been acquired.
                        //imgGpsState.setImageDrawable(ctx.getResources().getDrawable(R.drawable.gps_on_green));
                        isGPSFix = 3;
                    } else { // The fix has been lost.
                        //imgGpsState.setImageDrawable(ctx.getResources().getDrawable(R.drawable.gps_on_orange));
                        isGPSFix = 2;
                    }

                    int timetofix = mlocManager.getGpsStatus(null).getTimeToFirstFix();
                    Log.i(TAG, "Time to first fix = " + timetofix);
                    for (GpsSatellite sat : mlocManager.getGpsStatus(null).getSatellites()) {
                        if(sat.usedInFix()) {
                            satellitesInFix++;
                        }
                        satellites++;
                    }
                    Log.i(TAG, satellites + " Used In Last Fix (" + satellitesInFix + ")");


                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    //imgGpsState.setImageDrawable(ctx.getResources().getDrawable(R.drawable.gps_on_green));
                    isGPSFix = 3;
                    break;
                case GpsStatus.GPS_EVENT_STARTED:
                    //imgGpsState.setImageDrawable(ctx.getResources().getDrawable(R.drawable.gps_on_orange));
                    isGPSFix = 2;
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    //imgGpsState.setImageDrawable(ctx.getResources().getDrawable(R.drawable.gps_on_red));
                    isGPSFix = 1;
                    break;
            }
            if (changedFix != null) {
                changedFix.valueChangedFix(isGPSFix, satellites, satellitesInFix);
            }
        }
    }

    interface ChangedFix{
        void valueChangedFix(int isGPSFix, int satellites, int satellitesInFix);
    }

    interface ChangedLocation{
        void locationChanged(Location location, int isGPSFix);
    }
}
