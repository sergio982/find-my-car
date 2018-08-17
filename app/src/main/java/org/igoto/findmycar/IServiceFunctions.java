package org.igoto.findmycar;

import android.app.Activity;

/**
 * Created by programer on 4.3.2016.
 */
public interface IServiceFunctions {
    void registerActivity(Activity activity, IListenerFunctions callback);

    void unregisterActivity(Activity activity);

    Boolean isRunning();
    void shutDownService();
    void saveLoaction();
    void updateSettings();
    void setOptimize(Boolean optimize);
}