package com.example.btl_android;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** RecyclerView + Adapter + ViewHolder giống bài mẫu quản lý sinh viên. */
public class RecordsActivity extends AppCompatActivity {
    private String role = "";
    private final List<DocumentSnapshot> records = new ArrayList<>();
    private ListenerRegistration listener;
    private RecordAdapter adapter;
    private TextView txtEmpty;
    private RecyclerView rcvRecords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.requireLogin(this);
        if (App.user() == null) return;
        setContentView(R.layout.activity_records);
        App.applyInsets(this);
        mapping();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        adapter = new RecordAdapter(records, this::showRecord);
        rcvRecords.setLayoutManager(new LinearLayoutManager(this));
        rcvRecords.setAdapter(adapter);
        loadRecords();
    }

    private void mapping() {
        txtEmpty = findViewById(R.id.txtEmpty);
        rcvRecords = findViewById(R.id.recycler);
    }

    private void loadRecords() {
        App.db().collection("users").document(App.uid()).get()
                .addOnSuccessListener(user -> {
                    role = App.str(user, "role");
                    if (!"doctor".equals(role) && !"patient".equals(role)) {
                        App.logout(this);
                        return;
                    }
                    adapter.setRole(role);
                    String key = "doctor".equals(role) ? "doctorId" : "patientId";
                    listener = App.db().collection("medical_records")
                            .whereEqualTo(key, App.uid())
                            .addSnapshotListener((snapshot, error) -> {
                                if (error != null) {
                                    txtEmpty.setText("Lỗi tải bệnh án: " + error.getLocalizedMessage());
                                    return;
                                }
                                records.clear();
                                if (snapshot != null) {
                                    for (QueryDocumentSnapshot record : snapshot) records.add(record);
                                }
                                Collections.sort(records, (a, b) -> {
                                    Timestamp at = a.getTimestamp("visitAt");
                                    Timestamp bt = b.getTimestamp("visitAt");
                                    if (at == null || bt == null) return 0;
                                    return bt.compareTo(at);
                                });
                                txtEmpty.setText(records.isEmpty()
                                        ? "Chưa có bệnh án" : "Số bệnh án: " + records.size());
                                adapter.notifyDataSetChanged();
                            });
                })
                .addOnFailureListener(error -> App.error(this, error));
    }

    private void showRecord(DocumentSnapshot document) {
        App.db().collection("treatment_plans").document(document.getId()).get()
                .addOnSuccessListener(plan -> {
                    String detail = "Bệnh nhân: " + App.str(document, "patientName")
                            + "\nBác sĩ: " + App.str(document, "doctorName")
                            + "\nNgày khám: " + App.date(document.getTimestamp("visitAt"))
                            + "\n\nCHẨN ĐOÁN\n" + App.str(document, "diagnosis")
                            + "\n\nGHI CHÚ\n" + App.str(document, "notes")
                            + "\n\nĐƠN THUỐC & LỊCH UỐNG\n";
                    Object raw = plan.get("items");
                    if (raw instanceof List && !((List<?>) raw).isEmpty()) {
                        @SuppressWarnings("unchecked") List<Object> items = (List<Object>) raw;
                        StringBuilder rx = new StringBuilder();
                        for (Object obj : items) {
                            if (!(obj instanceof java.util.Map)) continue;
                            @SuppressWarnings("unchecked") java.util.Map<String, Object> m =
                                    (java.util.Map<String, Object>) obj;
                            rx.append("\n• ").append(String.valueOf(m.get("name")))
                                    .append(" — SL: ").append(String.valueOf(m.get("quantity")))
                                    .append(" ").append(String.valueOf(m.get("unit")))
                                    .append("\n  Liều: ").append(String.valueOf(m.get("dose")))
                                    .append("; ").append(String.valueOf(m.get("schedule")))
                                    .append("; ").append(String.valueOf(m.get("days"))).append(" ngày");
                            Object linePrice = m.get("lineTotalVnd");
                            if (linePrice instanceof Number)
                                rx.append("\n  Thành tiền: ")
                                        .append(PriceText.vnd(((Number) linePrice).longValue()));
                            String instruction = m.get("instruction") instanceof String
                                    ? (String) m.get("instruction") : "";
                            if (!instruction.isEmpty()) rx.append("\n  Dặn dò: ").append(instruction);
                        }
                        detail += rx.toString();
                        Object total = plan.get("totalAmountVnd");
                        if (total instanceof Number) detail += "\n\nTổng tiền thuốc: "
                                + PriceText.vnd(((Number) total).longValue());
                    } else if (plan.exists() && !App.str(plan, "medicine").isEmpty()) {
                        detail += "Tên thuốc: " + App.str(plan, "medicine")
                                + "\nCách dùng: " + App.str(plan, "method")
                                + "\nSố lần/ngày: " + plan.getLong("timesPerDay")
                                + "\nMục đích: " + App.str(plan, "purpose")
                                + "\nHướng dẫn: " + App.str(plan, "guide");
                    } else {
                        detail += "Không kê thuốc trong lượt khám này";
                    }
                    AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                            .setTitle("Chi tiết bệnh án")
                            .setMessage(detail)
                            .setNegativeButton("Đóng", null);
                    if ("doctor".equals(role)) {
                        dialog.setPositiveButton("Chỉnh sửa", (d, which) -> {
                            Intent intent = new Intent(this, RecordEditorActivity.class);
                            intent.putExtra("appointmentId", document.getId());
                            startActivity(intent);
                        });
                    }
                    dialog.show();
                }).addOnFailureListener(error -> App.error(this, error));
    }

    @Override
    protected void onDestroy() {
        if (listener != null) listener.remove();
        super.onDestroy();
    }
}
