package com.example.stepcounter;

public class StepPayload {
    public String userId;
    public long timestamp;
    public int steps;

    public StepPayload(String userId, long timestamp, int steps) {
        this.userId = userId;
        this.timestamp = timestamp;
        this.steps = steps;
    }
}
