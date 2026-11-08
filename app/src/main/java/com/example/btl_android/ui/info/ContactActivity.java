package com.example.btl_android.ui.info;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.btl_android.R;
import com.example.btl_android.core.App;

public class ContactActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
        App.applyInsets(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}