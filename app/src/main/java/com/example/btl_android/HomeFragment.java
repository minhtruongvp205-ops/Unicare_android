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
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
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
        setupOtherFeatures(view);

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
        features.add(new Feature("Thanh toán", R.drawable.ic_medical_record, () -> startActivity(new Intent(getContext(), PaymentActivity.class).putExtra("prescId", "examplePrescId"))));
        features.add(new Feature("Hoá đơn", R.drawable.ic_medical_record, () -> startActivity(new Intent(getContext(), InvoiceActivity.class))));

        // Hàng 2
        features.add(new Feature("Hồ sơ sức khoẻ", R.drawable.ic_medical_record,
                () -> startActivity(new Intent(getContext(), HealthVitalsActivity.class))));
        features.add(new Feature("Hướng dẫn", R.drawable.ic_search,
                () -> startActivity(new Intent(getContext(), UserGuideActivity.class))));
        features.add(new Feature("Theo dõi tại nhà", R.drawable.ic_medical_record,
                () -> startActivity(new Intent(getContext(), MedicationActivity.class)))); // Sử dụng nhắc nhở thuốc cho Theo dõi tại nhà
        features.add(new Feature("Hỏi đáp (Chat)", R.drawable.ic_person, this::showComingSoon));

        rv.setAdapter(new FeatureAdapter(features));
    }

    private static class NewsItem {
        String title;
        String url;
        NewsItem(String t, String u) {
            title = t;
            url = u;
        }
    }

    private void setupNews(View view) {
        RecyclerView rv = view.findViewById(R.id.rvNews);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        List<NewsItem> news = new ArrayList<>();
        news.add(new NewsItem("Các bệnh về tim mạch: Nguyên nhân, dấu hiệu...", "https://timbenhvien.vn/cam-nang/cac-benh-ve-tim-nguyen-nhan-dau-hieu-nhan-biet-va-huong-dieu-tri/"));
        news.add(new NewsItem("Các bệnh lý về gan thường gặp", "https://benhviendongnai.com.vn/cac-benh-ly-ve-gan-thuong-gap/"));
        news.add(new NewsItem("Ung thư cổ tử cung: Nguyên nhân, dấu hiệu...", "https://vinuni.edu.vn/vi/ung-thu-co-tu-cung-nguyen-nhan-dau-hieu-va-phuong-phap-dieu-tri/"));

        rv.setAdapter(new NewsAdapter(news));
        
        TextView tvViewAllNews = view.findViewById(R.id.tvViewAllNews);
        if (tvViewAllNews != null) {
            tvViewAllNews.setOnClickListener(v -> {
                startActivity(new Intent(getContext(), NewsActivity.class));
            });
        }
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
        features.add(new Feature("Liên hệ", R.drawable.ic_schedule,
                () -> startActivity(new Intent(getContext(), ContactActivity.class))));
        features.add(new Feature("Đăng xuất", R.drawable.ic_person, () -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getContext(), "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getContext(), AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }));

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
        List<NewsItem> list;

        NewsAdapter(List<NewsItem> list) {
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