package com.sensor;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.sensor.common.CameraSource;
import com.sensor.facedetection.FaceDetectionProcessor;

public class ReactNativeSensorModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
//    private final CameraSource cameraSource;

    public ReactNativeSensorModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
//        cameraSource = new CameraSource(this.reactContext.getCurrentActivity(), overlay);
//        cameraSource.setMachineLearningFrameProcessor(
//                new FaceDetectionProcessor(this.reactContext.getResources(), this, this.reactContext)
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


}
