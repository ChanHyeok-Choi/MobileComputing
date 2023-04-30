package com.example.assignment1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class SeeActivity extends AppCompatActivity {
    List<ScanResult> scanResults;
    Button Yes;

    String mApStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see);

        Yes = findViewById(R.id.GoBackYes);

        Intent intent = getIntent();
        scanResults = (List<ScanResult>) intent.getSerializableExtra("ScanResults");

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

        Yes.setOnClickListener(v -> { finish(); });
    }

    private void setTextView(String str) {
        TextView tv = findViewById(R.id.textSavedScanView);
        tv.setMovementMethod(new ScrollingMovementMethod());
        tv.setText(str);
    }
}