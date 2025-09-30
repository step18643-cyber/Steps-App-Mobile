package com.example.stepcounter;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int REQ_ACTIVITY_PERMISSION = 1001;
    private TextView stepTextView, distanceTextView, timeTextView, stepCounterTargetTextView;
    private Button pauseButton;
    private ProgressBar progressBar;
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private float initialSensorValue = -1f;
    private int stepCount = 0;
    private boolean isPaused = false;
    private long startTime;
    private long pauseStart;
    private boolean justResumed = false;
    private int stepCountTarget = 5000;

    private Handler timerHandler = new Handler();
    private Handler syncHandler = new Handler();
    private Executor dbExecutor = Executors.newSingleThreadExecutor();

    private AppDatabase db;
    private StepApi api;

    private SharedPreferences prefs;
    private String lastDay;
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            String currentDay = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            if (!currentDay.equals(lastDay)) {
                stepCount = 0;
                justResumed = true;
                startTime = System.currentTimeMillis();
                lastDay = currentDay;
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("last_day", currentDay);
                editor.putInt("step_count", 0);
                editor.apply();
            }

            long mils = System.currentTimeMillis() - startTime;
            int sec = (int) (mils / 1000);
            int min = sec / 60;
            sec = sec % 60;
            timeTextView.setText(String.format(Locale.getDefault(), "Time: %02d:%02d", min, sec));
            timerHandler.postDelayed(this, 1000);
        }
    };

    private Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            dbExecutor.execute(() -> {
                List<StepEntity> unsynced = db.stepDao().getUnsyncedSteps();
                for (StepEntity se : unsynced) {
                    StepPayload payload = new StepPayload(se.userId, se.timestamp, se.steps);
                    api.postStep(payload).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                dbExecutor.execute(() -> db.stepDao().delete(se));
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                        }
                    });
                }
            });
            syncHandler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_main);

        stepTextView = findViewById(R.id.stepTextView);
        distanceTextView = findViewById(R.id.distanceTextView);
        timeTextView = findViewById(R.id.timeTextView);
        pauseButton = findViewById(R.id.pauseButton);
        progressBar = findViewById(R.id.progressBar);
        stepCounterTargetTextView = findViewById(R.id.stepCounterTargetTextView);

        startTime = System.currentTimeMillis();
        progressBar.setMax(stepCountTarget);
        stepCounterTargetTextView.setText("Step Goal: " + stepCountTarget);

        db = AppDatabase.getInstance(getApplicationContext());
        api = ApiClient.getApi("https://step-counter-app-backend-yc2i-fsx1nwv7b.vercel.app");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCounterSensor = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) : null;

        if (stepCounterSensor == null) {
            stepTextView.setText("Step counter not available");
        }
        prefs = getSharedPreferences("step_prefs", Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        lastDay = prefs.getString("last_day", "");
        if (!today.equals(lastDay)) {
            stepCount = 0;
            lastDay = today;
            startTime = System.currentTimeMillis();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("last_day", today);
            editor.putInt("step_count", 0);
            editor.apply();
        } else {
            stepCount = prefs.getInt("step_count", 0);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        REQ_ACTIVITY_PERMISSION);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isPaused) {
            if (stepCounterSensor != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                        == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
                }
            }
            timerHandler.post(timerRunnable);
            syncHandler.post(syncRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        timerHandler.removeCallbacks(timerRunnable);
        syncHandler.removeCallbacks(syncRunnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) return;

        float sensorValue = event.values[0];
        if (justResumed) {
            initialSensorValue = sensorValue - stepCount;
            justResumed = false;
        }

        if (initialSensorValue < 0) {
            initialSensorValue = sensorValue - stepCount;
        }

        int currentSteps = Math.max(0, (int) (sensorValue - initialSensorValue));
        this.stepCount = currentSteps;

        runOnUiThread(() -> {
            stepTextView.setText("Step Count: " + currentSteps);
            progressBar.setProgress(Math.min(currentSteps, progressBar.getMax()));
            if (currentSteps >= stepCountTarget) {
                stepCounterTargetTextView.setText("Step Goal " + stepCountTarget + " achieved!");
            }
            double distanceInKM = currentSteps * 0.782 / 1000.0;
            distanceTextView.setText(String.format(Locale.getDefault(), "Distance: %.2f km", distanceInKM));
        });

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("step_count", stepCount);
        editor.apply();

        final int saveSteps = currentSteps;
        dbExecutor.execute(() -> {
            StepEntity entity = new StepEntity(System.currentTimeMillis(), saveSteps, false, "user1");
            db.stepDao().insert(entity);
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void onPauseButtonClick(View view) {
        if (isPaused) {
            isPaused = false;
            pauseButton.setText("Pause");
            startTime += (System.currentTimeMillis() - pauseStart);
            timerHandler.post(timerRunnable);
            syncHandler.post(syncRunnable);
            justResumed = true;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        } else {
            isPaused = true;
            pauseButton.setText("Resume");
            pauseStart = System.currentTimeMillis();
            timerHandler.removeCallbacks(timerRunnable);
            syncHandler.removeCallbacks(syncRunnable);
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQ_ACTIVITY_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (stepCounterSensor != null && !isPaused) {
                    sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}