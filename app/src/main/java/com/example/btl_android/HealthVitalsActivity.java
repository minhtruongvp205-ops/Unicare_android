package com.example.btl_android;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HealthVitalsActivity extends AppCompatActivity {

    private String patientId, appointmentId;
    private boolean doctorMode;
    // Một loại chỉ số chỉ xuất hiện một lần trong cùng lượt khám.
    // Bản ghi cũ vẫn tồn tại trên Firestore, không tự động xóa dữ liệu cũ.
    private final List<DocumentSnapshot> vitals = new ArrayList<>();
    private final Map<String, DocumentSnapshot> visitByType = new HashMap<>();
    private boolean vitalsLoaded = false;
    private boolean savingVitals = false;
    private FloatingActionButton fabVitals;
    private VitalsAdapter adapter;
    private ListenerRegistration listener;
    private TextView txtHeartRate, txtBloodPressure, txtWeight, txtGlucose, txtEmpty;

    private static final String[] TYPES = {
        "Nhịp tim (bpm)", "Huyết áp tâm thu (mmHg)", "Huyết áp tâm trương (mmHg)",
        "Cân nặng (kg)", "Đường huyết (mg/dL)", "Nhiệt độ (°C)", "SpO2 (%)"
    };
    private static final String[] TYPE_KEYS = {
        "heart_rate", "bp_systolic", "bp_diastolic",
        "weight", "glucose", "temperature", "spo2"
    };
    private static final String[] ICONS = { "❤️", "💉", "💉", "⚖️", "🩸", "🌡️", "💨" };
    // Dải kiểm tra giá trị nhập hợp lý (không dùng để chẩn đoán bệnh).
    private static final double[] MIN_VALUES = { 20, 50, 30, 1, 20, 30, 50 };
    private static final double[] MAX_VALUES = { 250, 260, 180, 400, 600, 45, 100 };
    private static final int[] INPUT_IDS = {
        R.id.edtAllHeartRate, R.id.edtAllSystolic, R.id.edtAllDiastolic,
        R.id.edtAllWeight, R.id.edtAllGlucose, R.id.edtAllTemperature, R.id.edtAllSpo2
    };
    private static final int[] LAYOUT_IDS = {
        R.id.tilAllHeartRate, R.id.tilAllSystolic, R.id.tilAllDiastolic,
        R.id.tilAllWeight, R.id.tilAllGlucose, R.id.tilAllTemperature, R.id.tilAllSpo2
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.requireLogin(this);
        if (App.user() == null) return;
        setContentView(R.layout.activity_health_vitals);
        App.applyInsets(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        patientId = getIntent().getStringExtra("patientId");
        appointmentId = getIntent().getStringExtra("appointmentId");
        doctorMode = patientId != null && !patientId.isEmpty() && !patientId.equals(App.uid());
        if (!doctorMode) patientId = App.uid();
        txtHeartRate = findViewById(R.id.txtHeartRate);
        txtBloodPressure = findViewById(R.id.txtBloodPressure);
        txtWeight = findViewById(R.id.txtWeight);
        txtGlucose = findViewById(R.id.txtGlucose);
        txtEmpty = findViewById(R.id.txtEmpty);

        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VitalsAdapter();
        rv.setAdapter(adapter);

        fabVitals = findViewById(R.id.fabAdd);
        if (doctorMode) {
            fabVitals.setVisibility(View.VISIBLE);
            fabVitals.setEnabled(false); // Chờ tải dữ liệu để không tạo trùng
            fabVitals.setOnClickListener(v -> showAllVitalsDialog());
        } else {
            fabVitals.setVisibility(View.GONE);
        }

        loadVitals();
    }

    private void loadVitals() {
        com.google.firebase.firestore.Query query = App.db().collection("health_vitals")
                .document(patientId).collection("records");
        if (doctorMode) query = query.whereEqualTo("appointmentId", appointmentId);
        listener = query.addSnapshotListener((snap, e) -> {
            if (e != null) {
                App.error(this, e);
                return;
            }
            List<DocumentSnapshot> all = new ArrayList<>();
            if (snap != null) for (QueryDocumentSnapshot doc : snap) all.add(doc);
            Collections.sort(all, (a, b) -> {
                Timestamp at = a.getTimestamp("recordedAt");
                Timestamp bt = b.getTimestamp("recordedAt");
                if (at == null && bt == null) return 0;
                if (at == null) return 1;
                if (bt == null) return -1;
                return bt.compareTo(at);
            });

            // Gom trùng type + appointmentId. Không gộp chỉ số của 2 lịch khám khác nhau.
            // Với dữ liệu cũ chưa có appointmentId, dùng doc ID riêng để giữ lịch sử.
            Map<String, DocumentSnapshot> unique = new LinkedHashMap<>();
            for (DocumentSnapshot doc : all) {
                String type = doc.getString("type");
                if (type == null) continue;
                String visit = doc.getString("appointmentId");
                String key = visit != null && !visit.isEmpty()
                        ? visit + "|" + type : doc.getId();
                if (!unique.containsKey(key)
                        || (visit != null && doc.getId().equals(visit + "_" + type))) {
                    unique.put(key, doc);
                }
            }
            vitals.clear();
            vitals.addAll(unique.values());

            visitByType.clear();
            if (doctorMode) {
                // Nếu bản cũ dùng document ID ngẫu nhiên, sửa ngay bản cũ đó;
                // với dữ liệu mới, luôn ưu tiên ID cố định của lượt khám.
                for (DocumentSnapshot doc : all) {
                    String type = doc.getString("type");
                    if (type == null) continue;
                    DocumentSnapshot previous = visitByType.get(type);
                    if (previous == null || doc.getId().equals(appointmentId + "_" + type)) {
                        visitByType.put(type, doc);
                    }
                }
            }
            vitalsLoaded = true;
            if (doctorMode && fabVitals != null) {
                fabVitals.setEnabled(true);
                boolean hasData = !visitByType.isEmpty();
                fabVitals.setImageResource(hasData
                        ? android.R.drawable.ic_menu_edit : R.drawable.ic_add);
                fabVitals.setContentDescription(hasData ? "Sửa chỉ số" : "Nhập chỉ số");
            }
            updateSummary();
            txtEmpty.setVisibility(vitals.isEmpty() ? View.VISIBLE : View.GONE);
            adapter.notifyDataSetChanged();
        });
    }

    private void updateSummary() {
        // Show latest value for each key type
        Map<String, String> latest = new HashMap<>();
        for (DocumentSnapshot d : vitals) {
            String key = d.getString("type");
            if (key != null && !latest.containsKey(key)) {
                latest.put(key, d.getString("value"));
            }
        }
        txtHeartRate.setText(latest.getOrDefault("heart_rate", "--"));
        String sys = latest.getOrDefault("bp_systolic", "--");
        String dia = latest.getOrDefault("bp_diastolic", "--");
        txtBloodPressure.setText(sys + "/" + dia);
        txtWeight.setText(latest.getOrDefault("weight", "--"));
        txtGlucose.setText(latest.getOrDefault("glucose", "--"));
    }

    // Một lần mở form có thể nhập đầy đủ 7 chỉ số. Nếu đã có dữ liệu,
    // form tự điền giá trị cũ và thao tác tiếp theo là SỬA, không thêm bản ghi.
    private void showAllVitalsDialog() {
        if (!doctorMode || appointmentId == null || appointmentId.trim().isEmpty()) {
            App.note(this, "Hãy mở từ một lịch khám được phân công.");
            return;
        }
        if (!vitalsLoaded || savingVitals) {
            App.note(this, "Đang tải hoặc lưu dữ liệu, vui lòng kiểm tra lại.");
            return;
        }
        final boolean editing = !visitByType.isEmpty();
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_vitals_all, null);
        EditText[] inputs = new EditText[INPUT_IDS.length];
        TextInputLayout[] layouts = new TextInputLayout[LAYOUT_IDS.length];
        String noteValue = "";
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = form.findViewById(INPUT_IDS[i]);
            layouts[i] = form.findViewById(LAYOUT_IDS[i]);
            DocumentSnapshot existing = visitByType.get(TYPE_KEYS[i]);
            if (existing != null) {
                String value = existing.getString("value");
                if (value != null) inputs[i].setText(value);
                if (noteValue.isEmpty() && existing.getString("note") != null) {
                    noteValue = existing.getString("note");
                }
            }
        }
        EditText edtNote = form.findViewById(R.id.edtAllVitalNote);
        edtNote.setText(noteValue);
        TextView txtError = form.findViewById(R.id.txtAllVitalError);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(editing ? "Sửa chỉ số sức khỏe" : "Nhập chỉ số sức khỏe")
                .setView(form)
                .setPositiveButton(editing ? "Lưu thay đổi" : "Lưu tất cả", null)
                .setNegativeButton("Hủy", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(
                android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(button -> {
            if (savingVitals) return;
            txtError.setText("");
            boolean invalid = false;
            int provided = 0;
            String[] values = new String[inputs.length];
            for (int i = 0; i < inputs.length; i++) {
                layouts[i].setError(null);
                String v = App.value(inputs[i]).replace(',', '.');
                values[i] = v;
                if (v.isEmpty()) continue;
                provided++;
                try {
                    double number = Double.parseDouble(v);
                    if (!Double.isFinite(number) || number < MIN_VALUES[i] || number > MAX_VALUES[i]) {
                        layouts[i].setError("Vui lòng kiểm tra lại giá trị.");
                        invalid = true;
                    }
                } catch (NumberFormatException ex) {
                    layouts[i].setError("Chỉ nhập số hợp lệ.");
                    invalid = true;
                }
            }
            if (!values[1].isEmpty() && !values[2].isEmpty() && !invalid
                    && Double.parseDouble(values[1]) <= Double.parseDouble(values[2])) {
                layouts[2].setError("Tâm trương phải nhỏ hơn tâm thu.");
                invalid = true;
            }
            if (provided == 0) {
                txtError.setText("Hãy nhập ít nhất một chỉ số.");
                return;
            }
            if (invalid) return;
            WriteBatch batch = App.db().batch();
            String note = App.value(edtNote);
            int changes = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i].isEmpty()) {
                    // Ô trống = chưa đo, không tự động xóa chỉ số cũ.
                    continue;
                }
                DocumentSnapshot existing = visitByType.get(TYPE_KEYS[i]);
                if (existing == null) {
                    // Document ID cố định để bấm lưu nhiều lần không tạo thêm bản ghi.
                    DocumentReference record = App.db().collection("health_vitals")
                            .document(patientId).collection("records")
                            .document(appointmentId + "_" + TYPE_KEYS[i]);
                    Map<String, Object> data = new HashMap<>();
                    data.put("type", TYPE_KEYS[i]);
                    data.put("label", TYPES[i]);
                    data.put("value", values[i]);
                    data.put("icon", ICONS[i]);
                    data.put("note", note);
                    data.put("recordedAt", FieldValue.serverTimestamp());
                    data.put("createdBy", App.uid());
                    data.put("appointmentId", appointmentId);
                    batch.set(record, data);
                    changes++;
                } else {
                    String oldValue = existing.getString("value");
                    String oldNote = existing.getString("note");
                    if (!values[i].equals(oldValue) || !note.equals(oldNote == null ? "" : oldNote)) {
                        // Sửa trên document hiện có, kể cả document cũ ID ngẫu nhiên.
                        // Giữ nguyên recordedAt và createdBy.
                        batch.update(existing.getReference(), "value", values[i], "note", note);
                        changes++;
                    }
                }
            }
            if (changes == 0) {
                App.note(this, "Không có thay đổi mới.");
                dialog.dismiss();
                return;
            }
            savingVitals = true;
            button.setEnabled(false);
            txtError.setText("Đang lưu các chỉ số...");
            batch.commit()
                    .addOnSuccessListener(unused -> {
                        savingVitals = false;
                        App.note(this, editing ? "Đã cập nhật chỉ số." : "Đã lưu chỉ số.");
                        dialog.dismiss();
                    })
                    .addOnFailureListener(error -> {
                        savingVitals = false;
                        button.setEnabled(true);
                        txtError.setText("Không lưu được: " + error.getLocalizedMessage()
                                + "\nKiểm tra quyền Firestore của health_vitals.");
                    });
        }));
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        if (listener != null) listener.remove();
        super.onDestroy();
    }

    private class VitalsAdapter extends RecyclerView.Adapter<VitalsAdapter.Holder> {
        class Holder extends RecyclerView.ViewHolder {
            TextView icon, type, value, date, note, status, del;
            Holder(View v) {
                super(v);
                icon = v.findViewById(R.id.txtIcon);
                type = v.findViewById(R.id.txtType);
                value = v.findViewById(R.id.txtValue);
                date = v.findViewById(R.id.txtDate);
                note = v.findViewById(R.id.txtNote);
                status = v.findViewById(R.id.txtStatus);
                del = v.findViewById(R.id.btnDelete);
            }
        }

        @NonNull @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_vital, p, false));
        }

        @Override public int getItemCount() { return vitals.size(); }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int pos) {
            DocumentSnapshot d = vitals.get(pos);
            String typeKey = d.getString("type");
            String label = d.getString("label");
            String ic = d.getString("icon");
            String val = d.getString("value");
            String noteStr = d.getString("note");

            h.icon.setText(ic != null ? ic : "📊");
            h.type.setText(label != null ? label : "Chỉ số");
            h.value.setText(val != null ? val : "--");
            h.date.setText(App.date(d.getTimestamp("recordedAt")));
            h.note.setText(noteStr != null && !noteStr.isEmpty() ? noteStr : "");
            h.note.setVisibility(noteStr != null && !noteStr.isEmpty() ? View.VISIBLE : View.GONE);

            // Simple range check for common vitals
            boolean warning = false;
            if ("heart_rate".equals(typeKey)) {
                try { int v = Integer.parseInt(val); warning = v < 60 || v > 100; } catch (Exception ignored) {}
            } else if ("bp_systolic".equals(typeKey)) {
                try { int v = Integer.parseInt(val); warning = v < 90 || v > 140; } catch (Exception ignored) {}
            } else if ("bp_diastolic".equals(typeKey)) {
                try { double v = Double.parseDouble(val); warning = v < 50 || v > 110; } catch (Exception ignored) {}
            } else if ("glucose".equals(typeKey)) {
                try { double v = Double.parseDouble(val); warning = v < 70 || v > 140; } catch (Exception ignored) {}
            } else if ("spo2".equals(typeKey)) {
                try { double v = Double.parseDouble(val); warning = v < 95 || v > 100; } catch (Exception ignored) {}
            } else if ("temperature".equals(typeKey)) {
                try { double v = Double.parseDouble(val); warning = v < 35 || v > 38; } catch (Exception ignored) {}
            }
            if (warning) {
                h.status.setText("⚠️ Kiểm tra");
                h.status.setTextColor(0xFFF59E0B);
                h.status.setBackgroundResource(R.drawable.bg_badge_amber);
            } else {
                // Không kết luận "bình thường" chỉ từ một chỉ số đơn lẻ.
                h.status.setText("✓ Đã ghi nhận");
                h.status.setTextColor(0xFF10B981);
                h.status.setBackgroundResource(R.drawable.bg_badge_green);
            }

            // Chỉ sửa: không có nút xóa để tránh bác sĩ tạo lại 2–3 lần.
            if (h.del != null) {
                h.del.setVisibility(doctorMode ? View.VISIBLE : View.GONE);
                if (doctorMode) {
                    h.del.setText("Sửa");
                    h.del.setContentDescription("Sửa " + (label == null ? "chỉ số" : label));
                    h.del.setTextColor(0xFF0D6EFD);
                    h.del.setBackgroundResource(R.drawable.bg_badge_green);
                    h.del.setOnClickListener(v -> showAllVitalsDialog());
                }
            }
            h.itemView.setOnClickListener(doctorMode ? v -> showAllVitalsDialog() : null);
        }
    }
}
