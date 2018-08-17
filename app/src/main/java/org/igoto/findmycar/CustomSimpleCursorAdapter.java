package org.igoto.findmycar;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import org.geoindex.codec.GeoIndexUtil;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by programer on 1.5.2016.
 */
public class CustomSimpleCursorAdapter extends SimpleCursorAdapter {


    Cursor c;
    Context context;
    Activity activity;

    public CustomSimpleCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int i) {
        super(context, layout, c, from, to);

        this.c = c;
        this.context=context;
        this.activity=(Activity) context;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null)
            convertView = View.inflate(context, R.layout.track_info, null);
        View row = convertView;

        c.moveToPosition(position);


        TextView deviceName = (TextView) convertView.findViewById(R.id.textViewDeviceName);
        TextView time = (TextView) convertView.findViewById(R.id.textView1);
        TextView longitude = (TextView) convertView.findViewById(R.id.textView2);
        TextView latitude = (TextView) convertView.findViewById(R.id.textView3);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
        DateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        utcFormat.setTimeZone(TimeZone.getDefault());

        Date date = null;
        try {
            date = utcFormat.parse(c.getString(c.getColumnIndex(AppConst.LOG_DATETIME)));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        DateFormat pstFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        //pstFormat.setTimeZone(TimeZone.getTimeZone("PST"));

        time.setText(pstFormat.format(date));

        if (MainActivity.MULTI_MODE) {
            deviceName.setVisibility(View.VISIBLE);
            deviceName.setText(c.getString(c.getColumnIndex(AppConst.LOG_DEVICE_NAME)));
        } else {
            deviceName.setVisibility(View.GONE);
        }


        String longitudeString = c.getString(c.getColumnIndex(AppConst.LOG_LONGITUDE));
        String latitudeString = c.getString(c.getColumnIndex(AppConst.LOG_LATITUDE));
        double longitudeDouble = Double.valueOf(longitudeString);
        double latitudeDouble = Double.valueOf(latitudeString);
        longitude.setText(longitudeString);
        latitude.setText(latitudeString);


        String code = GeoIndexUtil.GeoCoordinatesToCodeV01Compress(longitudeDouble, latitudeDouble, 10);
        String colorCode = "#" + new StringBuilder(code.substring(4, 10)).reverse().toString();
        imageView.setBackgroundColor(Color.parseColor(colorCode));
        return(row);
    }

}
