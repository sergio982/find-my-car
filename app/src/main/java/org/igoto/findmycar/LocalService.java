package org.igoto.findmycar;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalService extends Service {

    private int mValue = 0; // Holds last value set by a client.

    private boolean isRunning = false;
    private NotificationManager nm;
    private String addresses[] = null;
    private MyDB db = null;
    private GPSTracker gps;
    private Map<Activity, IListenerFunctions> clients = new ConcurrentHashMap<Activity, IListenerFunctions>();
    private final Binder binder = new LocalBinder();
    private boolean optimize = true;
    private boolean vibrate = false;
    private boolean ringtone = false;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    public LocalService() {

    }


    @Override
    public void onCreate() {
        super.onCreate();
    }

    private boolean onlyOneLoc = false;
    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Toast.makeText(LocalService.this, "BT change received !", Toast.LENGTH_LONG).show();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Toast.makeText(LocalService.this, device.getName() + " Device found", Toast.LENGTH_LONG).show();
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Toast.makeText(LocalService.this, device.getName() + " Device is now connected", Toast.LENGTH_LONG).show();
                //connected
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                Toast.makeText(LocalService.this, device.getName() + " Device is about to disconnect", Toast.LENGTH_LONG).show();
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Toast.makeText(LocalService.this, device.getName() + " Device has disconnected", Toast.LENGTH_LONG).show();
                //disconected
            }
            for (String address : addresses) {
                if (!address.equals(AppConst.DEVICE_MAC_MANUAL) && address.equals(device.getAddress())) {
                    if (!optimize) {
                        if (gps.canGetLocation()) {
                            //String smsMessage = "longitude: " + longitude + ", Latitude: " + latitude + "";
                            //String code = GeoIndexUtil.GeoCoordinatesToCodeCompress(longitude, latitude);
                            //    smsMessage += " URL: " + "http://geoindex.org/" + code;
                            saveAndShowRecord(device.getName(), device.getAddress(), action, gps.getLatitude(), gps.getLongitude());
                        }

                    } else {
                        getLocation(device, action);
                    }
                }
            }
        }
    };

    private void getLocation(final BluetoothDevice device, final String action) {
        onlyOneLoc = true;
        gps = new GPSTracker(LocalService.this);
        gps.getLocation(1 * 1000, GPSTracker.GPS_UPDATE_DISTANCE_INTERVAL);
        if (gps.isNetworkEnabled && gps.canGetLocation()) {
            if (device != null) {
                saveAndShowRecord(device.getName(), device.getAddress(), action, gps.getLatitude(), gps.getLongitude());
            } else {
                saveAndShowRecord("Manual", AppConst.DEVICE_MAC_MANUAL, action, gps.getLatitude(), gps.getLongitude());
            }

            if (gps != null) {
                gps.stopUsingGPS();
                gps = null;
            }
        } else if (gps.isGPSEnabled) {
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
                            if (onlyOneLoc) {
                                onlyOneLoc = false;

                                if (device != null) {
                                    saveAndShowRecord(device.getName(), device.getAddress(), action, gps.getLatitude(), gps.getLongitude());
                                } else {
                                    saveAndShowRecord("Manual", AppConst.DEVICE_MAC_MANUAL, action, gps.getLatitude(), gps.getLongitude());
                                }
                            }
                            if (gps != null) {
                                gps.stopUsingGPS();
                                gps = null;
                            }

                            break;
                    }
                }

            });
        }
    }

    private void saveAndShowRecord(String device_name, String device_address, String status, double latitude, double longitude) {
        db.createRecords(device_name, device_address, status, String.valueOf(longitude), String.valueOf(latitude));

        for (Activity client : clients.keySet()) {
            IListenerFunctions callback = clients.get(client);
            callback.refreshList(status, latitude, longitude);
        }
        if (status.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED) || status.equals("Manual")) {
            if (ringtone) {
                playRingtone();
            }

            if (vibrate) {
                Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                v.vibrate(500);
            }
        }
    }


    private void showNotification() {
        nm = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.mipmap.ic_launcher, text, System.currentTimeMillis());
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        // Set the info for the views that show in the notification panel.
        Context context = this;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            notification = new Notification();
            notification.icon = R.mipmap.ic_launcher;
            try {
                Method deprecatedMethod = notification.getClass().getMethod("setLatestEventInfo", Context.class, CharSequence.class, CharSequence.class, PendingIntent.class);
                deprecatedMethod.invoke(this, getText(R.string.local_service_label), text, contentIntent);
            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                Log.w("", "Method not found", e);
            }
        } else {
            // Use new API
            Notification.Builder builder = new Notification.Builder(context)
                    .setContentIntent(contentIntent)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(text);
            notification = builder.build();
        }
        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        nm.notify(R.string.local_service_started, notification);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(this);
        String info = settings.getString("infos", "");
        addresses = settings.getString("addresses", "").split(",");
        optimize = settings.getBoolean("optimizes", true);
        LoadSettings();
        db = new MyDB(this);

        if (addresses == null || addresses.length == 0) {
            Toast.makeText(this, "Please select bluetooth device.", Toast.LENGTH_LONG).show();
            return START_FLAG_RETRY;
        }
        gps = new GPSTracker(LocalService.this);
        if (!optimize) {
            gps.getLocation(10 * 1000, 0);
        }

        Toast.makeText(this, R.string.local_service_started, Toast.LENGTH_LONG).show();

        buildResources();
        isRunning = true;
        showNotification();

        return START_STICKY;
    }

    public void buildResources() {
        IntentFilter filter1, filter2, filter3, filter4;
        filter1 = new IntentFilter("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        filter2 = new IntentFilter(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter3 = new IntentFilter(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED);
        filter4 = new IntentFilter(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        this.registerReceiver(mReceiver, filter1);
        this.registerReceiver(mReceiver, filter2);
        this.registerReceiver(mReceiver, filter3);
        this.registerReceiver(mReceiver, filter4);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseResources();
    }

    public class LocalBinder extends Binder implements IServiceFunctions {


        // Registers a Activity to receive updates
        public void registerActivity(Activity activity, IListenerFunctions callback) {
            clients.clear();
            clients.put(activity, callback);

        }

        public void unregisterActivity(Activity activity) {
            clients.remove(activity);
        }

        @Override
        public Boolean isRunning() {
            return isRunning;
        }

        @Override
        public void shutDownService() {

            LocalService.this.stopSelf();
            releaseResources();
        }

        @Override
        public void saveLoaction() {
            getLocation(null, "Manual");
        }

        @Override
        public void updateSettings() {

        }

        @Override
        public void setOptimize(Boolean optimize) {
            LocalService.this.optimize = optimize;
            gps = new GPSTracker(LocalService.this);
            if (!optimize) {
                gps.getLocation(10 * 1000, 0);
            }
        }

    }

    public void releaseResources() {
        this.unregisterReceiver(mReceiver);
        Toast.makeText(LocalService.this, "FindMyCar shutdown.", Toast.LENGTH_LONG).show();
        //customHandler.removeCallbacks(updateTimerThread);

        if (gps != null)
            gps.stopUsingGPS();

        gps = null;
        if (nm != null) {
            nm.cancel(R.string.local_service_stopped); // Cancel the persistent notification.
            nm.cancelAll();
        }
        nm = null;
        Log.i("LocalService", "FindMyCar shutdown.");
        isRunning = false;
        //Toast.makeText(LocalService.this, "GPS sledenje - izključeno", Toast.LENGTH_SHORT).show();
    }

    private void LoadSettings() {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(LocalService.this);
        vibrate = settings.getBoolean("vibrate", false);
        ringtone = settings.getBoolean("ringtone", false); //RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI);
    }

    private void playRingtone() {
        //Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        Uri alert = null;
        //if (alert == null) {
            // alert is null, using backup
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            // I can't see this ever being null (as always have a default notification)
            // but just incase
            if (alert == null) {
                // alert backup is null, using 2nd backup
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        //}

        MediaPlayer player = MediaPlayer.create(this, alert);
        player.setLooping(false);
        player.start();
    }

    /*public synchronized static void createNotification(Context context,
                                                       String message, int soundFile, int notifyId) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                context);
        Intent notificationIntent = new Intent(context, DashBoardActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent;
        contentIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);
        mBuilder.setSmallIcon(R.drawable.notification)
                .setContentIntent(contentIntent)
                .setContentTitle(
                        context.getResources().getString(
                                R.string.schoolezone_map_title))
                .setContentText(message)
                .setAutoCancel(true)
                .setStyle(
                        new NotificationCompat.BigTextStyle().bigText(message));

        Uri sound = Uri.parse("android.resource://" + context.getPackageName()
                + "/" + soundFile);
        mBuilder.setSound(sound, 1);
        int mNotificationId = notifyId;
        NotificationManager mNotifyMgr = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = mBuilder.build();
        // notification.sound = sound;
        notification.flags |= Notification.DEFAULT_LIGHTS;
        notification.flags |= Notification.DEFAULT_VIBRATE;
        mNotifyMgr.notify(mNotificationId, notification);
    }*/
}