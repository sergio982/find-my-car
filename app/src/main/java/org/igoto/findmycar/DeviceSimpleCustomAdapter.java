package org.igoto.findmycar;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by programer on 1.5.2016.
 */
public class DeviceSimpleCustomAdapter extends ArrayAdapter<DeviceBean> {


    private ArrayList<DeviceBean> dbList;

    public DeviceSimpleCustomAdapter(Context context, int textViewResourceId,
                           ArrayList<DeviceBean> dbList) {
        super(context, textViewResourceId, dbList);
        this.dbList = new ArrayList<DeviceBean>();
        this.dbList.addAll(dbList);
    }

    private class ViewHolder {
        TextView code;
        CheckBox name;
        TextView type;
        TextView address;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder = null;
        Log.v("ConvertView", String.valueOf(position));

        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.device_info, null);

            holder = new ViewHolder();
            holder.code = (TextView) convertView.findViewById(R.id.textViewDeviceName);
            holder.address = (TextView) convertView.findViewById(R.id.textViewHardwareAddress);
            holder.type = (TextView) convertView.findViewById(R.id.textViewType);
            holder.name = (CheckBox) convertView.findViewById(R.id.checkBoxDevice);

            if (MainActivity.MULTI_MODE) {
                holder.name.setVisibility(View.VISIBLE);
                //deviceName.setText(c.getString(c.getColumnIndex(AppConst.LOG_DEVICE_NAME)));
            } else {
                holder.name.setVisibility(View.GONE);
            }

            convertView.setTag(holder);

            holder.name.setOnClickListener( new View.OnClickListener() {
                public void onClick(View v) {
                    CheckBox cb = (CheckBox) v ;
                    DeviceBean db = (DeviceBean) cb.getTag();
                    /*Toast.makeText(DeviceSimpleCustomAdapter.this.getContext(),
                            "Clicked on Checkbox: " + cb.getText() +
                                    " is " + cb.isChecked(),
                            Toast.LENGTH_LONG).show();*/
                    db.setSelected(cb.isChecked());
                }
            });
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        DeviceBean db = dbList.get(position);
        holder.code.setText(db.getName());
        holder.type.setText("(" + db.getDeviceType() + ")");
        holder.address.setText(db.getDeviceAddress());
        holder.name.setChecked(db.isSelected());
        holder.name.setTag(db);

        return convertView;

    }

}
