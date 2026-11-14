package com.example.btl_android.ui_giao_dien_giao_dien.dashboard_trang_chu;

import com.example.btl_android.R;
import com.example.btl_android.core_he_thong.*;
import com.example.btl_android.core_he_thong.manager.*;
import com.example.btl_android.data_du_lieu.*;
import com.example.btl_android.data_du_lieu.model.*;
import com.example.btl_android.utils_tien_ich.*;
import com.example.btl_android.ui_giao_dien_giao_dien.auth_dang_nhap.*;
import com.example.btl_android.ui_giao_dien_giao_dien.dashboard_trang_chu.*;
import com.example.btl_android.ui_giao_dien_giao_dien.appointment_lich_hen.*;
import com.example.btl_android.ui_giao_dien_giao_dien.medical_kham_benh.*;
import com.example.btl_android.ui_giao_dien_giao_dien.medical_kham_benh.adapter.*;
import com.example.btl_android.ui_giao_dien_giao_dien.payment_thanh_toan.*;
import com.example.btl_android.ui_giao_dien_giao_dien.payment_thanh_toan.adapter.*;
import com.example.btl_android.ui_giao_dien_giao_dien.info_tin_tuc_ho_tro.*;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

    private String userRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.requireLogin(this);
        if (App.user() == null) return;
        setContentView(R.layout.activity_dashboard);
        App.applyInsets(this);

        // Xin quyền gửi thông báo trên Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        BottomNavigationView navView = findViewById(R.id.bottom_navigation);

        navView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                if (userRole.equals("doctor")) {
                    loadFragment(new DoctorHomeFragment());
                } else {
                    loadFragment(new HomeFragment());
                }
                return true;
            } else if (id == R.id.nav_notices) {
                Intent intent = new Intent(this, AppointmentsActivity.class);
                intent.putExtra("notices", true);
                startActivity(intent);
                return false;
            } else if (id == R.id.nav_features) {
                startActivity(new Intent(this, AppointmentsActivity.class));
                return false;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return false;
            }
            return false;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (App.user() == null) return;
        App.db().collection("users").document(App.uid()).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists()) { App.note(this, "Không tìm thấy hồ sơ"); App.logout(this); return; }
                    userRole = App.str(d, "role");
                    AppNotificationManager.startListening(this, App.uid(), userRole);
                    
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (userRole.equals("doctor")) {
                        if (!(current instanceof DoctorHomeFragment)) loadFragment(new DoctorHomeFragment());
                    } else if (userRole.equals("patient")) {
                        if (!(current instanceof HomeFragment)) loadFragment(new HomeFragment());
                    } else {
                        App.logout(this);
                    }
                })
                .addOnFailureListener(e -> App.error(this, e));
    }

    @Override
    protected void onDestroy() {
        AppNotificationManager.stopListening();
        super.onDestroy();
    }
    
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}