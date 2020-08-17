package com.sensorlib.sensor.facedetection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FoundPerson {
    Set<Integer> linkedFaces;
    Set<Integer> linkedTrackingIds;

    FoundPerson(int faceId, int trackingId) {
        linkedFaces = new HashSet<>();
        linkedTrackingIds = new HashSet<>();
        linkedFaces.add(faceId);
        linkedTrackingIds.add(trackingId);
    }

    public void merge(FoundPerson person) {
        linkedFaces.addAll(person.linkedFaces);
        linkedTrackingIds.addAll(person.linkedTrackingIds);
    }
}
