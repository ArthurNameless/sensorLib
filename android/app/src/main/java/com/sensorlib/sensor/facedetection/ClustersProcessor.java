package com.sensorlib.sensor.facedetection;

import java.util.ArrayList;

public class ClustersProcessor {
    ArrayList<FoundFace> foundFaces;
    ArrayList<FoundPerson> foundPeople;
    float distanceThreshold = 24.0f;

    ClustersProcessor() {

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

    public void recordFace(byte[] jpegData, float[] vector, int trackingId) {
        FoundFace newFace = new FoundFace(jpegData, vector, trackingId, foundFaces.size());
        foundFaces.add(newFace);
        FoundPerson assignedPerson = null;
        // Check if Person is already known
        for (FoundPerson foundPerson : foundPeople) {
            if (foundPerson == null) {
                continue;
            }
            if (foundPerson.linkedTrackingIds.contains(trackingId)) {
                assignedPerson = foundPerson;
                break;
            }
        }
        // Create a person if one is unknown
        if (assignedPerson == null) {
            assignedPerson = new FoundPerson(newFace.faceId, trackingId);
        }
        // Search if this face matches any previously known people
        boolean merged = false;
        for (FoundPerson foundPerson : foundPeople) {
            if (foundPerson == null) {
                continue;
            }
            for (int faceId : foundPerson.linkedFaces) {
                FoundFace foundFace = foundFaces.get(faceId);
                if (foundFace == null) {
                    continue;
                }
                if (distance512(newFace.vector, foundFace.vector) < distanceThreshold) {
                    // it is the same person
                    foundPerson.merge(assignedPerson);
                    merged = true;
                    break;
                }
            }
            if (merged) {
                break;
            }
        }
    }

    public FoundFace getFace(int faceId) {
        return foundFaces.get(faceId);
    }
}
