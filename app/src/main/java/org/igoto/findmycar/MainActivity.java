package org.igoto.findmycar;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.geoindex.codec.GeoIndexUtil;
import org.igoto.findmycar.compass.CompassActivity;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PAIRED_DEVICE = 2;
    private static final int RESULT_SETTINGS = 3;

    /**
     * Called when the activity is first created.
     */
    //private TextView statusTextView;
    //private TextView satellitesTextView;
    //private TextView stateBluetooth;
    private TextView textViewBluetoothdevice;
    private Button buttonCapture;
    private ListView listView;
    private BluetoothAdapter bluetoothAdapter;
    public static MyDB db;

    // GPSTracker class
    private GPSTracker gps;
    private CustomSimpleCursorAdapter dataAdapter;

    private static DecimalFormat df = null;

    private boolean mIsBound;
    private IServiceFunctions service = null;

    private Boolean optimize = true;
    public static Boolean MULTI_MODE = true;

    private static final String[] INITIAL_PERMS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.VIBRATE
    };

    private static final int INITIAL_REQUEST = 1337;

    private boolean canAccessAll() {
        for (String premissionName : INITIAL_PERMS) {
            if (hasPermission(premissionName) != true) {
                return false;
            }
        }

        return true;
    }

    private boolean hasPermission(String perm) {
        return (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, perm));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case INITIAL_REQUEST:
                if (canAccessAll()) {
                    enableBT();
                } else {
                    Toast.makeText(this, "Access denied. Program exit.", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if (!canAccessAll()) {
                {
                    requestPermissions(INITIAL_PERMS, INITIAL_REQUEST);
                }
            }

        enableBT();

        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
        otherSymbols.setDecimalSeparator('.');
        otherSymbols.setGroupingSeparator(',');
        df = new DecimalFormat("0.#######", otherSymbols);

        //statusTextView = (TextView) findViewById(R.id.statusTextView);
        //satellitesTextView = (TextView) findViewById(R.id.satellitesTextView);
        //stateBluetooth = (TextView) findViewById(R.id.bluetoothstate);
        buttonCapture = (Button) findViewById(R.id.buttonCapture);
        buttonCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                service.saveLoaction();
            }
        });


        textViewBluetoothdevice = (TextView) findViewById(R.id.bluetoothdevice);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> arg0, View view,
                                            int position, long id) {
                        cursorContextMenu = (Cursor) listView.getItemAtPosition(position);
                        view.showContextMenu();
                    }
                }
        );

        /*listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View v,
                                           int index, long arg3) {
                Log.d("", "in onLongClick");
                cursorContextMenu = (Cursor) listView.getItemAtPosition(index);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Delete selecte record")
                        .setMessage("Are you sure you want to delete selected record?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue with delete
                                Cursor c = cursorContextMenu;
                                String log_id = c.getString(c.getColumnIndex(AppConst.LOG_ID));
                                db.deleteRecordId(log_id);
                                displayListView();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                return true;
            }
        });*/


        registerForContextMenu(listView);
        CheckBlueToothState();

        // create class object
        gps = new GPSTracker(this);
        gps.getLocation(60000, GPSTracker.GPS_UPDATE_DISTANCE_INTERVAL);
        gps.addFixListner(new GPSTracker.ChangedFix() {
            @Override
            public void valueChangedFix(int isGPSFix, int satellites, int satellitesInFix) {
                String description = "";
                switch (isGPSFix) {
                    case 0:
                        description = "Off";
                        break;
                    case 1:
                        description = "No signal";
                        break;
                    case 2:
                        description = "Waiting for GPS signal";
                        break;
                    case 3:
                        description = "OK";
                        break;
                }
                //statusTextView.setText("GPS status: " + description);
                //satellitesTextView.setText("Satellites: " + satellitesInFix + "/7 fixed" + ", " + satellites + " visible");
            }
        });


        if (gps.canGetLocation()) {
            if (gps.isGPSEnabled) {
                if (gps.isNetworkEnabled) {

                    /*AlertDialog.Builder builder = new AlertDialog.Builder(SMSActivity.this);
                    builder.setTitle("Locations");
                    builder.setMessage("Network enabled position won't get exact location in some places, but it will use less time. Would you like to use GPS only?")
                            .setCancelable(true)
                            .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    SMSActivity.this.startActivity(intent);
                                }
                            }).setNegativeButton("Don't Allow", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();*/
                    gps.stopUsingGPS();
                } else {
                    gps.stopUsingGPS();
                }
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Locations");
                builder.setMessage("GPS is not enabled")
                        .setCancelable(true)
                        .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                gps.showSettingsAlert();

                            }
                        }).setNegativeButton("Don't Allow", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // origin, allow, remember
                        finish();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(this);
        optimize = settings.getBoolean("optimize", true);
        MULTI_MODE = settings.getBoolean("multi_mode", false);
        String device_info = settings.getString("infos", "Manual(Custom)");
        String device_address = settings.getString("addresses", AppConst.DEVICE_MAC_MANUAL);
        if (MULTI_MODE || device_address.equals(AppConst.DEVICE_MAC_MANUAL)) {
            buttonCapture.setVisibility(View.VISIBLE);
        } else {
            buttonCapture.setVisibility(View.GONE);
        }

        if (MULTI_MODE) {
            textViewBluetoothdevice.setVisibility(View.GONE);
        } else {
            textViewBluetoothdevice.setVisibility(View.VISIBLE);
            textViewBluetoothdevice.setText("Device: " + device_info);
        }
        if (service != null && service.isRunning() != null && service.isRunning() == false) {
            startService(new Intent(MainActivity.this, LocalService.class));
            doBindService();
        } else if (service == null) {
            startService(new Intent(MainActivity.this, LocalService.class));
            doBindService();
        }

        db = new MyDB(this);
        displayListView();//Generate ListView from SQLite Database


        if (savedInstanceState != null) {
        } else {
            // Probably initialize members with default values for a new instance
            if (!isMyServiceRunning(LocalService.class)) {
                /*SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
                SharedPreferences.Editor e = sp.edit();
                e.putBoolean(KEY_ONOFF, false);
                e.commit();*/
            }
            {
                Intent bindIntent = new Intent(this, LocalService.class);
                bindService(bindIntent, mConnection, 0);
                mIsBound = true;

            }
        }
    }


    public void displayListView() {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(this);
        String device_info = settings.getString("infos", "Manual(Custom)");
        String device_address = settings.getString("addresses", AppConst.DEVICE_MAC_MANUAL);

        Cursor cursor = null;

        if (!MULTI_MODE) {
            cursor = db.selectAllRecordsSingleMode(device_info, device_address);
        } else {
            cursor = db.selectAllRecordsMultiMode(device_info, device_address);
        }
        String[] columns = new String[]{AppConst.LOG_DATETIME, AppConst.LOG_LONGITUDE, AppConst.LOG_LATITUDE};
        int[] toViewsID = {R.id.textView1, R.id.textView2, R.id.textView3};
        // create the adapter using the cursor pointing to the desired data
        //as well as the layout information
        dataAdapter = new CustomSimpleCursorAdapter(
                this, R.layout.track_info,
                cursor,
                columns,
                toViewsID,
                0);
        /*dataAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence constraint) {
                return db.selectAllRecordsSingleMode();
            }
        });*/
        // Assign adapter to ListView
        listView.setAdapter(dataAdapter);
    }

    public void onStop() {

        super.onStop();
        //updateLocationHandler.removeCallbacks(updateLocationThread);
        /*
         * Check if the HOME key was pressed. If the HOME key was pressed then
         * the app will be killed. Otherwise the user or the app is navigating
         * away from this activity so assume that the HOME key will be pressed
         * next unless a navigation event by the user or the app occurs.
         */
    }

    public void finish() {
        /*
         * This can only invoked by the user or the app finishing the activity
         * by navigating from the activity so the HOME key was not pressed.
         */
        super.finish();
        if (gps != null) {
            gps.stopUsingGPS();
        }

    }

    private void CheckBlueToothState() {
        if (bluetoothAdapter == null) {
            //stateBluetooth.setText("Bluetooth: NOT support");
        } else {
            if (bluetoothAdapter.isEnabled()) {
                if (bluetoothAdapter.isDiscovering()) {
                    //stateBluetooth.setText("Bluetooth: is currently in device discovery process.");
                } else {
                    //stateBluetooth.setText("Bluetooth: is Enabled.");
                }
            } else {
                //stateBluetooth.setText("Bluetooth: is NOT Enabled!");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if (requestCode == REQUEST_ENABLE_BT) {
            CheckBlueToothState();
        } else if (requestCode == REQUEST_PAIRED_DEVICE) {
            if (resultCode == RESULT_OK) {
                SharedPreferences settings = PreferenceManager
                        .getDefaultSharedPreferences(this);
                String device_info = settings.getString("infos", "Manual(Custom)");
                String device_address = settings.getString("addresses", AppConst.DEVICE_MAC_MANUAL);
                if (MULTI_MODE || device_address.equals(AppConst.DEVICE_MAC_MANUAL)) {
                    buttonCapture.setVisibility(View.VISIBLE);

                } else {
                    buttonCapture.setVisibility(View.GONE);
                }

                if (MULTI_MODE) {
                    textViewBluetoothdevice.setVisibility(View.GONE);
                } else {
                    textViewBluetoothdevice.setVisibility(View.VISIBLE);
                    textViewBluetoothdevice.setText("Device: " + device_info);
                }


                if (service != null && service.isRunning() != null && service.isRunning() == true) {
                    doUnbindService();
                    stopService(new Intent(MainActivity.this, LocalService.class));

                }
                startService(new Intent(MainActivity.this, LocalService.class));
                doBindService();
                displayListView();
            }
        } else if (requestCode == RESULT_SETTINGS) {
            SharedPreferences preferences = null;

            SharedPreferences settings = PreferenceManager
                    .getDefaultSharedPreferences(this);
            MULTI_MODE = settings.getBoolean("multi_mode", false);

            displayListView();

            if (MULTI_MODE) {
                textViewBluetoothdevice.setVisibility(View.GONE);
            } else {
                textViewBluetoothdevice.setVisibility(View.VISIBLE);

                String device_info = settings.getString("infos", "");
                textViewBluetoothdevice.setText("Device: " + device_info);
            }
            optimize = settings.getBoolean("optimize", true);
            if (service != null)
                service.setOptimize(optimize);
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_mammy, menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        /*menu.findItem(R.id.checkable_optimize_battary).setChecked(optimize);
        menu.findItem(R.id.checkable_multi_mode).setChecked(MULTI_MODE);*/
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        Intent intent;
        SharedPreferences preferences = null;
        SharedPreferences.Editor edit = null;
        switch (id) {
            /*case R.id.checkable_multi_mode:
                //boolean MULTI_MODE = false;
                if (item.isChecked() == true) {
                    item.setChecked(false);
                    MULTI_MODE = false;
                } else {
                    item.setChecked(true);
                    MULTI_MODE = true;
                }
                //service.setOptimize(MULTI_MODE);
                preferences = PreferenceManager
                        .getDefaultSharedPreferences(this);
                edit = preferences.edit();
                edit.putBoolean("multi_mode", MULTI_MODE);
                edit.commit();
                displayListView();
                if (MULTI_MODE) {
                    textViewBluetoothdevice.setVisibility(View.GONE);
                } else {
                    textViewBluetoothdevice.setVisibility(View.VISIBLE);
                    SharedPreferences settings = PreferenceManager
                            .getDefaultSharedPreferences(this);
                    String device_info = settings.getString("infos", "");
                    textViewBluetoothdevice.setText("Device: " + device_info);
                }
                break;*/
            /*case R.id.checkable_optimize_battary:
                //boolean optimize = false;
                if (item.isChecked() == true) {
                    item.setChecked(false);
                    optimize = false;
                } else {
                    item.setChecked(true);
                    optimize = true;
                }
                service.setOptimize(optimize);
                preferences = PreferenceManager
                        .getDefaultSharedPreferences(this);
                edit = preferences.edit();
                edit.putBoolean("optimize", optimize);
                edit.commit();
                break;*/
            case R.id.action_settings:
                intent = new Intent();
                intent.setClass(MainActivity.this, ListPairedDevicesActivity.class);
                startActivityForResult(intent, REQUEST_PAIRED_DEVICE);
                break;
            case R.id.action_settings_gps:
                intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                this.startActivity(intent);
                break;
            /*case R.id.action_clear_history:
                new AlertDialog.Builder(this)
                        .setTitle("Delete all history logs")
                        .setMessage("Are you sure you want to delete all history logs?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue with delete
                                db.deleteRecords();
                                displayListView();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                break;*/
            case R.id.action_exit:

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Exit");
                builder.setMessage("Do you want completly stop using FindMyCar application?")
                        .setCancelable(true)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                doUnbindService();
                                stopService(new Intent(MainActivity.this, LocalService.class));
                                NotificationManager notifManager = (NotificationManager) MainActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
                                notifManager.cancelAll();
                                finish();
                                System.exit(0);
                            }
                        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // origin, allow, remember
                        finish();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
                break;
            case R.id.action_main_settings:
                Intent i = new Intent(this, MainSettingsActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public static void cancelNotification(Context ctx, int notifyId) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancel(notifyId);
    }

    private Cursor cursorContextMenu;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        //super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.listView) {
            ListView lv = (ListView) v;
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
            cursorContextMenu = (Cursor) lv.getItemAtPosition(acmi.position);
            menu.setHeaderTitle("Actions");
            menu.add(0, v.getId(), 0, "Navigate to object");
            menu.add(0, v.getId(), 0, "Show on Map");
            menu.add(0, v.getId(), 0, "Send SMS with location");
            menu.add(0, v.getId(), 0, "Copy location to cliborad");
            menu.add(0, v.getId(), 0, "Delete");
            menu.add(0, v.getId(), 0, "Cancel");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getTitle() == "Navigate to object") {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, CompassActivity.class);
            Cursor c = cursorContextMenu;
            double longitude = Double.valueOf(c.getString(c.getColumnIndex(AppConst.LOG_LONGITUDE)));
            double latitude = Double.valueOf(c.getString(c.getColumnIndex(AppConst.LOG_LATITUDE)));
            intent.putExtra("longitude", longitude);
            intent.putExtra("latitude", latitude);
            startActivity(intent);
        } else if (item.getTitle() == "Show on Map") {
            Cursor c = cursorContextMenu;
            String url = "http://maps.google.com/maps?q=" + c.getString(c.getColumnIndex(AppConst.LOG_LATITUDE)) + "," + c.getString(c.getColumnIndex(AppConst.LOG_LONGITUDE));
            Uri googlemap_uri = Uri.parse(url);
            Intent resultIntent = new Intent(Intent.ACTION_VIEW, googlemap_uri);
            resultIntent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
            try {
                startActivity(resultIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "No navigation application avalible!", Toast.LENGTH_SHORT).show();
            }
        } else if (item.getTitle() == "Send SMS with location") {
            Cursor c = cursorContextMenu;
            double longitude = Double.valueOf(c.getString(c.getColumnIndex(AppConst.LOG_LONGITUDE)));
            double latitude = Double.valueOf(c.getString(c.getColumnIndex(AppConst.LOG_LATITUDE)));
            String code = GeoIndexUtil.GeoCoordinatesToCodeCompress(longitude, latitude);
            String sendsms = "Location: " + "http://geoindex.org/" + code + " \nLat:" + df.format(latitude) + "° Lon:" + df.format(longitude) + "°";

            Uri sms_uri = Uri.parse("smsto:");
            Intent sms_intent = new Intent(Intent.ACTION_SENDTO, sms_uri);
            sms_intent.putExtra("sms_body", sendsms);
            startActivity(sms_intent);

        } else if (item.getTitle() == "Copy location to cliborad") {
            Cursor c = cursorContextMenu;
            double longitude = Double.valueOf(c.getString(c.getColumnIndex(AppConst.LOG_LONGITUDE)));
            double latitude = Double.valueOf(c.getString(c.getColumnIndex(AppConst.LOG_LATITUDE)));
            String code = GeoIndexUtil.GeoCoordinatesToCodeCompress(longitude, latitude);
            String sendsms = "Location: " + "http://geoindex.org/" + code + " \nLat:" + df.format(latitude) + "° Lon:" + df.format(longitude) + "°";

            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setText(sendsms);
        } else if (item.getTitle() == "Delete") {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete selecte record")
                    .setMessage(MULTI_MODE ? "Are you sure you want to delete selected device log?" : "Are you sure you want to delete selected record?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                            Cursor c = cursorContextMenu;

                            if (MULTI_MODE) {
                                String log_device_address = c.getString(c.getColumnIndex(AppConst.LOG_DEVICE_ADDRESS));
                                db.deleteRecordDeviceAddress(log_device_address);
                            } else {
                                String log_id = c.getString(c.getColumnIndex(AppConst.LOG_ID));
                                db.deleteRecordId(log_id);
                            }
                            displayListView();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else if (item.getTitle() == "Cancel") {
            Toast.makeText(this, "Operation cancled", Toast.LENGTH_SHORT).show();
        } else {
            return false;
        }
        return true;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = (IServiceFunctions) binder;

            try {
                service.registerActivity(MainActivity.this, listener);
            } catch (Throwable t) {
            }
            //showStatusServiceGPSTrace();
        }

        public void onServiceDisconnected(ComponentName className) {

            service = null;
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.

        }
    };

    private void doBindService() {
        Intent bindIntent = new Intent(this, LocalService.class);
        mIsBound = true;
        /*SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        SharedPreferences.Editor e = sp.edit();
        e.putBoolean(KEY_ONOFF, true);
        e.commit();*/
        boolean bindResult = bindService(bindIntent, mConnection, BIND_AUTO_CREATE);
        //PrintLog("Binding service.");
    }

    private void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            /*SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
            SharedPreferences.Editor e = sp.edit();
            e.putBoolean(KEY_ONOFF, false);
            e.commit();*/
            // Detach our existing connection.
            if (mConnection != null) {
                unbindService(mConnection);
            }
            mIsBound = false;
            //PrintLog("Unbinding service.");
        }
    }

    @Override
    protected void onDestroy() {
        // Deactivate updates to us so that we dont get callbacks no more.
        if (service != null)
            service.unregisterActivity(this);

        // Finally stop the service
        if (mIsBound) {
            if (mConnection != null) {
                unbindService(mConnection);
            }
        }
        super.onDestroy();
    }

    /*Handler handler = new Handler();

        @Override
        public void onSharedPreferenceChanged(SharedPreferences pref, String key) {

            if (!mIsBound) {
                // Notifier

                    if (KEY_ONOFF.equals(key) && pref.getBoolean(KEY_ONOFF, false)) {
                        try {
                            mIsBound = true;
                            SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
                            SharedPreferences.Editor e = sp.edit();
                            e.putBoolean(KEY_ONOFF, false);
                            e.commit();
                            Toast.makeText(this, "Napaka. Varnostni pas se je ugasnil.", Toast.LENGTH_SHORT).show();
                        } finally {
                            mIsBound=false;
                        }
                    }

            }
        }*/

    // This is essentially the callback that the service uses to notify us about
    // changes.
    private IListenerFunctions listener = new IListenerFunctions() {
        public void refreshList(String state, double lat, double lon) {
            //Toast.makeText(MainActivity.this, "New paring location.", Toast.LENGTH_LONG).show();
            displayListView();
        }
    };

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void enableBT() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intentBtEnabled = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // The REQUEST_ENABLE_BT constant passed to startActivityForResult() is a locally defined integer (which must be greater than 0), that the system passes back to you in your onActivityResult()
            // implementation as the requestCode parameter.
            int REQUEST_ENABLE_BT = 1;
            startActivityForResult(intentBtEnabled, REQUEST_ENABLE_BT);
        }
    }
}
