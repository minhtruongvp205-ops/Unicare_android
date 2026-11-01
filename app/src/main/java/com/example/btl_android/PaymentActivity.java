package com.example.btl_android;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chỉ hiển thị đơn thuốc có items và CHƯA có phiếu trong collection orders.
 * Một đơn thuốc tương ứng một lịch khám; orders/{appointmentId} là phiếu của đơn đó.
 * Phiếu ở trạng thái PENDING cũng được xem là đã tạo, nên đơn không hiện lại.
 */
public class PaymentActivity extends AppCompatActivity {
    public static final String EXTRA_APPOINTMENT_ID = "appointmentId";

    private Spinner spinnerPlans;
    private RecyclerView rvItems;
    private TextView tvStatus, tvTotal;
    private EditText etAddress;
    private RadioGroup rgMethod;
    private Button btnConfirm;
    private LinearLayout paymentFormContent;

    private final List<DocumentSnapshot> allPlans = new ArrayList<>();
    private final List<DocumentSnapshot> plans = new ArrayList<>();
    private final List<Map<String, Object>> medicines = new ArrayList<>();
    private final Set<String> orderedAppointmentIds = new HashSet<>();

    private PaymentItemAdapter itemAdapter;
    private DocumentSnapshot selectedPlan;
    private long selectedTotal;

    private ListenerRegistration planListener, orderListener;
    private boolean plansLoaded, ordersLoaded, creatingOrder;
    private String plansError, ordersError;

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

