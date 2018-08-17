package org.igoto.findmycar;

import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by programer on 29.12.2015.
 */
public class MyDB{

    private MyDatabaseHelper dbHelper;

    private SQLiteDatabase database;

    /**
     *
     * @param context
     */
    public MyDB(Context context){
        dbHelper = new MyDatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
    }


    public long createRecords(String device_name, String device_address, String status, String longitude, String latitude){
        ContentValues values = new ContentValues();
        //values.put(LOG_ID, id);
        values.put(AppConst.LOG_DEVICE_NAME, device_name);
        values.put(AppConst.LOG_DEVICE_ADDRESS, device_address);
        values.put(AppConst.LOG_STATUS, status);
        values.put(AppConst.LOG_LONGITUDE, longitude);
        values.put(AppConst.LOG_LATITUDE, latitude);

        return database.insert(AppConst.LOG_TABLE, null, values);
    }

    public Cursor selectRecords() {
        String[] cols = new String[] {AppConst.LOG_ID, AppConst.LOG_DEVICE_NAME, AppConst.LOG_DEVICE_ADDRESS, AppConst.LOG_STATUS, AppConst.LOG_DATETIME, AppConst.LOG_LONGITUDE, AppConst.LOG_LATITUDE};
        Cursor mCursor = database.query(true, AppConst.LOG_TABLE, cols, null, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }

    public Cursor selectAllRecordsSingleMode(String device_name, String device_address) {
        //String[] cols = new String[] {AppConst.LOG_ID, AppConst.LOG_STATUS, AppConst.LOG_DATETIME, AppConst.LOG_LONGITUDE, AppConst.LOG_LATITUDE};
        //" + AppConst.LOG_STATUS + ", " + AppConst.LOG_DATETIME + ", " + AppConst.LOG_LONGITUDE + ", " + AppConst.LOG_LATITUDE + "
        String [] param = null;
        String selectQuery = "";
        if (device_address != null && device_address.length() != 0 && device_address.equals(AppConst.DEVICE_MAC_MANUAL)){
            param = new String [] {device_address, "Manual"};
            selectQuery = "SELECT * FROM " + AppConst.LOG_TABLE + " WHERE " + AppConst.LOG_DEVICE_ADDRESS + " LIKE ? AND " + AppConst.LOG_STATUS + " = ? ORDER BY " + AppConst.LOG_ID + " DESC;";
        } else if (device_address != null && device_address.length() != 0){
            param = new String [] {device_address, BluetoothDevice.ACTION_ACL_DISCONNECTED};
            selectQuery = "SELECT * FROM " + AppConst.LOG_TABLE + " WHERE " + AppConst.LOG_DEVICE_ADDRESS + " LIKE ? AND " + AppConst.LOG_STATUS + " = ? ORDER BY " + AppConst.LOG_ID + " DESC;";
        } else if (device_name != null && device_name.length() != 0) {
            param = new String [] {device_name, BluetoothDevice.ACTION_ACL_DISCONNECTED};
            selectQuery = "SELECT * FROM " + AppConst.LOG_TABLE + " WHERE " + AppConst.LOG_DEVICE_NAME + " LIKE ? AND " + AppConst.LOG_STATUS + " = ? ORDER BY " + AppConst.LOG_ID + " DESC;";
        } else {
            selectQuery = "SELECT * FROM " + AppConst.LOG_TABLE + " WHERE " + AppConst.LOG_STATUS + " = ? ORDER BY " + AppConst.LOG_ID + " DESC;";
        }

        Cursor mCursor = database.rawQuery(selectQuery, param);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }


    /*select filename ,
       status   ,
       max_date = max( dates )
from some_table t
group by filename , status
having status = '<your-desired-status-here>'
*/
    public Cursor selectAllRecordsMultiMode(String device_name, String device_address) {
        //String[] cols = new String[] {AppConst.LOG_ID, AppConst.LOG_STATUS, AppConst.LOG_DATETIME, AppConst.LOG_LONGITUDE, AppConst.LOG_LATITUDE};
        //" + AppConst.LOG_STATUS + ", " + AppConst.LOG_DATETIME + ", " + AppConst.LOG_LONGITUDE + ", " + AppConst.LOG_LATITUDE + "
        String [] param = null;
        String selectQuery = "";

        selectQuery = "SELECT " + AppConst.LOG_ID + ", " + AppConst.LOG_STATUS +
                ", " + AppConst.LOG_DEVICE_ADDRESS  + ", " + AppConst.LOG_DEVICE_NAME +
                ", " + AppConst.LOG_DATETIME + ", " + AppConst.LOG_LONGITUDE + ", " + AppConst.LOG_LATITUDE +
                ", dateTime = max( " +  AppConst.LOG_DATETIME + " ) FROM " + AppConst.LOG_TABLE +
                " GROUP BY " + AppConst.LOG_DEVICE_ADDRESS +
                " ORDER BY " + AppConst.LOG_ID + " DESC;";

        Cursor mCursor = database.rawQuery(selectQuery, param);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor; // iterate to get each value.
    }

    public void deleteRecords() {
        String delete = "DELETE FROM " + AppConst.LOG_TABLE + ";";
        //Cursor mCursor = database.rawQuery(delete, null);
        int count = database.delete(AppConst.LOG_TABLE, null, null);
    }

    public void deleteRecordId(String id) {
        String where = AppConst.LOG_ID + " = " + id;
        database.delete(AppConst.LOG_TABLE, where, null);
    }

    public void deleteRecordDeviceAddress(String log_device_address) {
        String where = AppConst.LOG_DEVICE_ADDRESS + " = '" + log_device_address + "'";
        database.delete(AppConst.LOG_TABLE, where, null);
    }
}


