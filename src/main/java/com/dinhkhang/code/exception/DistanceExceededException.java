package com.dinhkhang.code.exception;

public class DistanceExceededException extends AttendanceException {
    private final double distance;
    private final int maxDistance;

    public DistanceExceededException(double distance, int maxDistance) {
        super(String.format("Khoảng cách %.2fm vượt quá giới hạn %dm", distance, maxDistance));
        this.distance = distance;
        this.maxDistance = maxDistance;
    }

    public double getDistance() {
        return distance;
    }

    public int getMaxDistance() {
        return maxDistance;
    }
}

