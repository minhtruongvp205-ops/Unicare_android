package com.example.btl_android.ui_giao_dien_giao_dien.auth_dang_nhap;

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

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Animate logo container in
        View logo = findViewById(R.id.logoContainer);
        logo.setAlpha(0f);
        logo.setScaleX(0.6f);
        logo.setScaleY(0.6f);
        logo.setTranslationY(60f);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.6f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.6f, 1f);
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(logo, "translationY", 60f, 0f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeIn, scaleX, scaleY, slideUp);
        set.setDuration(800);
        set.setInterpolator(new OvershootInterpolator(1.2f));
        set.start();

        // Navigate after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (App.user() != null) {
                // Already logged in – go to Dashboard
                Intent i = new Intent(this, DashboardActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            } else {
                Intent i = new Intent(this, AuthActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            }
        }, 2000);
    }
}
