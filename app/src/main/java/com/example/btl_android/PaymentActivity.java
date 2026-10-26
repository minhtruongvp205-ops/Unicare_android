package com.example.btl_android;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Đọc đơn thuốc thật từ treatment_plans (cùng nơi bác sĩ lưu đơn).
 * Không tự động xác nhận đã thanh toán: chỉ ghi nhận yêu cầu trả tiền mặt.
 */
public class PaymentActivity extends AppCompatActivity {
    public static final String EXTRA_APPOINTMENT_ID = "appointmentId";

    private Spinner spinnerPlans;
    private RecyclerView rvItems;
    private TextView tvStatus, tvTotal;
    private EditText etAddress;
    private RadioGroup rgMethod;
    private Button btnConfirm;

    private final List<DocumentSnapshot> plans = new ArrayList<>();
    private final List<Map<String, Object>> medicines = new ArrayList<>();
    private PaymentItemAdapter itemAdapter;
    private DocumentSnapshot selectedPlan;
    private long selectedTotal;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (App.user() == null) {
            App.requireLogin(this);
            return;
        }
        setContentView(R.layout.activity_payment);
        App.applyInsets(this);
        mapping();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Chỉ có thanh toán tiền mặt mô phỏng khâu ghi nhận yêu cầu.
        findViewById(R.id.rbCard).setEnabled(false);
        findViewById(R.id.rbWallet).setEnabled(false);
        btnConfirm.setOnClickListener(v -> confirmOrder());
        loadPrescriptions();
    }

    private void mapping() {
        spinnerPlans = findViewById(R.id.spinnerPrescriptions);
        rvItems = findViewById(R.id.rvPaymentItems);
        tvStatus = findViewById(R.id.tvPaymentStatus);
        tvTotal = findViewById(R.id.tvTotalAmount);
        etAddress = findViewById(R.id.etAddress);
        rgMethod = findViewById(R.id.rgPaymentMethod);
        btnConfirm = findViewById(R.id.btnConfirmPayment);
        btnConfirm.setEnabled(false);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setNestedScrollingEnabled(false);
        itemAdapter = new PaymentItemAdapter(medicines);
        rvItems.setAdapter(itemAdapter);
    }

    private void loadPrescriptions() {
        tvStatus.setText("Đang tải đơn thuốc của bạn...");
        App.db().collection("treatment_plans")
                .whereEqualTo("patientId", App.uid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    plans.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Object raw = doc.get("items");
                        if (raw instanceof List && !((List<?>) raw).isEmpty()) {
                            plans.add(doc);
                        }
                    }
                    Collections.sort(plans, (a, b) -> {
                        com.google.firebase.Timestamp x = a.getTimestamp("createdAt");
                        com.google.firebase.Timestamp y = b.getTimestamp("createdAt");
                        if (x == null || y == null) return 0;
                        return y.compareTo(x);
                    });
                    if (plans.isEmpty()) {
                        tvStatus.setText("Chưa có đơn thuốc được bác sĩ kê. Hãy kiểm tra mục Hồ sơ bệnh án sau khi khám.");
                        btnConfirm.setEnabled(false);
                        spinnerPlans.setVisibility(View.GONE);
                        return;
                    }
                    spinnerPlans.setVisibility(View.VISIBLE);
                    List<String> titles = new ArrayList<>();
                    for (int i = 0; i < plans.size(); i++) {
                        DocumentSnapshot doc = plans.get(i);
                        long amount = num(doc.get("totalAmountVnd"));
                        titles.add("Đơn " + (i + 1) + " • " + doc.getId()
                                + " • " + PriceText.vnd(amount));
                    }
                    ArrayAdapter<String> labels = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, titles);
                    labels.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerPlans.setAdapter(labels);
                    spinnerPlans.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                            showPlan(plans.get(pos));
                        }
                        @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
                    });
                    String requested = getIntent().getStringExtra(EXTRA_APPOINTMENT_ID);
                    if (!TextUtils.isEmpty(requested)) {
                        for (int i = 0; i < plans.size(); i++) {
                            if (plans.get(i).getId().equals(requested)) {
                                spinnerPlans.setSelection(i);
                                break;
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> tvStatus.setText("Không tải được đơn thuốc: " + e.getLocalizedMessage()));
    }

    private void showPlan(DocumentSnapshot plan) {
        selectedPlan = plan;
        medicines.clear();
        Object raw = plan.get("items");
        if (raw instanceof List) {
            for (Object element : (List<?>) raw) {
                if (element instanceof Map) {
                    @SuppressWarnings("unchecked") Map<String, Object> item = (Map<String, Object>) element;
                    medicines.add(item);
                }
            }
        }
        itemAdapter.notifyDataSetChanged();
        selectedTotal = num(plan.get("totalAmountVnd"));
        tvTotal.setText("Tổng tiền thuốc dự kiến: " + PriceText.vnd(selectedTotal));
        tvStatus.setText("Đơn thuốc của bác sĩ • " + medicines.size() + " loại thuốc");
        btnConfirm.setEnabled(!medicines.isEmpty());
    }

    private void confirmOrder() {
        if (selectedPlan == null || medicines.isEmpty()) {
            Toast.makeText(this, "Chưa chọn đơn thuốc", Toast.LENGTH_SHORT).show();
            return;
        }
        String address = etAddress.getText().toString().trim();
        if (address.length() < 5) {
            etAddress.setError("Nhập địa chỉ nhận thuốc (ít nhất 5 ký tự)");
            etAddress.requestFocus();
            return;
        }
        if (rgMethod.getCheckedRadioButtonId() != R.id.rbCash) {
            Toast.makeText(this, "Chưa hỗ trợ thanh toán trực tuyến", Toast.LENGTH_LONG).show();
            return;
        }
        btnConfirm.setEnabled(false);
        String appointmentId = selectedPlan.getId();
        // Một lượt khám có tối đa một yêu cầu thanh toán, không tạo trùng khi bấm lại.
        Map<String, Object> order = new HashMap<>();
        order.put("orderId", appointmentId);
        order.put("appointmentId", appointmentId);
        order.put("patientId", App.uid());
        order.put("address", address);
        order.put("paymentMethod", "CASH");
        order.put("status", "PENDING");
        order.put("totalAmountVnd", selectedTotal);
        order.put("items", new ArrayList<>(medicines));
        order.put("createdAt", FieldValue.serverTimestamp());

        App.db().collection("orders").document(appointmentId).set(order)
                .addOnSuccessListener(ignored -> {
                    Toast.makeText(this, "Đã ghi nhận yêu cầu thanh toán tiền mặt", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, InvoiceActivity.class)
                            .putExtra(InvoiceActivity.EXTRA_ORDER_ID, appointmentId));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirm.setEnabled(true);
                    tvStatus.setText("Không lưu được yêu cầu: " + e.getLocalizedMessage());
                    Toast.makeText(this, "Nếu đã tạo hóa đơn, hãy xem ở mục Hóa đơn.", Toast.LENGTH_LONG).show();
                });
    }

    private long num(Object obj) { return obj instanceof Number ? ((Number) obj).longValue() : 0L; }
}
