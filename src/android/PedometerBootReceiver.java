package com.emesonsantana.cordova.pedometer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

public class PedometerBootReceiver extends BroadcastReceiver {
  private final String TAG = "PedometerBootReceiver";

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.i(TAG, "Receive boot!");

    PackageManager pm = context.getPackageManager();

    if (!this.hasStepFunctionality(pm)) {
      Intent stepCounterServiceIntent = new Intent(context, ForegroundService.class);
      stepCounterServiceIntent.setAction("start");
      if (Build.VERSION.SDK_INT >= 26) {
        context.startForegroundService(stepCounterServiceIntent);
      } else {
        context.startService(stepCounterServiceIntent);
      }
    }
  }

  public boolean hasStepFunctionality(PackageManager pm) {
    return pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)
      && pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_DETECTOR);
  }
}
