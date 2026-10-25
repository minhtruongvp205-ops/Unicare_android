package com.example.btl_android;

import android.content.Intent;
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

public class ServicesActivity extends AppCompatActivity {

    private static class ServiceItem {
        String title, date;
        ServiceItem(String t, String d) { title = t; date = d; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services);
        App.applyInsets(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        RecyclerView rv = findViewById(R.id.rvServices);
        rv.setLayoutManager(new LinearLayoutManager(this));
        
        List<ServiceItem> services = new ArrayList<>();
        services.add(new ServiceItem("CHĂM SÓC KHÁCH HÀNG", "22/10/2020"));
        services.add(new ServiceItem("KHÁM BỆNH NGOẠI TRÚ", "19/07/2022"));
        services.add(new ServiceItem("PHÒNG KHÁM CHUYÊN GIA", "20/10/2022"));
        services.add(new ServiceItem("TIÊM CHỦNG", "15/09/2022"));
        services.add(new ServiceItem("PHÒNG KHÁM THẦN KINH CHUYÊN SÂU", "20/09/2022"));
        services.add(new ServiceItem("CHĂM SÓC THAI SẢN", "31/08/2022"));
        services.add(new ServiceItem("DỊCH VỤ VẬN CHUYỂN BẰNG XE CỨU THƯƠNG 24/7", "30/06/2022"));
        
        rv.setAdapter(new ServiceAdapter(services));
    }
    
    private class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.Holder> {
        List<ServiceItem> list;
        ServiceAdapter(List<ServiceItem> list) { this.list = list; }
        class Holder extends RecyclerView.ViewHolder {
            TextView title, date;
            Holder(View v) { 
                super(v); 
                title = v.findViewById(R.id.tvServiceTitle); 
                date = v.findViewById(R.id.tvServiceDate); 
            }
        }
        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_service, p, false));
        }
        @Override
        public void onBindViewHolder(@NonNull Holder h, int pos) {
            ServiceItem s = list.get(pos);
            h.title.setText(s.title);
            h.date.setText("🕒 " + s.date);
            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(ServicesActivity.this, ServiceDetailActivity.class);
                i.putExtra("title", s.title);
                startActivity(i);
            });
        }
        @Override public int getItemCount() { return list.size(); }
    }
}