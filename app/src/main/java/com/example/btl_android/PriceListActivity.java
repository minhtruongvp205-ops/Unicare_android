package com.example.btl_android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Bảng giá tham khảo công khai, không phải báo giá của UniCare. */
public class PriceListActivity extends AppCompatActivity {
    private static final String SOURCE = "https://thuvienphapluat.vn/van-ban/The-thao-Y-te/Quyet-dinh-2375-QD-BYT-2026-Gia-dich-vu-kham-chua-benh-tai-Benh-vien-Dai-hoc-Y-Ha-Noi-720804.aspx";

    static class PriceItem {
        final String category, name, notes;
        final int amount;
        PriceItem(String category, String name, int amount, String notes) {
            this.category = category;
            this.name = name;
            this.amount = amount;
            this.notes = notes;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_price_list);
        App.applyInsets(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.tvPriceSource).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE))));

        List<PriceItem> prices = new ArrayList<>();
        prices.add(new PriceItem("KHÁM BỆNH", "Khám bệnh", 52600,
                "Mức giá khám bệnh thuộc danh mục do quỹ BHYT thanh toán, Phụ lục 01."));
        prices.add(new PriceItem("KHÁM BỆNH", "Hội chẩn xác định ca bệnh khó", 208000,
                "Chỉ áp dụng khi mời chuyên gia từ đơn vị khác đến hội chẩn tại cơ sở khám chữa bệnh."));
        prices.add(new PriceItem("SIÊU ÂM", "Siêu âm ổ bụng", 61000,
                "Mức giá dịch vụ kỹ thuật ở Phụ lục 03; thực hiện theo chỉ định y tế."));
        prices.add(new PriceItem("SIÊU ÂM", "Siêu âm tuyến giáp", 61000,
                "Mức giá dịch vụ kỹ thuật ở Phụ lục 03."));
        prices.add(new PriceItem("SIÊU ÂM", "Siêu âm thai (thai, nhau thai, nước ối)", 61000,
                "Mức giá dịch vụ kỹ thuật ở Phụ lục 03; tùy chỉ định của bác sĩ."));
        prices.add(new PriceItem("SIÊU ÂM", "Siêu âm Doppler tim", 262400,
                "Mức giá dịch vụ kỹ thuật ở Phụ lục 03."));
        prices.add(new PriceItem("SIÊU ÂM", "Siêu âm tim 4D", 505800,
                "Theo Phụ lục 03, chỉ áp dụng trong trường hợp chỉ định để thực hiện phẫu thuật hoặc can thiệp tim mạch."));
        prices.add(new PriceItem("CHẨN ĐOÁN HÌNH ẢNH", "Chụp X-quang ngực thẳng (1 tư thế)", 66900,
                "Áp dụng cho phim kích thước lớn hơn 24 × 30 cm; một vị trí, một tư thế, Phụ lục 03."));
        prices.add(new PriceItem("CHẨN ĐOÁN HÌNH ẢNH", "Chụp X-quang ngực thẳng (số hóa 1 phim)", 76300,
                "Áp dụng cho một vị trí, một phim số hóa, Phụ lục 03."));
        prices.add(new PriceItem("THỦ THUẬT", "Tiêm bắp thịt", 15700,
                "Chỉ áp dụng với người bệnh ngoại trú và chưa bao gồm thuốc tiêm, Phụ lục 03."));
        prices.add(new PriceItem("THỦ THUẬT", "Tiêm tĩnh mạch", 15700,
                "Chỉ áp dụng với người bệnh ngoại trú và chưa bao gồm thuốc tiêm, Phụ lục 03."));

        RecyclerView recycler = findViewById(R.id.rvPrices);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(new PriceAdapter(prices));
    }

    private String money(int amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + " đ";
    }

    private class PriceAdapter extends RecyclerView.Adapter<PriceAdapter.Holder> {
        final List<PriceItem> items;
        PriceAdapter(List<PriceItem> items) { this.items = items; }

        class Holder extends RecyclerView.ViewHolder {
            TextView category, name, amount;
            Holder(View view) {
                super(view);
                category = view.findViewById(R.id.tvPriceCategory);
                name = view.findViewById(R.id.tvPriceName);
                amount = view.findViewById(R.id.tvPriceAmount);
            }
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_service_price, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            PriceItem item = items.get(position);
            holder.category.setText(item.category);
            holder.name.setText(item.name);
            holder.amount.setText(money(item.amount));
            holder.itemView.setOnClickListener(v -> new AlertDialog.Builder(PriceListActivity.this)
                    .setTitle(item.name)
                    .setMessage("Giá công bố: " + money(item.amount) + "\n\n" + item.notes
                            + "\n\nĐây là giá tham khảo tại Bệnh viện Đại học Y Hà Nội theo Quyết định 2375/QĐ-BYT ngày 28/07/2026, không phải giá dịch vụ do UniCare thu.")
                    .setPositiveButton("Đã hiểu", null)
                    .show());
        }

        @Override public int getItemCount() { return items.size(); }
    }
}
