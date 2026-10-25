package com.example.btl_android;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HealthVitalsActivity extends AppCompatActivity {

    private String patientId, appointmentId;
    private boolean doctorMode;
    private final List<DocumentSnapshot> vitals = new ArrayList<>();
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

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        if (doctorMode) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> showAddDialog());
        } else {
            fab.setVisibility(View.GONE);
        }

        loadVitals();
    }

    private void loadVitals() {
        com.google.firebase.firestore.Query query = App.db().collection("health_vitals")
                .document(patientId).collection("records");
        if (doctorMode) query = query.whereEqualTo("appointmentId", appointmentId);
        listener = query.addSnapshotListener((snap, e) -> {
                    if (e != null) { App.error(this,e); return; }
                    vitals.clear();
                    if (snap != null) for (QueryDocumentSnapshot d : snap) vitals.add(d);
                    Collections.sort(vitals, (a, b) -> {
                        Timestamp at = a.getTimestamp("recordedAt"), bt = b.getTimestamp("recordedAt");
                        if (at == null || bt == null) return 0;
                        return bt.compareTo(at);
                    });
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

    // Material 3 dialog: khi validateData() sai, hộp thoại không tự đóng.
    private void showAddDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_vital, null);
        Spinner spinner = view.findViewById(R.id.spinnerType);
        EditText edtValue = view.findViewById(R.id.edtValue);
        EditText edtNote = view.findViewById(R.id.edtNote);
        TextInputLayout tilValue = view.findViewById(R.id.tilValue);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, TYPES);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Thêm chỉ số sức khỏe")
                .setView(view)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(
                android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(button -> {
            if (!validateData(edtValue, tilValue)) return;
            int position = spinner.getSelectedItemPosition();
            Map<String, Object> data = new HashMap<>();
            data.put("type", TYPE_KEYS[position]);
            data.put("label", TYPES[position]);
            data.put("value", App.value(edtValue).replace(',', '.'));
            data.put("icon", ICONS[position]);
            data.put("note", App.value(edtNote));
            data.put("recordedAt", FieldValue.serverTimestamp());
            data.put("createdBy", App.uid());
            if (doctorMode) data.put("appointmentId", appointmentId);
            App.db().collection("health_vitals").document(patientId)
                    .collection("records").add(data)
                    .addOnSuccessListener(ref -> App.note(this, "Đã lưu chỉ số"))
                    .addOnFailureListener(error -> App.error(this, error));
            dialog.dismiss();
        }));
        dialog.show();
    }

    private boolean validateData(EditText edtValue, TextInputLayout tilValue) {
        tilValue.setError(null);
        String text = App.value(edtValue).replace(',', '.');
        if (text.isEmpty()) {
            tilValue.setError("Vui lòng nhập chỉ số sức khỏe");
            return false;
        }
        try {
            double value = Double.parseDouble(text);
            if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0) {
                tilValue.setError("Giá trị phải là số lớn hơn 0");
                return false;
            }
        } catch (NumberFormatException e) {
            tilValue.setError("Vui lòng nhập số hợp lệ");
            return false;
        }
        return true;
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
            } else if ("glucose".equals(typeKey)) {
                try { int v = Integer.parseInt(val); warning = v < 70 || v > 140; } catch (Exception ignored) {}
            }
            if (warning) {
                h.status.setText("⚠️ Chú ý");
                h.status.setTextColor(0xFFF59E0B);
                h.status.setBackgroundResource(R.drawable.bg_badge_amber);
            } else {
                h.status.setText("✓ Bình thường");
                h.status.setTextColor(0xFF10B981);
                h.status.setBackgroundResource(R.drawable.bg_badge_green);
            }

            // Xóa chỉ số (Chỉ bác sĩ mới được xóa)
            if (h.del != null) h.del.setVisibility(doctorMode ? View.VISIBLE : View.GONE);
            if (h.del != null && doctorMode) {
                h.del.setOnClickListener(v -> new AlertDialog.Builder(HealthVitalsActivity.this)
                        .setTitle("🗑️ Xóa chỉ số")
                        .setMessage("Xóa bản ghi " + (label != null ? label : "này") + "?")
                        .setPositiveButton("Xóa", (dlg, w) -> d.getReference().delete()
                                .addOnSuccessListener(x -> App.note(HealthVitalsActivity.this, "Đã xóa chỉ số"))
                                .addOnFailureListener(e -> App.error(HealthVitalsActivity.this, e)))
                        .setNegativeButton("Hủy", null).show());
            }
        }
    }
}
