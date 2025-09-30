package com.example.stepcounter;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface StepDao {
    @Insert
    long insert(StepEntity step);

    @Query("SELECT * FROM steps WHERE synced = 0 ORDER BY timestamp ASC")
    List<StepEntity> getUnsyncedSteps();

    @Query("UPDATE steps SET synced = 1 WHERE id = :id")
    void markSynced(int id);

    @Query("SELECT * FROM steps ORDER BY timestamp DESC LIMIT :limit")
    List<StepEntity> getLatest(int limit);

    @androidx.room.Delete
    void delete(StepEntity step);
}
