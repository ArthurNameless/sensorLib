package com.sensorlib.sensor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.IllegalViewOperationException;
import com.sensorlib.sensor.common.CameraSource;
import com.sensorlib.sensor.facedetection.FaceDetectionProcessor;
import com.facebook.react.bridge.Promise;

import java.io.IOException;

import javax.annotation.Nullable;

import static androidx.core.app.ActivityCompat.requestPermissions;

public class ReactNativeSensorModule extends ReactContextBaseJavaModule implements SensorDelegate {

    private final ReactApplicationContext reactContext;
    private final CameraSource cameraSource;
    private static final String FACES_DETECTED_EVENT = "FacesDetectedEvent";
    private String cacheDirectory = "";
    public final int REQUEST_CODE = 1001;


    public ReactNativeSensorModule(ReactApplicationContext reactContext) throws IOException {
        super(reactContext);
        this.reactContext = reactContext;
        cameraSource = new CameraSource(this.reactContext.getCurrentActivity());
        cameraSource.setMachineLearningFrameProcessor(
                new FaceDetectionProcessor(this.reactContext.getResources(), this, this.reactContext));
        if (ContextCompat.checkSelfPermission(
                this.reactContext, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            cameraSource.start();
        } else {
//            requestPermissions(this,
//                    new String[] { Manifest.permission.CAMERA },
//                    REQUEST_CODE);
        }

        Log.d("onCameraStart", "camera=: " + cameraSource);
    }


    @Override
    public String getName() {
        return "ReactNativeSensor";
    }

    @ReactMethod
    public void sampleMethod(String stringArgument, int numberArgument, Callback callback) {
        // TODO: Implement some actually useful functionality
        callback.invoke("Received numberArgument: " + numberArgument + " stringArgument: " + stringArgument);
    }

    @ReactMethod
    public void getText(
            String text,
            Promise promise) {
        try {
            promise.resolve(text);
        } catch (IllegalViewOperationException e) {
            promise.reject(e);
        }
    }

//
//    @ReactMethod
//    public void onFacesDetected(WritableArray faceData, Promise promise) {
//        try {
//            promise.resolve(faceData);
//        } catch (IllegalViewOperationException e) {
//            promise.reject(e);
//        }
//    }

    @ReactMethod
    public void init(String cacheDirectory) {
        Log.d("onInitTAG", "dir: " + cacheDirectory);
        setCacheDirectory(cacheDirectory);
    }

    private void sendEvent(String eventName,
                           @Nullable WritableArray params) {
        this.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @Override
    public void onFacesDetected(WritableArray faceData) {
        sendEvent(FACES_DETECTED_EVENT, faceData);
        Log.d("onFaceTAG", "onFacesDetected: ");
    }

    @Override
    public void setCacheDirectory(String dir) {
        cacheDirectory = dir;
    }

    @Override
    public void setAuthToken(String authToken) {

    }

    @Override
    public String getCacheDirectory() {
        return cacheDirectory;
    }

    @Override
    public String getAuthToken() {
        return null;
    }
}

