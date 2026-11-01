package com.example.btl_android;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ServiceDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_detail);
        App.applyInsets(this);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        String title = getIntent().getStringExtra("title");
        if (title != null) {
            ((TextView) findViewById(R.id.tvTitle)).setText(title);
        }
    }
}