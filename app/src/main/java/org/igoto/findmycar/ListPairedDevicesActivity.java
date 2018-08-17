package org.igoto.findmycar;

import java.util.ArrayList;
import java.util.Set;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class ListPairedDevicesActivity extends AppCompatActivity {
    private static BluetoothAdapter mBtAdapter;
    private final static int REQUEST_ENABLE_BT = 1;
    private DeviceSimpleCustomAdapter dataAdapter = null;
    private Button buttonOk;
    private Button buttonCancel;
    private ArrayList<DeviceBean> deviceBeanList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_paired_devices);
        this.setTitle("Please select car device");
        deviceBeanList = new ArrayList<DeviceBean>();

        BluetoothAdapter bluetoothAdapter
                = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices
                = bluetoothAdapter.getBondedDevices();
        SharedPreferences settings = getSharedPreferences("prefName", 0);
        String [] addresses = settings.getString("addresses", "").split(",");

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                DeviceBean db = new DeviceBean();
                db.setName(device.getName());
                db.setDeviceAddress(device.getAddress());
                db.setDeviceType(getBTMajorDeviceClass(device
                        .getBluetoothClass()
                        .getMajorDeviceClass()));
                db.setSelected(false);
                for (String address : addresses) {
                    if (address.equals(db.getDeviceAddress())) {
                        db.setSelected(true);
                    }
                }


                deviceBeanList.add(db);
            }
        }
        DeviceBean db = new DeviceBean();
        db.setName("Manual");
        db.setDeviceAddress(AppConst.DEVICE_MAC_MANUAL);
        db.setDeviceType("Custom");
        db.setSelected(false);
        for (String address : addresses) {
            if (address.equals(db.getDeviceAddress())) {
                db.setSelected(true);
            }
        }
        deviceBeanList.add(db);

        //create an ArrayAdaptar from the String Array
        dataAdapter = new DeviceSimpleCustomAdapter(this,
                R.layout.device_info, deviceBeanList);
        ListView listView = (ListView) findViewById(R.id.listView);
        // Assign adapter to ListView
        listView.setAdapter(dataAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SharedPreferences preferences = PreferenceManager
                        .getDefaultSharedPreferences(ListPairedDevicesActivity.this);
                SharedPreferences.Editor edit = preferences.edit();
                Object o = parent.getItemAtPosition(position);
                DeviceBean db = (DeviceBean) o;
                edit.putString("infos", db.toString());
                edit.putString("names", db.getName());
                edit.putString("types", db.getDeviceType());
                edit.putString("addresses", db.getDeviceAddress());
                edit.commit();
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });

        buttonOk = (Button) findViewById(R.id.buttonOk);
        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String infos = "";
                String names = "";
                String types = "";
                String addresess = "";
                SharedPreferences preferences = PreferenceManager
                        .getDefaultSharedPreferences(ListPairedDevicesActivity.this);
                SharedPreferences.Editor edit = preferences.edit();
                for (DeviceBean db : deviceBeanList) {
                    if (db.isSelected()) {
                        if (infos.isEmpty()) {
                            infos += db.toString();
                            names += db.getName();
                            types += db.getDeviceType();
                            addresess += db.getDeviceAddress();
                        } else {
                            infos += "," + db.toString();
                            names += "," + db.getName();
                            types += "," + db.getDeviceType();
                            addresess += "," + db.getDeviceAddress();
                        }
                    }
                }
                edit.putString("infos", infos);
                edit.putString("names", names);
                edit.putString("types", types);
                edit.putString("addresses", addresess);
                edit.commit();
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        buttonCancel = (Button) findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(RESULT_CANCELED, intent);
                finish();
            }
        });

        if (MainActivity.MULTI_MODE) {
            buttonOk.setVisibility(View.VISIBLE);
            buttonCancel.setVisibility(View.VISIBLE);
        } else {
            buttonOk.setVisibility(View.GONE);
            buttonCancel.setVisibility(View.GONE);
        }
    }

    private String getBTMajorDeviceClass(int major) {
        switch (major) {
            case BluetoothClass.Device.Major.AUDIO_VIDEO:
                return "AUDIO_VIDEO";
            case BluetoothClass.Device.Major.COMPUTER:
                return "COMPUTER";
            case BluetoothClass.Device.Major.HEALTH:
                return "HEALTH";
            case BluetoothClass.Device.Major.IMAGING:
                return "IMAGING";
            case BluetoothClass.Device.Major.MISC:
                return "MISC";
            case BluetoothClass.Device.Major.NETWORKING:
                return "NETWORKING";
            case BluetoothClass.Device.Major.PERIPHERAL:
                return "PERIPHERAL";
            case BluetoothClass.Device.Major.PHONE:
                return "PHONE";
            case BluetoothClass.Device.Major.TOY:
                return "TOY";
            case BluetoothClass.Device.Major.UNCATEGORIZED:
                return "UNCATEGORIZED";
            case BluetoothClass.Device.Major.WEARABLE:
                return "AUDIO_VIDEO";
            default:
                return "unknown!";
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }
}
