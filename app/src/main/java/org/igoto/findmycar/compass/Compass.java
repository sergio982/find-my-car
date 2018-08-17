package org.igoto.findmycar.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.LinkedList;
import java.util.List;

public class Compass implements SensorEventListener {

    private float[] valuesAccelerometer;
    private float[] valuesMagneticField;
    private float[] matrixR;
    private float[] matrixI;
    private float[] matrixValues;
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorMagneticField;
    private List<Observer> actionListeners = new LinkedList<Observer>();
    private double currentValue = 0f;

    public Compass(Context c) {
        sensorManager = (SensorManager) c.getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        valuesAccelerometer = new float[3];
        valuesMagneticField = new float[3];

        matrixR = new float[9];
        matrixI = new float[9];
        matrixValues = new float[3];
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
        case Sensor.TYPE_ACCELEROMETER:
            for (int i = 0; i < 3; i++) {
                valuesAccelerometer[i] = event.values[i];
            }
            break;
        case Sensor.TYPE_MAGNETIC_FIELD:
            for (int i = 0; i < 3; i++) {
                valuesMagneticField[i] = event.values[i];
            }
            break;
        }

        boolean success = SensorManager.getRotationMatrix(matrixR, matrixI, valuesAccelerometer, valuesMagneticField);

        if (success) {
            SensorManager.getOrientation(matrixR, matrixValues);

            double azimuth = Math.toDegrees(matrixValues[0]);
            // double pitch = Math.toDegrees(matrixValues[1]);
            // double roll = Math.toDegrees(matrixValues[2]);

            currentValue = azimuth;
            notifyListeners();
        }

    }

    private void notifyListeners() {
        for (Observer l : actionListeners) {
            l.onNotified();
        }
    }

    public void registerListener(int delay) {
        sensorManager.registerListener(this, sensorAccelerometer, delay);
        sensorManager.registerListener(this, sensorMagneticField, delay);

    }

    public void unregisterListener() {
        sensorManager.unregisterListener(this, sensorAccelerometer);
        sensorManager.unregisterListener(this, sensorMagneticField);
    }

    public void addObserver(Observer al) {
        actionListeners.add(al);
    }

    public double getCompassValue() {
        return currentValue;
    }
}