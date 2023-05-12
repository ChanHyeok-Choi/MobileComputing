package com.example.assignment1;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class LocationService extends Service {
    int imageViewId;
    ImageView imageView;
    Uri uri;
    List<PointF> locationPoints = new ArrayList<>();
    List<List<ScanResult>> scanResults = new ArrayList<>();

    WifiManager wifiManager;
    Timer timer = new Timer();
    TimerTask timerTask = null;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                startTimerTask();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        uri = intent.getData().normalizeScheme();
        imageViewId = intent.getIntExtra("ImageView", 0);
        locationPoints = (List<PointF>) intent.getSerializableExtra("locationPoints");
        scanResults = (List<List<ScanResult>>) intent.getSerializableExtra("scanResults");

        imageView = imageView.findViewById(imageViewId);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);

        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        Log.d("LocationService", "running");

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        imageView = imageView.findViewById(imageViewId);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);

        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        Log.d("LocationService", "running");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTimerTask();
        unregisterReceiver(mReceiver);
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
        currentScanResult.sort((t1, t2) -> {
            if (t1.level < t2.level) {
                return 1;
            } else {
                return -1;
            }
        });
        for (int i = 0; i < 3; i++) {
            Log.d("Current AP", i + " " + currentScanResult.get(i).SSID + " " + currentScanResult.get(i).level);
        }

        int idx = 0;
        for (List<ScanResult> lsr : scanResults) {
            lsr.sort((t1, t2) -> {
                if (t1.level < t2.level) {
                    return 1;
                } else {
                    return -1;
                }
            });
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
        timerTask = new TimerTask() {
            @Override
            public void run() {
                wifiManager.startScan();
                List<ScanResult> scanResult = wifiManager.getScanResults();
                PointF cur = getLocationFromScanResult(scanResult);
                if (cur != null) {
                    drawPointOnImageView(cur);
                } else {
                    Toast.makeText(getApplicationContext(), "There's no matched AP location", Toast.LENGTH_SHORT).show();
                }
            }
        };

        timer.schedule(timerTask, 0, 2000);
    }

    private void stopTimerTask() {
        if(timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
}
