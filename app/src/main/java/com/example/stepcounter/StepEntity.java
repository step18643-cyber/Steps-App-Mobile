package com.example.stepcounter;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "steps")
public class StepEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public long timestamp;
    public int steps;
    public boolean synced;
    public String userId;

    public StepEntity(long timestamp, int steps, boolean synced, String userId) {
        this.timestamp = timestamp;
        this.steps = steps;
        this.synced = synced;
        this.userId = userId;
    }
}
