package com.sensorlib.sensor.facedetection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Clusters {
    private Set<Integer> clearedTrackingIds;
    private ArrayList<FoundFace> foundFaces;
    private static final float uncertaintyThreshold = 28.0f;
    private static final float certaintyThreshold = 25.0f;

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
            double vectorsDistance = distance512(foundFace.vector, vector);
            if (vectorsDistance < certaintyThreshold) {
                // The face is already known
                clearedTrackingIds.add(trackingId);
                return false;
            }
            if (vectorsDistance < uncertaintyThreshold) {
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
