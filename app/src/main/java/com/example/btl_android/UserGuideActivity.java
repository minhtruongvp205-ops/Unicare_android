package com.example.btl_android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class UserGuideActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_guide);
        App.applyInsets(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        RecyclerView rv = findViewById(R.id.rvGuides);
        rv.setLayoutManager(new LinearLayoutManager(this));
        
        List<String> guides = new ArrayList<>();
        guides.add("Hướng dẫn sử dụng và cài đặt UniCare");
        guides.add("Hướng dẫn đồng bộ số hồ sơ người bệnh trên ứng dụng di động UniCare");
        guides.add("Hướng dẫn đặt khám trên UniCare");
        guides.add("Hướng dẫn huỷ phiếu đặt khám");
        guides.add("Hướng dẫn quy trình khám bệnh");
        guides.add("Hướng dẫn thanh toán đơn thuốc trên ứng dụng UniCare");
        
        rv.setAdapter(new GuideAdapter(guides));
    }
    
    private class GuideAdapter extends RecyclerView.Adapter<GuideAdapter.Holder> {
        List<String> list;
        GuideAdapter(List<String> list) { this.list = list; }
        class Holder extends RecyclerView.ViewHolder {
            TextView title;
            Holder(View v) { super(v); title = v.findViewById(R.id.tvGuideTitle); }
        }
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_guide, p, false));
        }
        @Override
        public void onBindViewHolder(@NonNull Holder h, int pos) {
            h.title.setText(list.get(pos));
        }
        @Override public int getItemCount() { return list.size(); }
    }
}