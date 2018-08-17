package org.igoto.findmycar.compass;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;

import org.igoto.findmycar.R;

public class CompassActivity extends Activity {
    private Compass myCompass;
    private Navigator myNavigator;
    private CompassView compassView;
    private DistanceTextView distanceText;
    private AccuracyTextView accuracyText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        //Constants.TEXT_TYPEFACE = Typeface.createFromAsset(getAssets(), "fonts/SpecialElite.ttf");
        Constants.LOCALE = getResources().getConfiguration().locale;

        compassView = (CompassView) findViewById(R.id.compassView);
        distanceText = (DistanceTextView) findViewById(R.id.distanceText);
        accuracyText = (AccuracyTextView) findViewById(R.id.accuracyText);

        if (!gpsEnabled()) {
            // GPS OFF
            askToEnableGPS();
        }

        intializeServices();

        compassView.setNavigator(myNavigator);
        compassView.setCompass(myCompass);
        distanceText.setNavigator(myNavigator);
        accuracyText.setNavigator(myNavigator);

        Bundle extras = getIntent().getExtras();

        myNavigator.setReferenceLocation(extras.getDouble("latitude"), extras.getDouble("longitude"));
    }

    private void askToEnableGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, please enable it!").setCancelable(false)
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog,
                                        @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private boolean gpsEnabled() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void intializeServices() {
        myCompass = new Compass(this);
        myNavigator = new Navigator(this);
    }


    @Override
    protected void onResume() {
        myCompass.registerListener(SensorManager.SENSOR_DELAY_NORMAL);
        myNavigator.registerListener();
        super.onResume();
    }

    @Override
    protected void onPause() {
        myCompass.unregisterListener();
        myNavigator.unregisterListener();

        super.onPause();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }
}