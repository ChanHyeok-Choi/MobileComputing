package com.example.assignment1;


import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    Button upload_button;
    Button delete_button;
    ToggleButton warDriving;
    ToggleButton localization;

    Uri uri;
    List<PointF> locationPoints = new ArrayList<>();
    List<List<ScanResult>> scanResults = new ArrayList<>();

    String[] scannedDialog = {"Add new WiFi data", "Delete data", "See the data", "Cancel"};

    WifiManager wifiManager;
    Timer timer = new Timer();
    TimerTask timerTask = null;
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Log.e("Scan", "Scan results available");
                startTimerTask();
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        upload_button = findViewById(R.id.uploadMap);
        delete_button = findViewById(R.id.deleteMap);
        warDriving = findViewById(R.id.warDriving);
        localization = findViewById(R.id.localization);

        upload_button.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 1);
        });

        delete_button.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            startActivityForResult(intent, 4);
        });

        warDriving.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                Toast.makeText(getApplicationContext(), "WarDriving mode ON", Toast.LENGTH_SHORT).show();
                if (locationPoints.size() > 1) {
                    drawPointsOnImageView(locationPoints);
                }
                imageView.setOnTouchListener((view, motionEvent) -> {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        int x = (int) motionEvent.getX();
                        int y = (int) motionEvent.getY();
                        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                        int scaledX = (int) (x * bitmap.getWidth() / imageView.getWidth());
                        int scaledY = (int) (y * bitmap.getHeight() / imageView.getHeight());
                        for (PointF point : locationPoints) {
                            float dx = scaledX - point.x;
                            float dy = scaledY - point.y;
//                            Log.d("Notice", String.valueOf(dx) + ", " + String.valueOf(dy));
                            if (Math.abs(dx) < 20 && Math.abs(dy) < 20) {
                                int idx = locationPoints.indexOf(point);
                                // Show dialog box
                                AlertDialog.Builder savedBuilder = new AlertDialog.Builder(MainActivity.this);
                                savedBuilder.setTitle("What would like to do for this location?");
                                savedBuilder.setItems(scannedDialog, (dialogInterface, i) -> {
                                    switch (i) {
                                        case 0:
                                            Intent newScanIntent = new Intent(MainActivity.this, ScanActivity.class);
                                            newScanIntent.putExtra("Index", idx);
                                            startActivityForResult(newScanIntent, 3);
                                            break;
                                        case 1:
                                            locationPoints.remove(idx);
                                            scanResults.remove(idx);
                                            drawPointsOnImageView(locationPoints);
                                            break;
                                        case 2:
                                            Intent seeIntent = new Intent(MainActivity.this, SeeActivity.class);
                                            seeIntent.putExtra("ScanResults", (Serializable) scanResults.get(idx));
                                            startActivity(seeIntent);
                                            break;
                                        case 3:
                                            break;
                                    }
                                });

                                AlertDialog dialog = savedBuilder.create();
                                dialog.show();

                                return true;
                            }
                        }
                        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(mutableBitmap);

                        PointF pointF = new PointF(scaledX, scaledY);
                        Paint paint = new Paint();
                        paint.setColor(Color.BLUE);
                        paint.setStyle(Paint.Style.FILL);
                        float radius = 5f;
                        canvas.drawCircle(scaledX, scaledY, radius, paint);
                        imageView.setImageBitmap(mutableBitmap);

                        // Show dialog box
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);

                        builder.setTitle("What would like to scan?");

                        builder.setPositiveButton("OK", (dialog, which) -> {
                            locationPoints.add(pointF);
                            Intent intent = new Intent(this, ScanActivity.class);
                            startActivityForResult(intent, 2);
                        });

                        builder.setNegativeButton("Cancel", (dialog, which) -> {
                            drawPointsOnImageView(locationPoints);
                            dialog.cancel();
                        });

                        AlertDialog dialog = builder.create();
                        dialog.show();

                        return true;
                    }
                    return false;
                });
            } else {
                imageView.setEnabled(false);
                imageView.setClickable(false);
                Toast.makeText(getApplicationContext(), "WarDriving mode OFF", Toast.LENGTH_SHORT).show();
                imageView.setImageURI(uri);
            }
        });

        localization.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                if (scanResults.size() < 1) {
                    Toast.makeText(getApplicationContext(), "There's no WiFi APs!", Toast.LENGTH_SHORT).show();
                    localization.setChecked(false);
                    return;
                }
                Toast.makeText(getApplicationContext(), "Localization mode ON", Toast.LENGTH_SHORT).show();
                wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

                IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
                registerReceiver(mReceiver, filter);

                startTimerTask();
