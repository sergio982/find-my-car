package org.igoto.findmycar.compass;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class DistanceTextView extends TextView implements Observer {
    
    private Navigator navigator;

    public DistanceTextView(Context context) {
        super(context);
        init();
    }
    
    public DistanceTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DistanceTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        //setTypeface(Constants.TEXT_TYPEFACE);
    }
    
    public void setNavigator(Navigator n) {
        navigator = n;
        navigator.addObserver(this);
    }

    @Override
    public void onNotified() {
        updateDistance();
    }
    
    private void updateDistance() {
        String dist = String.format(Constants.LOCALE, "%.1f", navigator.getDistance());
        setText("Distance: " + dist + "m");
    }
    
}
