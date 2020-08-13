package com.sensor;

import com.facebook.react.bridge.WritableArray;

public interface SensorDelegate {

    void onFacesDetected(WritableArray faceData);

    void setCacheDirectory(String dir);

    void setAuthToken(String authToken);

    String getCacheDirectory();

    String getAuthToken();
}
