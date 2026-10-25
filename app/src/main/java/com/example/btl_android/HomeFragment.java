package com.example.btl_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static class Feature {
        String title;
        int iconRes;
        Runnable action;

        Feature(String t, int ic, Runnable a) {
            title = t;
            iconRes = ic;
            action = a;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        setupMainFeatures(view);
        setupNews(view);
        // setupOtherFeatures(view); // removed per user request

        return view;
    }

    private void setupMainFeatures(View view) {
        RecyclerView rv = view.findViewById(R.id.rvMainFeatures);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 4));

        List<Feature> features = new ArrayList<>();
        // Hàng 1
        features.add(new Feature("Đặt khám", R.drawable.ic_schedule,
                () -> startActivity(new Intent(getContext(), BookingActivity.class))));
        features.add(new Feature("Lịch sử đặt khám", R.drawable.ic_calendar,
                () -> startActivity(new Intent(getContext(), AppointmentsActivity.class))));
        features.add(new Feature("Thanh toán", R.drawable.ic_medical_record, this::showComingSoon));
        features.add(new Feature("Hoá đơn", R.drawable.ic_medical_record, this::showComingSoon));

        // Hàng 2
        features.add(new Feature("Hồ sơ sức khoẻ", R.drawable.ic_medical_record,
                () -> startActivity(new Intent(getContext(), HealthVitalsActivity.class))));
        features.add(new Feature("Kết quả CLS", R.drawable.ic_search, this::showComingSoon));
        features.add(new Feature("Lịch hẹn NT", R.drawable.ic_schedule, this::showComingSoon));
        features.add(new Feature("Lắng nghe KH", R.drawable.ic_notification, this::showComingSoon));

        // Hàng 3
        features.add(new Feature("Hướng dẫn", R.drawable.ic_search,
                () -> startActivity(new Intent(getContext(), UserGuideActivity.class))));
        features.add(new Feature("Theo dõi tại nhà", R.drawable.ic_medical_record,
                () -> startActivity(new Intent(getContext(), MedicationActivity.class)))); // Sử dụng nhắc nhở thuốc cho
                                                                                           // Theo dõi tại nhà
        features.add(new Feature("Tiêm chủng", R.drawable.ic_schedule, this::showComingSoon));
        features.add(new Feature("Hỏi đáp (Chat)", R.drawable.ic_person, this::showComingSoon));

        rv.setAdapter(new FeatureAdapter(features));
    }

    private void setupNews(View view) {
        RecyclerView rv = view.findViewById(R.id.rvNews);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        List<String> news = new ArrayList<>();
        news.add("Chương trình sinh hoạt nhóm người bệnh Ung thư dạ dày: Niềm tin...");
        news.add("Thông báo nghỉ lễ Ngày Thống nhất Đất nước 30/4 & Ngày Quốc tế LĐ...");
        news.add("Tầm soát ung thư sớm: Lợi ích và những điều cần biết");

        rv.setAdapter(new NewsAdapter(news));
    }

    private void setupOtherFeatures(View view) {
        RecyclerView rv = view.findViewById(R.id.rvOtherFeatures);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 4));

        List<Feature> features = new ArrayList<>();
        features.add(new Feature("Hướng dẫn\nkhách hàng", R.drawable.ic_search,
                () -> startActivity(new Intent(getContext(), UserGuideActivity.class))));
        features.add(new Feature("Dịch vụ\nnổi bật", R.drawable.ic_medical_record,
                () -> startActivity(new Intent(getContext(), ServicesActivity.class))));
        features.add(new Feature("Bảng giá\ndịch vụ", R.drawable.ic_medical_record, this::showComingSoon));
        features.add(new Feature("Thư viện\nsức khoẻ", R.drawable.ic_medical_record, this::showComingSoon));
        features.add(new Feature("Tin tức -\nSự kiện", R.drawable.ic_notification, this::showComingSoon));
        features.add(new Feature("Liên hệ", R.drawable.ic_schedule,
                () -> startActivity(new Intent(getContext(), ContactActivity.class))));

        rv.setAdapter(new FeatureAdapter(features));
    }

    private void showComingSoon() {
        Toast.makeText(getContext(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
    }

    // --- ADAPTERS ---
    private class FeatureAdapter extends RecyclerView.Adapter<FeatureAdapter.Holder> {
        List<Feature> list;

        FeatureAdapter(List<Feature> list) {
            this.list = list;
        }

        class Holder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView title;
            LinearLayout container;

            Holder(View v) {
                super(v);
                icon = v.findViewById(R.id.ivIcon);
                title = v.findViewById(R.id.tvTitle);
                container = v.findViewById(R.id.iconContainer);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_feature_grid, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int pos) {
            Feature f = list.get(pos);
            h.title.setText(f.title);
            h.icon.setImageResource(f.iconRes);
            h.itemView.setOnClickListener(v -> {
                if (f.action != null)
                    f.action.run();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    private class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.Holder> {
        List<String> list;

        NewsAdapter(List<String> list) {
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
            return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_news, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int pos) {
            h.title.setText(list.get(pos));
            h.itemView.setOnClickListener(v -> showComingSoon());
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }
}