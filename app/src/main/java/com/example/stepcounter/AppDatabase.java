package com.example.stepcounter;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {StepEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract StepDao stepDao();

    private static volatile AppDatabase INSTANCE;
    public static AppDatabase getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(ctx.getApplicationContext(),
                            AppDatabase.class, "steps_db").build();
                }
            }
        }
        return INSTANCE;
    }
}
