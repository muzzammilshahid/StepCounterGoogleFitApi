package com.deskconn.stepcountgooglefitapi;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.deskconn.stepcountgooglefitapi.databinding.ActivityMainBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.SensorRequest;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OAUTH_REQUEST_CODE = 99;
    private static final int REQUEST_PERMISSIONS = 1;
    FitnessOptions fitnessOptions;
    ActivityMainBinding binding;
    long steps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fitnessOptions =
                FitnessOptions.builder()
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                        .build();
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSIONS);
        } else {
            if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
                GoogleSignIn.requestPermissions(
                        this,
                        REQUEST_OAUTH_REQUEST_CODE,
                        GoogleSignIn.getLastSignedInAccount(this),
                        fitnessOptions);
            } else {
                subscribe();
            }

            Fitness.getSensorsClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                    .add(new SensorRequest.Builder()
                                    .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                                    .setMaxDeliveryLatency(30, TimeUnit.MINUTES)
                                    .setFastestRate(1, TimeUnit.SECONDS)
                                    .setSamplingRate(1, TimeUnit.SECONDS)
                                    .build(),
                            dataPoint -> {
                                steps = steps + dataPoint.getValue(Field.FIELD_STEPS).asInt();
                                binding.textview.setText(String.valueOf(steps));
                                Toast.makeText(MainActivity.this, "here is " + dataPoint.getValue(Field.FIELD_STEPS), Toast.LENGTH_SHORT).show();
                            }).addOnSuccessListener(unused -> System.out.println("here success" + unused));

        }
    }

    public void subscribe() {
        Fitness.getRecordingClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                // This example shows subscribing to a DataType, across all possible
                // data sources. Alternatively, a specific DataSource can be used.
                .subscribe(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(unused -> {
                    Log.i(TAG, "Successfully subscribed!");
                    readData();
                })
                .addOnFailureListener(e ->
                        Log.w(TAG, "There was a problem subscribing.", e));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                subscribe();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
                GoogleSignIn.requestPermissions(
                        this,
                        REQUEST_OAUTH_REQUEST_CODE,
                        GoogleSignIn.getLastSignedInAccount(this),
                        fitnessOptions);
            }
        }
    }

    private void readData() {
        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(
                        dataSet -> {
                            long total =
                                    dataSet.isEmpty()
                                            ? 0
                                            : dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                            Log.i(TAG, "Total steps: " + total);
                            steps = total;
                            binding.textview.setText(String.valueOf(steps));
                            Toast.makeText(this, "Total steps = " + total, Toast.LENGTH_SHORT).show();
                        })
                .addOnFailureListener(
                        e -> Log.w(TAG, "There was a problem getting the step count.", e));
    }
}