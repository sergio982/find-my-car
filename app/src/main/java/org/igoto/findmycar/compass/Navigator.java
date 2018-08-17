package org.igoto.findmycar.compass;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

public class Navigator implements LocationListener {
    private static final String TAG = "Navigator";
    private Location lastKnownLocation;
    private LocationManager locationManager;
    private String locationProvider = LocationManager.GPS_PROVIDER;
    private Location reference;
    private List<Observer> actionListeners = new LinkedList<Observer>();

    public Navigator(Context c) {
        locationManager = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
        lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
    }

    public void registerListener() {
        locationManager.requestLocationUpdates(locationProvider, 1000, 1, this);
    }

    public void unregisterListener() {
        locationManager.removeUpdates(this);
    }

    public void setReferenceLocation(double latitude, double longitude) {
        Log.i(TAG, "nav to new location: " + latitude + "; " + longitude);
        
        Location l = new Location("CUSTOM");
        l.setLatitude(latitude);
        l.setLongitude(longitude);
        reference = l;

        fixGPSLocation();
        notifyListeners();
    }

    public float getBearing() {
        fixGPSLocation();
        if (lastKnownLocation != null && reference != null) {
            float bearTo = lastKnownLocation.bearingTo(reference);
            if (bearTo < 0) {
                bearTo = bearTo + 360;
            }
            return bearTo;
        } else {
            return 0;
        }
    }

    public float getDistance() {
        fixGPSLocation();
        
        Log.i(TAG, "current Location: " + lastKnownLocation + " target location: " +  reference);
        
        if (lastKnownLocation != null && reference != null) {
            return lastKnownLocation.distanceTo(reference);
        } else {
            return 9999;
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.i(TAG, "provider enabled: " + provider + " " + status + " (" + extras + ")");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.i(TAG, "provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.i(TAG, "provider disabled" + provider);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "new location: " + location);
        lastKnownLocation = location;
        notifyListeners();
    }

    public void addObserver(Observer al) {
        actionListeners.add(al);
    }

    private void notifyListeners() {
        for (Observer l : actionListeners) {
            l.onNotified();
        }
    }

    public void resetReference() {
        reference = null;

        onLocationChanged(lastKnownLocation);
    }

    public void fixGPSLocation() {
        if (lastKnownLocation == null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            long GPSLocationTime = 0;
            if (null != locationGPS) {
                GPSLocationTime = locationGPS.getTime();
            }

            long NetLocationTime = 0;

            if (null != locationNet) {
                NetLocationTime = locationNet.getTime();
            }

            if (0 < GPSLocationTime - NetLocationTime) {
                lastKnownLocation = locationGPS;
            } else {
                lastKnownLocation = locationNet;
            }

            onLocationChanged(lastKnownLocation);
        }
    }

    public float getAccuracy() {
        if (lastKnownLocation != null) {
            return lastKnownLocation.getAccuracy();
        } else {
            return 999f;
        }
    }
}
