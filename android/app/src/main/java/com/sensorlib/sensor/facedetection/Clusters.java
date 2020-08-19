package com.sensorlib.sensor.facedetection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Clusters {
    private Set<Integer> clearedTrackingIds;
    private ArrayList<FoundFace> foundFaces;
    private static final float distanceThreshold = 29.0f;

    Clusters() {
        clearedTrackingIds = new HashSet<>();
        foundFaces = new ArrayList<>();
    }

    private double distance(float[] a, float[] b, int size) {
        double sum = 0;
        for (int i = 0; i < size; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }

    private double distance512(float[] a, float[] b) {
        return distance(a, b, 512);
    }

    public boolean recordFace(byte[] jpegData, float[] vector, int trackingId) {
        if (clearedTrackingIds.contains(trackingId)) {
            return false;
        }
        // Check is the face already known
        for (FoundFace foundFace : foundFaces) {
            if (distance512(foundFace.vector, vector) < distanceThreshold) {
                // The face is already known
                clearedTrackingIds.add(trackingId);
                return false;
            }
        }
        FoundFace newFace = new FoundFace(null, vector, trackingId, foundFaces.size());
        foundFaces.add(newFace);
        return true;
    }

    public void clear() {
        clearedTrackingIds.clear();
        foundFaces.clear();
    }
}
