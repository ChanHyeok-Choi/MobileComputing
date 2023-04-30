package com.example.assignment1;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_WIFI_STATE;
import static android.Manifest.permission.CHANGE_WIFI_STATE;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.Serializable;
import java.util.List;

public class ScanActivity extends AppCompatActivity {
    Button Yes;
    Button No;
    WifiManager mWifiManager;

    String mApStr;

    IntentFilter mIntentFilter;
    List<ScanResult> scanResults;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action))
                Log.e("Scan", "Scan results available");
                scanResults = mWifiManager.getScanResults();
                mApStr = "";
                for (ScanResult result : scanResults)
                {
                    mApStr = mApStr + result.SSID + "; ";
                    mApStr = mApStr + result.BSSID + "; ";
                    mApStr = mApStr + result.capabilities + "; ";
                    mApStr = mApStr + result.frequency + " MHz;";
                    mApStr = mApStr + result.level + " dBm\n\n";
                }
                setTextView(mApStr);
        }
    };

    private void setTextView(String str) {
        TextView tv = findViewById(R.id.textScanView);
        tv.setMovementMethod(new ScrollingMovementMethod());
        tv.setText(str);
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("permission","checkSelfPermission");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_COARSE_LOCATION)) {
                Log.d("permission","shouldShowRequestPermissionRationale");
            } else {
                Log.d("permission","Request Permission!");
                ActivityCompat.requestPermissions(this,
                        new String[]{ACCESS_COARSE_LOCATION,
                                ACCESS_FINE_LOCATION,
                                ACCESS_WIFI_STATE,
                                CHANGE_WIFI_STATE}, 1000);

            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        checkPermission();

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean scanStarted = mWifiManager.startScan();
        if(!scanStarted) Log.e("Error", "Wifi scan failed...");

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(mWifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        Yes = findViewById(R.id.Yes);
        No = findViewById(R.id.No);

        Yes.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("scanResults", (Serializable) scanResults);
            Intent intent = getIntent();
            int idx = intent.getIntExtra("Index", -1);
            if (idx != -1) {
                resultIntent.putExtra("Index", idx);
            }
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });

        No.setOnClickListener(v -> {
            setResult(Activity.RESULT_OK, null);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }
}