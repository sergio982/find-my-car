package org.igoto.findmycar;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by programer on 27.12.2015.
 */
public class MyDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "DBName";

    private static final int DATABASE_VERSION = 2;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table if not exists TrackLog ( " +
            "_id INTEGER primary key," +
            "device_name TEXT," +
            "device_address TEXT," +
            "status INTEGER," +
            "dateTime DATETIME DEFAULT (DATETIME(CURRENT_TIMESTAMP, 'LOCALTIME')),"+ //"dateTime DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "latitude REAL, " +
            "longitude REAL)";

    public MyDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

    // Method is called during an upgrade of the database,
    @Override
    public void onUpgrade(SQLiteDatabase database,int oldVersion,int newVersion){
        Log.w(MyDatabaseHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS TrackLog");
        onCreate(database);
    }
}