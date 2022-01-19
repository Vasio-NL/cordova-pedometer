package com.emesonsantana.cordova.pedometer;

import android.content.Intent;
import android.content.Context;
import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.util.Log;

import com.vasio.iamokay.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ForegroundService extends Service implements SensorEventListener, StepListener {
  private final  String TAG        = "ForegroundService";
  public static final String HISTORY_PREF      = "accelerometerStepsData";

  private SensorManager sensorManager; // Sensor manager
  private Sensor mSensor;             // Pedometer sensor returned by sensor manager
  private StepDetector stepDetector;

  public ForegroundService() {
    Log.i(TAG, "Construct foreground");

    this.stepDetector = new StepDetector();
    this.stepDetector.registerListener(this);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(TAG, "onStartCommand");
    Log.i(TAG, intent.getAction());

    if (intent.getAction().equals("start")) {
      // Start the service
      startPluginForegroundService(intent.getExtras());
      doInit();
    } else {
      // Stop the service
      stopForeground(true);
      stopSelf();
    }

    return START_STICKY;
  }

  @TargetApi(26)
  private void startPluginForegroundService(Bundle extras) {
    Log.i(TAG, "startPluginForegroundService");

    Context context = getApplicationContext();

    // Delete notification channel if it already exists
    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    try {
      manager.deleteNotificationChannel("foreground.service.channel");
    } catch(Exception e) {
      Log.i(TAG, "Attempt to delete notification but failed");
    }

    // Get notification channel importance
    Integer importance = 1;
    switch(importance) {
      case 2:
        importance = NotificationManager.IMPORTANCE_DEFAULT;
        break;
      case 3:
        importance = NotificationManager.IMPORTANCE_HIGH;
        break;
      default:
        importance = NotificationManager.IMPORTANCE_LOW;
        // We are not using IMPORTANCE_MIN because we want the notification to be visible
    }

    // Create notification channel
    NotificationChannel channel = new NotificationChannel("foreground.service.channel", "Background Services", importance);
    channel.setDescription("Enables background processing.");
    getSystemService(NotificationManager.class).createNotificationChannel(channel);

    // Make notification
    Notification notification = new Notification.Builder(context, "foreground.service.channel")
      .setContentTitle("I AM OKAY") // TODO: Make configurable
      .setContentText("Tracking steps in the background") // TODO: Make configurable
      .setSmallIcon(android.R.drawable.ic_media_play)
      .setOngoing(true)
      .build();

    // Get notification ID
    Integer id = 0;

    Log.i(TAG, "startPluginForegroundService2");

    // Put service in foreground and show notification (id of 0 is not allowed)
    startForeground(id != 0 ? id : 197812504, notification);
  }

  public void doInit() {
    Log.i(TAG, "Registering TYPE_ACCELEROMETER sensor");

    this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    this.mSensor    = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    this.sensorManager.registerListener(this, this.mSensor, SensorManager.SENSOR_DELAY_FASTEST);
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.i(TAG, "Not yet implemented");

    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    // Only look at step counter or accelerometer events
    if (event.sensor.getType() != this.mSensor.getType()) {
      return;
    }

    stepDetector.updateAccel(
      event.timestamp, event.values[0], event.values[1], event.values[2]);
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {

  }

  @Override
  public void step(long timeNs) {
    Integer daySteps = 0;

    Date currentDate = new Date();
    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    String currentDateString = dateFormatter.format(currentDate);
    SharedPreferences sharedPref = getSharedPreferences(PedometerListener.USER_DATA_PREF, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPref.edit();

    JSONObject pData = new JSONObject();
    JSONObject dayData = new JSONObject();
    if(sharedPref.contains(ForegroundService.HISTORY_PREF)){
      String pDataString = sharedPref.getString(ForegroundService.HISTORY_PREF,"{}");
      try{
        pData = new JSONObject(pDataString);
        Log.d(TAG," got json shared prefs "+pData.toString());
      }catch (JSONException err){
        Log.d(TAG," Exception while parsing json string : "+pDataString);
      }
    }

    //Get the datas previously stored for today
    if(pData.has(currentDateString)){
      try {
        dayData = pData.getJSONObject(currentDateString);
        daySteps = dayData.getInt("steps");
      }catch(JSONException err){
        Log.e(TAG,"Exception while getting Object from JSON for "+currentDateString);
      }
    }
    Log.i(TAG, "** daySteps :"+ daySteps);


    //Save calculated values to SharedPreferences
    try{
      dayData.put("steps",daySteps + 1);
      pData.put(currentDateString,dayData);
    }catch (JSONException err){
      Log.e(TAG,"Exception while setting int in JSON for "+currentDateString);
    }
    editor.putString(ForegroundService.HISTORY_PREF,pData.toString());
    editor.commit();
  }
}
