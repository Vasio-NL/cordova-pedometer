/**
 * Pedometer bridge with Cordova, programmed by Dario Salvi </dariosalvi78@gmail.com>; Edited by Emeson Santana </emesonsantana@gmail.com>
 * Based on the accelerometer plugin: https://github.com/apache/cordova-plugin-device-motion
 * License: MIT
 */
package com.emesonsantana.cordova.pedometer;

import java.util.List;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Handler;
import android.os.Bundle;

/**
 * This class listens to the pedometer sensor
 */
public class PedometerListener extends CordovaPlugin implements SensorEventListener, StepListener {

    public static int STOPPED = 0;
    public static int STARTING = 1;
    public static int RUNNING = 2;
    public static int ERROR_FAILED_TO_START = 3;
    public static int ERROR_NO_SENSOR_FOUND = 4;
    public static float STEP_IN_METERS = 0.762f;

    private int status;     // status of listener
    private float numSteps; // number of the steps
    private float startNumSteps; //first value, to be substracted in step counter sensor type
    private long startAt; //time stamp of when the measurement starts

    private SensorManager sensorManager; // Sensor manager
    private Sensor mSensor;             // Pedometer sensor returned by sensor manager
    private StepDetector stepDetector;

    private CallbackContext callbackContext; // Keeps track of the JS callback context.

    private Handler mainHandler=null;

    /**
     * Constructor
     */
    public PedometerListener() {
        this.startAt = 0;
        this.numSteps = 0;
        this.startNumSteps = 0;
        this.setStatus(PedometerListener.STOPPED);
        this.stepDetector = new StepDetector();
        this.stepDetector.registerListener(this);
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova the context of the main Activity.
     * @param webView the associated CordovaWebView.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.sensorManager = (SensorManager) cordova.getActivity().getSystemService(Context.SENSOR_SERVICE);
    }

    /**
     * Executes the request.
     *
     * @param action the action to execute.
     * @param args the exec() arguments.
     * @param callbackContext the callback context used when calling back into JavaScript.
     * @return whether the action was valid.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;

        if (action.equals("isStepCountingAvailable")) {
            Sensor stepCounter = this.sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            Sensor accel = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accel != null || stepCounter != null) {
                this.win(true);
                return true;
            } else {
                this.setStatus(PedometerListener.ERROR_NO_SENSOR_FOUND);
                this.win(false);
                return true;
            }
        } else if (action.equals("isDistanceAvailable")) {
            //distance is never available in Android
            this.win(false);
            return true;
        } else if (action.equals("isFloorCountingAvailable")) {
            //floor counting is never available in Android
            this.win(false);
            return true;
        }
        else if (action.equals("startPedometerUpdates")) {
            if (this.status != PedometerListener.RUNNING) {
                // If not running, then this is an async call, so don't worry about waiting
                // We drop the callback onto our stack, call start, and let start and the sensor callback fire off the callback down the road
                this.start();
            }
            PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT, "");
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        }
        else if (action.equals("stopPedometerUpdates")) {
            if (this.status == PedometerListener.RUNNING) {
                this.stop();
            }
            this.win(null);
            return true;
        } else {
            // Unsupported action
            return false;
        }
    }

    /**
     * Called by the Broker when listener is to be shut down.
     * Stop listener.
     */
    public void onDestroy() {
        this.stop();
    }


    /**
     * Start listening for pedometers sensor.
     */
    private void start() {
        // If already starting or running, then return
        if ((this.status == PedometerListener.RUNNING) || (this.status == PedometerListener.STARTING)) {
            return;
        }

        this.startAt = System.currentTimeMillis();
        this.numSteps = 0;
        this.startNumSteps = 0;
        this.setStatus(PedometerListener.STARTING);

        // Get pedometer or accelerometer from sensor manager
        this.mSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if(this.mSensor == null) this.mSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // If found, then register as listener
        if (this.mSensor != null) {
            int sensorDelay = this.mSensor.getType() == Sensor.TYPE_STEP_COUNTER ? SensorManager.SENSOR_DELAY_UI : SensorManager.SENSOR_DELAY_FASTEST;
            if (this.sensorManager.registerListener(this, this.mSensor, sensorDelay)) {
                this.setStatus(PedometerListener.STARTING);
            } else {
                this.setStatus(PedometerListener.ERROR_FAILED_TO_START);
                this.fail(PedometerListener.ERROR_FAILED_TO_START, "Device sensor returned an error.");
                return;
            };
        } else {
            this.setStatus(PedometerListener.ERROR_FAILED_TO_START);
            this.fail(PedometerListener.ERROR_FAILED_TO_START, "No sensors found to register step counter listening to.");
            return;
        }
    }

    /**
     * Stop listening to sensor.
     */
    private void stop() {
        if (this.status != PedometerListener.STOPPED) {
            this.sensorManager.unregisterListener(this);
        }
        this.setStatus(PedometerListener.STOPPED);
    }

    /**
     * Called when the accuracy of the sensor has changed.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
      //nothing to do here
      return;
    }

    /**
     * Sensor listener event.
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Only look at step counter or accelerometer events
        if (event.sensor.getType() != this.mSensor.getType()) {
            return;
        }

        // If not running, then just return
        if (this.status == PedometerListener.STOPPED) {
            return;
        }
        this.setStatus(PedometerListener.RUNNING);

        if(this.mSensor.getType() == Sensor.TYPE_STEP_COUNTER){
            float steps = event.values[0];

            if(this.startNumSteps == 0)
              this.startNumSteps = steps;

            this.numSteps = steps - this.startNumSteps;

            this.win(this.getStepsJSON());

        }else if(this.mSensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            stepDetector.updateAccel(
                event.timestamp, event.values[0], event.values[1], event.values[2]);
            
        }
    }

    @Override
    public void step(long timeNs) {
        this.numSteps++;
        this.win(this.getStepsJSON());
    }

    /**
     * Called when the view navigates.
     */
    @Override
    public void onReset() {
        if (this.status == PedometerListener.RUNNING) {
            this.stop();
        }
    }

    // Sends an error back to JS
    private void fail(int code, String message) {
        // Error object
        JSONObject errorObj = new JSONObject();
        try {
            errorObj.put("code", code);
            errorObj.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PluginResult err = new PluginResult(PluginResult.Status.ERROR, errorObj);
        err.setKeepCallback(true);
        callbackContext.sendPluginResult(err);
    }

    private void win(JSONObject message) {
        // Success return object
        PluginResult result;
        if(message != null)
            result = new PluginResult(PluginResult.Status.OK, message);
        else
            result = new PluginResult(PluginResult.Status.OK);

        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }

    private void win(boolean success) {
        // Success return object
        PluginResult result;
        result = new PluginResult(PluginResult.Status.OK, success);

        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    }

    private void setStatus(int status) {
        this.status = status;
    }

    private JSONObject getStepsJSON() {
        JSONObject r = new JSONObject();
        // pedometerData.startDate; -> ms since 1970
        // pedometerData.endDate; -> ms since 1970
        // pedometerData.numberOfSteps;
        // pedometerData.distance;
        // pedometerData.floorsAscended;
        // pedometerData.floorsDescended;
        try {
            r.put("startDate", this.startAt);
            r.put("endDate", System.currentTimeMillis());
            r.put("numberOfSteps", this.numSteps);
            r.put("distance", this.numSteps * PedometerListener.STEP_IN_METERS);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return r;
    }
}
