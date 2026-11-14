package com.example.btl_android.ui_giao_dien.info_tin_tuc_ho_tro;

import com.example.btl_android.R;
import com.example.btl_android.core_he_thong.*;
import com.example.btl_android.core_he_thong.manager.*;
import com.example.btl_android.data_du_lieu.*;
import com.example.btl_android.data_du_lieu.model.*;
import com.example.btl_android.utils_tien_ich.*;
import com.example.btl_android.ui_giao_dien.auth_dang_nhap.*;
import com.example.btl_android.ui_giao_dien.dashboard_trang_chu.*;
import com.example.btl_android.ui_giao_dien.appointment_lich_hen.*;
import com.example.btl_android.ui_giao_dien.medical_kham_benh.*;
import com.example.btl_android.ui_giao_dien.medical_kham_benh.adapter.*;
import com.example.btl_android.ui_giao_dien.payment_thanh_toan.*;
import com.example.btl_android.ui_giao_dien.payment_thanh_toan.adapter.*;
import com.example.btl_android.ui_giao_dien.info_tin_tuc_ho_tro.*;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class NewsActivity extends AppCompatActivity {

    private static class NewsItem {
        String title;
        String url;
        NewsItem(String t, String u) { title = t; url = u; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);
        App.applyInsets(this);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvAllNews);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<NewsItem> news = new ArrayList<>();
        news.add(new NewsItem("Các bệnh về tim mạch: Nguyên nhân, dấu hiệu...", "https://timbenhvien.vn/cam-nang/cac-benh-ve-tim-nguyen-nhan-dau-hieu-nhan-biet-va-huong-dieu-tri/"));
        news.add(new NewsItem("Các bệnh lý về gan thường gặp", "https://benhviendongnai.com.vn/cac-benh-ly-ve-gan-thuong-gap/"));
        news.add(new NewsItem("Ung thư cổ tử cung: Nguyên nhân, dấu hiệu...", "https://vinuni.edu.vn/vi/ung-thu-co-tu-cung-nguyen-nhan-dau-hieu-va-phuong-phap-dieu-tri/"));
        news.add(new NewsItem("Sỏi thận: Nguyên nhân, triệu chứng và điều trị", "https://benhviennamthanglong.vn/soi-than/"));
        news.add(new NewsItem("Đau dạ dày: Nguyên nhân, triệu chứng, cách điều trị", "https://tamanhhospital.vn/benh/dau-da-day/"));

        rv.setAdapter(new AllNewsAdapter(news));
    }

    private class AllNewsAdapter extends RecyclerView.Adapter<AllNewsAdapter.Holder> {
        List<NewsItem> list;

        AllNewsAdapter(List<NewsItem> list) {
            this.list = list;
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView title;
            Holder(View v) {
                super(v);
                title = v.findViewById(R.id.tvNewsTitle);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View view = LayoutInflater.from(p.getContext()).inflate(R.layout.item_news, p, false);
            // Change width to match parent for vertical list
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null) {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                view.setLayoutParams(lp);
            }
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int pos) {
            NewsItem item = list.get(pos);
            h.title.setText(item.title);
            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(item.url));
                v.getContext().startActivity(i);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }
}