//                Intent intent = new Intent(this, LocationService.class);
//                intent.putExtra("Uri", uri);
//                intent.putExtra("ImageView", R.id.imageView);
//                intent.putExtra("locationPoints", (Serializable) locationPoints);
//                intent.putExtra("scanResults", (Serializable) scanResults);
            } else {
                stopTimerTask();
                unregisterReceiver(mReceiver);
//                stopService(new Intent(this, LocationService.class));
                imageView.setImageURI(uri);
                Toast.makeText(getApplicationContext(), "Localization mode OFF", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                uri = data.getData();
                imageView.setImageURI(uri);
                Log.d("Uri", String.valueOf(uri));
                upload_button.setVisibility(View.GONE);
                delete_button.setVisibility(View.VISIBLE);
            }
        } else if (requestCode == 2) {
            if (resultCode == RESULT_OK && data != null) {
                List<ScanResult> mScanResults = (List<ScanResult>) data.getSerializableExtra("scanResults");
                scanResults.add(mScanResults);
                drawPointsOnImageView(locationPoints);
            } else {
                if (locationPoints.size() > 1) {
                    locationPoints.remove(locationPoints.size() - 1);
                    drawPointsOnImageView(locationPoints);
                } else {
                    locationPoints.remove(0);
                    imageView.setImageURI(uri);
                }
            }
        } else if (requestCode == 3) {
            List<ScanResult> mScanResults = (List<ScanResult>) data.getSerializableExtra("scanResults");
            int idx = data.getIntExtra("Index", -1);
            scanResults.set(idx, mScanResults);
            drawPointsOnImageView(locationPoints);
        } else {
            imageView.setImageResource(R.drawable.ic_launcher_foreground);
            delete_button.setVisibility(View.GONE);
            upload_button.setVisibility(View.VISIBLE);
        }
    }

    private void checkLocationPermission() {
        int permissionCheck1 = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION);
        int permissionCheck2 = ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION);

        if (permissionCheck1 == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, 0);
        }
        if (permissionCheck2 == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_COARSE_LOCATION}, 0);
        }
    }

    private void drawPointsOnImageView(List<PointF> locationPoints) {
        imageView.setImageURI(uri);
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        float radius = 5f;
        for (PointF point : locationPoints) {
            canvas.drawCircle(point.x, point.y, radius, paint);
        }

        imageView.setImageBitmap(mutableBitmap);
    }

    private void drawPointOnImageView(PointF cur) {
        imageView.setImageURI(uri);
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        float radius = 5f;
        canvas.drawCircle(cur.x, cur.y, radius, paint);

        imageView.setImageBitmap(mutableBitmap);
    }

    private PointF getLocationFromScanResult(List<ScanResult> currentScanResult) {
        if (scanResults.isEmpty()) {
            return null;
        }
        Collections.sort(currentScanResult, (t1, t2) -> t2.level - t1.level);
        for (int i = 0; i < 3; i++) {
            Log.d("Current AP", i + " " + currentScanResult.get(i).SSID + " " + currentScanResult.get(i).level);
        }

        int idx = 0;
        for (List<ScanResult> lsr : scanResults) {
            Collections.sort(lsr, (t1, t2) -> t2.level - t1.level);
            int count = 0;
            for (int i = 0; i < 3; i++) {
                if (Math.abs(currentScanResult.get(i).level - lsr.get(i).level) < 1) {
                    if (Objects.equals(currentScanResult.get(i).BSSID, lsr.get(i).BSSID)) {
                        count++;
                    }
                }
            }
            if (count == 3) {
                return locationPoints.get(idx);
            }
            idx++;
        }
        return null;
    }

    private void startTimerTask() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }

        timerTask = new TimerTask() {
            @Override
            public void run() {
                boolean scanStarted = wifiManager.startScan();
                if(!scanStarted) Log.e("Error", "Wifi scan failed...");
                List<ScanResult> scanResult = wifiManager.getScanResults();
                PointF cur = getLocationFromScanResult(scanResult);
                if (cur != null) {
                    runOnUiThread(() -> drawPointOnImageView(cur));
                } else {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "There's no matched AP location", Toast.LENGTH_SHORT).show());
                }
            }
        };

        timer.schedule(timerTask, 1000, 5000);
    }

    private void stopTimerTask() {
        if(timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
}
