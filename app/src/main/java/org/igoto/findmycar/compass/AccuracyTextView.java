package org.igoto.findmycar.compass;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class AccuracyTextView extends TextView implements Observer {
    
    private Navigator navigator;

    public AccuracyTextView(Context context) {
        super(context);
        init();
    }
    
    public AccuracyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AccuracyTextView(Context context, AttributeSet attrs, int defStyle) {
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
        updateAccuracy();
    }
    
    private void updateAccuracy() {
        String dist = String.format(Constants.LOCALE, "%.1f", navigator.getAccuracy());
        setText("Accuracy: " + dist + "m");
    }
    
}
