package com.example.btl_android;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Danh sách phiếu yêu cầu thanh toán của tài khoản bệnh nhân hiện tại. */
public class InvoiceActivity extends AppCompatActivity {
    public static final String EXTRA_ORDER_ID = "extra_order_id";
    private LinearLayout listContainer;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (App.user() == null) {
            App.requireLogin(this);
            return;
        }
        setContentView(R.layout.activity_invoice);
        App.applyInsets(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        listContainer = findViewById(R.id.invoiceListContainer);
        tvEmpty = findViewById(R.id.tvInvoiceEmpty);
        loadOrders();
    }

    private void loadOrders() {
        tvEmpty.setText("Đang tải hóa đơn...");
        App.db().collection("orders")
                .whereEqualTo("patientId", App.uid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    listContainer.removeAllViews();
                    List<DocumentSnapshot> orders = new ArrayList<>(snapshot.getDocuments());
                    Collections.sort(orders, (a, b) -> {
                        Timestamp x = a.getTimestamp("createdAt");
                        Timestamp y = b.getTimestamp("createdAt");
                        if (x == null || y == null) return 0;
                        return y.compareTo(x);
                    });
                    tvEmpty.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
                    if (orders.isEmpty()) tvEmpty.setText("Bạn chưa có phiếu yêu cầu thanh toán.");
                    String wantedId = getIntent().getStringExtra(EXTRA_ORDER_ID);
                    boolean opened = false;
                    for (DocumentSnapshot doc : orders) {
                        addInvoiceCard(doc);
                        if (!opened && doc.getId().equals(wantedId)) {
                            opened = true;
                            // Hóa đơn vừa lập: người dùng nhìn thấy trong danh sách, có thể chạm để xem chi tiết.
                        }
                    }
                })
                .addOnFailureListener(e -> tvEmpty.setText("Không tải được hóa đơn: " + e.getLocalizedMessage()
                        + "\nKiểm tra quyền đọc orders trong Firestore Rules."));
    }

    private void addInvoiceCard(DocumentSnapshot order) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int space = dp(12);
        params.setMargins(0, 0, 0, space);
        card.setLayoutParams(params);
        card.setRadius(dp(14));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(android.graphics.Color.WHITE);

        TextView text = new TextView(this);
        text.setPadding(dp(16), dp(15), dp(16), dp(15));
        text.setTextColor(0xff1a2440);
        text.setTextSize(15);
        text.setText("Phiếu: " + order.getId()
                + "\nNgày tạo: " + App.date(order.getTimestamp("createdAt"))
                + "\nTổng tiền: " + PriceText.vnd(num(order.get("totalAmountVnd")))
                + "\nTrạng thái: " + status(order.getString("status"))
                + "\nNhấn để xem chi tiết");
        card.addView(text);
        card.setOnClickListener(v -> showDetail(order));
        listContainer.addView(card);
    }

    private void showDetail(DocumentSnapshot order) {
        StringBuilder detail = new StringBuilder();
        detail.append("Mã phiếu: ").append(order.getId())
                .append("\nNgày: ").append(App.date(order.getTimestamp("createdAt")))
                .append("\nTrạng thái: ").append(status(order.getString("status")))
                .append("\nĐịa chỉ nhận: ").append(str(order.get("address")))
                .append("\nHình thức: Tiền mặt\n\nĐƠN THUỐC\n");
        Object raw = order.get("items");
        if (raw instanceof List) {
            for (Object item : (List<?>) raw) {
                if (!(item instanceof Map)) continue;
                @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) item;
                detail.append("\n• ").append(str(m.get("name")))
                        .append(" — SL: ").append(num(m.get("quantity")))
                        .append(" ").append(str(m.get("unit")))
                        .append("\n  ").append(str(m.get("dose")))
                        .append(" • ").append(str(m.get("schedule")))
                        .append("\n  Thành tiền: ").append(PriceText.vnd(num(m.get("lineTotalVnd"))))
                        .append("\n");
            }
        }
        detail.append("\nTổng tiền dự kiến: ")
                .append(PriceText.vnd(num(order.get("totalAmountVnd"))))
                .append("\n\nPhiếu này chưa xác nhận đã thu tiền.");
        new AlertDialog.Builder(this)
                .setTitle("Chi tiết hóa đơn")
                .setMessage(detail.toString())
                .setPositiveButton("Đóng", null)
                .show();
    }

    private int dp(int n) { return Math.round(getResources().getDisplayMetrics().density * n); }
    private static long num(Object o) { return o instanceof Number ? ((Number) o).longValue() : 0; }
    private static String str(Object o) { return o == null ? "" : o.toString(); }
    private static String status(String value) {
        return "PENDING".equals(value) ? "Chờ thanh toán" : str(value);
    }
}
