package org.igoto.findmycar.compass;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.igoto.findmycar.R;

public class CompassView extends RelativeLayout implements Observer {

    private ImageView needleImage;
    private ImageView compassImage;
    private float lastNeedleOrigin = 0;
    private float lastCompassOrigin = 0;
    private Compass compass;
    private Navigator navigator;

    public CompassView(Context context) {
        super(context);
        init();
    }
    
    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CompassView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.compass, this);

        needleImage = (ImageView) findViewById(R.id.needleImage);
        compassImage = (ImageView) findViewById(R.id.compassImage);
    }
    
    public void setCompass(Compass c) {
        compass = c;
        compass.addObserver(this);
    }
    
    public void setNavigator(Navigator n) {
        navigator = n;
        navigator.addObserver(this);
    }

    @Override
    public void onNotified() {
        updateNeedle();
    }

    private void updateNeedle() {
        float compassDirection = (float) compass.getCompassValue();

        float needleDirection = compassDirection + (360 - navigator.getBearing());

        if (compassDirection < 0) {
            compassDirection += 360;
        } else if (compassDirection > 360) {
            compassDirection -= 360;
        }

        if (needleDirection < 0) {
            needleDirection += 360;
        } else if (needleDirection > 360) {
            needleDirection -= 360;
        }

        // create roations
        RotateAnimation needleAnimation = new RotateAnimation(lastNeedleOrigin, -needleDirection,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        needleAnimation.setDuration(100);
        needleAnimation.setFillAfter(true);
        needleImage.startAnimation(needleAnimation);

        RotateAnimation compassAnimation = new RotateAnimation(lastCompassOrigin, -compassDirection,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        compassAnimation.setDuration(100);
        compassAnimation.setFillAfter(true);
        compassImage.startAnimation(compassAnimation);

        lastNeedleOrigin = -needleDirection;
        lastCompassOrigin = -compassDirection;
    }

}
