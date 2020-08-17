package com.sensorlib.sensor.facedetection;

public class FoundFace {
    byte[] jpegData;
    float[] vector;
    int trackingId;
    int faceId;

    FoundFace(byte[] jpegData, float[] vector, int trackingId, int faceId) {
        this.jpegData = jpegData;
        this.vector = vector;
        this.trackingId = trackingId;
        this.faceId = faceId;
    }
}
