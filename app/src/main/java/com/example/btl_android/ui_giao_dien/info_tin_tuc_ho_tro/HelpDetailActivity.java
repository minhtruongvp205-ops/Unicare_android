package com.example.btl_android.ui_giao_dien_giao_dien.info_tin_tuc_ho_tro;

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

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class HelpDetailActivity extends AppCompatActivity {
    public static final String EXTRA_FAQ = "isFaq";
    public static final String EXTRA_INDEX = "articleIndex";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_detail);
        App.applyInsets(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        boolean faq = getIntent().getBooleanExtra(EXTRA_FAQ, false);
        int index = getIntent().getIntExtra(EXTRA_INDEX, -1);
        List<HelpContent.Article> items = faq ? HelpContent.faqs() : HelpContent.guides();
        if (index < 0 || index >= items.size()) {
            finish();
            return;
        }
        HelpContent.Article article = items.get(index);
        ((TextView) findViewById(R.id.tvSectionName)).setText(faq ? "Câu hỏi thường gặp" : "Quy trình hướng dẫn");
        ((TextView) findViewById(R.id.tvArticleTitle)).setText(article.title);
        ((TextView) findViewById(R.id.tvArticleContent)).setText(article.content);
    }
}
