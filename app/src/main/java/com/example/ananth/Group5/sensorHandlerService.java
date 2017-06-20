package com.example.ananth.Group5;

/**
 * Created by ananth on 6/17/17.
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;

public class sensorHandlerService extends Service implements SensorEventListener{

    private SensorManager accelManage;
    private Sensor senseAccel;
    float accelValuesX[] = new float[128];
    float accelValuesY[] = new float[128];
    float accelValuesZ[] = new float[128];
    int index = 0;
    int k=0;
    Bundle b;
    static int SAMPLING_FREQUENCE_TIME_INTERVAL = 1000;
    long latestTime = System.currentTimeMillis();

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // TODO Auto-generated method stub
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if ((System.currentTimeMillis() - latestTime) > SAMPLING_FREQUENCE_TIME_INTERVAL) {
                latestTime = System.currentTimeMillis();

                if(index >= 127) {
                    Arrays.fill(accelValuesX, 0);
                    Arrays.fill(accelValuesY, 0);
                    Arrays.fill(accelValuesZ, 0);
                    index = 0;
                    accelManage.unregisterListener(this);
                    accelManage.registerListener(this, senseAccel, SensorManager.SENSOR_DELAY_NORMAL);
                }
                index++;
                accelValuesX[index] = sensorEvent.values[0];
                accelValuesY[index] = sensorEvent.values[1];
                accelValuesZ[index] = sensorEvent.values[2];
                sendBroadcastIntent(accelValuesX[index], accelValuesY[index], accelValuesZ[index]);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCreate(){
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        accelManage = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senseAccel = accelManage.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelManage.registerListener(this, senseAccel, SensorManager.SENSOR_DELAY_NORMAL);

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        //k = 0;
        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub

        return null;
    }

    public void sendBroadcastIntent(float xcoord, float ycoord, float zcoord) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(HealthView.mBroadcastAccData);
        broadcastIntent.putExtra("xcoord", xcoord);
        broadcastIntent.putExtra("ycoord", ycoord);
        broadcastIntent.putExtra("zcoord", zcoord);
        sendBroadcast(broadcastIntent);
    }

}