        // Chưa tích hợp cổng thẻ/ví: chỉ tiếp nhận yêu cầu thanh toán tiền mặt.
        findViewById(R.id.rbCard).setEnabled(false);
        findViewById(R.id.rbWallet).setEnabled(false);
        btnConfirm.setOnClickListener(v -> confirmOrder());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (App.user() != null) startListening();
    }

    @Override
    protected void onStop() {
        stopListening();
        super.onStop();
    }

    private void mapping() {
        spinnerPlans = findViewById(R.id.spinnerPrescriptions);
        rvItems = findViewById(R.id.rvPaymentItems);
        tvStatus = findViewById(R.id.tvPaymentStatus);
        tvTotal = findViewById(R.id.tvTotalAmount);
        etAddress = findViewById(R.id.etAddress);
        rgMethod = findViewById(R.id.rgPaymentMethod);
        btnConfirm = findViewById(R.id.btnConfirmPayment);
        paymentFormContent = findViewById(R.id.paymentFormContent);

        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setNestedScrollingEnabled(false);
        itemAdapter = new PaymentItemAdapter(medicines);
        rvItems.setAdapter(itemAdapter);
        paymentFormContent.setVisibility(View.GONE);
        btnConfirm.setEnabled(false);
    }

    private void startListening() {
        stopListening();
        plansLoaded = false;
        ordersLoaded = false;
        plansError = null;
        ordersError = null;
        clearSelection();
        tvStatus.setText("Đang kiểm tra các đơn thuốc chưa tạo phiếu yêu cầu thanh toán...");
        paymentFormContent.setVisibility(View.GONE);

        // Lấy các phiếu do chính bệnh nhân đã tạo, kể cả phiếu PENDING.
        orderListener = App.db().collection("orders")
                .whereEqualTo("patientId", App.uid())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        ordersLoaded = false;
                        ordersError = "Không kiểm tra được phiếu thanh toán: " + error.getLocalizedMessage();
                        renderPlans();
                        return;
                    }
                    if (snapshot == null) return;
                    orderedAppointmentIds.clear();
                    for (DocumentSnapshot order : snapshot.getDocuments()) {
                        orderedAppointmentIds.add(order.getId());
                        String appointmentId = order.getString("appointmentId");
                        if (!TextUtils.isEmpty(appointmentId)) {
                            orderedAppointmentIds.add(appointmentId);
                        }
                    }
                    ordersLoaded = true;
                    ordersError = null;
                    renderPlans();
                });

        // Nghe đơn thuốc mới: khi bác sĩ kê đơn, màn hình tự cập nhật.
        planListener = App.db().collection("treatment_plans")
                .whereEqualTo("patientId", App.uid())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        plansLoaded = false;
                        plansError = "Không tải được đơn thuốc: " + error.getLocalizedMessage();
                        renderPlans();
                        return;
                    }
                    if (snapshot == null) return;
                    allPlans.clear();
                    allPlans.addAll(snapshot.getDocuments());
                    plansLoaded = true;
                    plansError = null;
                    renderPlans();
                });
    }

    private void stopListening() {
        if (planListener != null) {
            planListener.remove();
            planListener = null;
        }
        if (orderListener != null) {
            orderListener.remove();
            orderListener = null;
        }
    }

    private void clearSelection() {
        selectedPlan = null;
        selectedTotal = 0;
        medicines.clear();
        if (itemAdapter != null) itemAdapter.notifyDataSetChanged();
        btnConfirm.setEnabled(false);
    }

    private void renderPlans() {
        // Không liệt kê đơn khi chưa biết đơn nào đã lập phiếu,
        // tránh bệnh nhân vô tình gửi yêu cầu lần thứ hai.
        if (ordersError != null || plansError != null) {
            paymentFormContent.setVisibility(View.GONE);
            clearSelection();
            tvStatus.setText(ordersError != null ? ordersError : plansError);
            return;
        }
        if (!ordersLoaded || !plansLoaded) {
            paymentFormContent.setVisibility(View.GONE);
            btnConfirm.setEnabled(false);
            return;
        }

        String previouslySelectedId = selectedPlan == null ? null : selectedPlan.getId();
        plans.clear();
        boolean hasPrescription = false;
        for (DocumentSnapshot doc : allPlans) {
            Object raw = doc.get("items");
            if (!(raw instanceof List) || ((List<?>) raw).isEmpty()) continue;
            hasPrescription = true;
            if (!orderedAppointmentIds.contains(doc.getId())) {
                plans.add(doc);
            }
        }
        Collections.sort(plans, (a, b) -> {
            com.google.firebase.Timestamp x = a.getTimestamp("createdAt");
            com.google.firebase.Timestamp y = b.getTimestamp("createdAt");
            if (x == null || y == null) return 0;
            return y.compareTo(x);
        });

        clearSelection();
        if (plans.isEmpty()) {
            paymentFormContent.setVisibility(View.GONE);
            tvStatus.setText(hasPrescription
                    ? "Bạn đã tạo phiếu yêu cầu thanh toán cho tất cả đơn thuốc hiện có. "
                      + "Khi bác sĩ kê đơn mới, đơn đó sẽ xuất hiện tại đây. "
                      + "Các phiếu trước đó có thể xem trong mục Hóa đơn."
                    : "Bạn chưa có đơn thuốc được bác sĩ kê. "
                      + "Đơn mới sẽ xuất hiện tại đây sau khi bác sĩ lưu bệnh án.");
            return;
        }

        paymentFormContent.setVisibility(View.VISIBLE);
        tvStatus.setText("Có " + plans.size() + " đơn thuốc chưa tạo phiếu yêu cầu thanh toán.");
        List<String> titles = new ArrayList<>();
        for (int i = 0; i < plans.size(); i++) {
            DocumentSnapshot doc = plans.get(i);
            titles.add("Đơn " + (i + 1) + " • " + doc.getId()
                    + " • " + PriceText.vnd(num(doc.get("totalAmountVnd"))));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, titles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPlans.setAdapter(adapter);
        spinnerPlans.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < plans.size()) showPlan(plans.get(position));
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {
                clearSelection();
            }
        });

        String requested = previouslySelectedId;
        if (TextUtils.isEmpty(requested)) {
            requested = getIntent().getStringExtra(EXTRA_APPOINTMENT_ID);
        }
        if (!TextUtils.isEmpty(requested)) {
            for (int i = 0; i < plans.size(); i++) {
                if (requested.equals(plans.get(i).getId())) {
                    spinnerPlans.setSelection(i);
                    break;
                }
            }
        }
    }

    private void showPlan(DocumentSnapshot plan) {
        // Phòng trường hợp đơn vừa được tạo phiếu trên thiết bị khác.
        if (orderedAppointmentIds.contains(plan.getId())) {
            renderPlans();
            return;
        }
        selectedPlan = plan;
        medicines.clear();
        Object raw = plan.get("items");
        if (raw instanceof List) {
            for (Object entry : (List<?>) raw) {
                if (entry instanceof Map) {
                    @SuppressWarnings("unchecked") Map<String, Object> item = (Map<String, Object>) entry;
                    medicines.add(item);
                }
            }
        }
        itemAdapter.notifyDataSetChanged();
        selectedTotal = num(plan.get("totalAmountVnd"));
        tvTotal.setText("Tổng tiền thuốc dự kiến: " + PriceText.vnd(selectedTotal));
        btnConfirm.setEnabled(!creatingOrder && !medicines.isEmpty());
    }

    private void confirmOrder() {
        if (creatingOrder || selectedPlan == null || medicines.isEmpty()) return;
        final String appointmentId = selectedPlan.getId();
        if (orderedAppointmentIds.contains(appointmentId)) {
            Toast.makeText(this, "Đơn này đã có phiếu yêu cầu thanh toán", Toast.LENGTH_LONG).show();
            renderPlans();
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

        creatingOrder = true;
        btnConfirm.setEnabled(false);
        // Không đọc document orders/{id} khi chưa tồn tại: Rules có thể từ chối
        // thao tác get trên tài liệu chưa được tạo. Danh sách orders bên trên
        // đã loại phiếu cũ; nếu đồng thời có thao tác khác thì Rules update
        // của orders vẫn phải từ chối việc ghi đè để không tạo phiếu trùng.
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

        DocumentReference orderRef = App.db().collection("orders").document(appointmentId);
        orderRef.set(order).addOnSuccessListener(unused -> {
            creatingOrder = false;
            Toast.makeText(this, "Đã tạo phiếu yêu cầu thanh toán", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, InvoiceActivity.class)
                    .putExtra(InvoiceActivity.EXTRA_ORDER_ID, appointmentId));
            finish();
        }).addOnFailureListener(e -> {
            creatingOrder = false;
            btnConfirm.setEnabled(selectedPlan != null);
            tvStatus.setText("Không tạo được phiếu: " + e.getLocalizedMessage()
                    + "\nNếu phiếu đã tồn tại, hãy xem trong mục Hóa đơn.");
        });
    }

    private long num(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}
