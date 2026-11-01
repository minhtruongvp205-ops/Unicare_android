package com.example.btl_android;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
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
    
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}