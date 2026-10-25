package com.example.btl_android;

import android.app.AlertDialog;
import android.graphics.Paint;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MedicationActivity extends AppCompatActivity {

    /** Wrapper gộp thuốc tự ghi lẫn thuốc bác sĩ kê */
    private static class MedItem {
        DocumentSnapshot doc; // chỉ có khi tự ghi
        String name, dosage, timeLabel, timeKey, frequency;
        boolean fromDoctor;
        String doctorName;

        MedItem(DocumentSnapshot d) {
            this.doc = d;
            name = nvl(d.getString("name"));
            dosage = nvl(d.getString("dosage"));
            timeLabel = nvl(d.getString("timeLabel"));
            timeKey = nvl(d.getString("timeOfDay"));
            frequency = nvl(d.getString("frequency"));
            fromDoctor = false;
        }

        MedItem(String name, String dosage, String method, String frequency, String doctorName) {
            this.name = name;
            this.dosage = dosage.isEmpty() ? method : dosage;
            this.timeKey = "";
            this.timeLabel = method;
            this.frequency = frequency;
            this.fromDoctor = true;
            this.doctorName = doctorName;
        }

        static String nvl(String s) {
            return s != null ? s : "";
        }
    }

    private String patientId, appointmentId;
    private boolean doctorMode;
    private final List<MedItem> allMeds = new ArrayList<>();
    private final List<MedItem> filtered = new ArrayList<>();
    private MedicationAdapter adapter;
    private ListenerRegistration listener;
    private TextView txtEmpty, txtTodayProgress, txtPercent;
    private String currentFilter = "ALL";
    private static final String TODAY = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.requireLogin(this);
        if (App.user() == null)
            return;
        setContentView(R.layout.activity_medication);
        App.applyInsets(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        patientId = getIntent().getStringExtra("patientId");
        appointmentId = getIntent().getStringExtra("appointmentId");
        doctorMode = patientId != null && !patientId.isEmpty() && !patientId.equals(App.uid());
        if (!doctorMode)
            patientId = App.uid();
        txtEmpty = findViewById(R.id.txtEmpty);
        txtTodayProgress = findViewById(R.id.txtTodayProgress);
        txtPercent = findViewById(R.id.txtPercent);

        RecyclerView rv = findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicationAdapter();
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        if (doctorMode) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(v -> showAddDialog());
        } else {
            fab.setVisibility(View.GONE);
        }

        setupTabFilter(R.id.tabAll, "ALL");
        setupTabFilter(R.id.tabMorning, "MORNING");
        setupTabFilter(R.id.tabNoon, "NOON");
        setupTabFilter(R.id.tabEvening, "EVENING");

        loadMedications();
    }

    private void setupTabFilter(int viewId, String filter) {
        TextView tab = findViewById(viewId);
        tab.setOnClickListener(v -> {
            currentFilter = filter;
            applyFilter();
            int[] tabs = { R.id.tabAll, R.id.tabMorning, R.id.tabNoon, R.id.tabEvening };
            for (int id : tabs) {
                TextView t = findViewById(id);
                t.setTextColor(0xFF6B7280);
                t.setBackgroundResource(0);
            }
            tab.setTextColor(0xFF0D6EFD);
            tab.setBackgroundResource(R.drawable.bg_icon_circle_blue);
        });
    }

    private void loadMedications() {
        // 1. Thuốc bệnh nhân tự ghi (realtime)
        com.google.firebase.firestore.Query query = App.db().collection("medications")
                .document(patientId).collection("list");
        if (doctorMode)
            query = query.whereEqualTo("appointmentId", appointmentId);
        listener = query.addSnapshotListener((snap, e) -> {
            if (e != null) {
                App.error(this, e);
                return;
            }
            allMeds.removeIf(m -> !m.fromDoctor);
            if (snap != null)
                for (QueryDocumentSnapshot d : snap)
                    allMeds.add(0, new MedItem(d));
            applyFilter();
            updateProgress();
        });

        // 2. Thuốc bác sĩ kê từ treatment_plans (one-time load)
        com.google.firebase.firestore.Query prescribed = App.db().collection("treatment_plans")
                .whereEqualTo("patientId", patientId);
        if (doctorMode)
            prescribed = prescribed.whereEqualTo("doctorId", App.uid());
        prescribed.get()
                .addOnSuccessListener(snap -> {
                    allMeds.removeIf(m -> m.fromDoctor);
                    if (snap != null) {
                        for (QueryDocumentSnapshot d : snap) {
                            String medicine = d.getString("medicine");
                            if (medicine == null || medicine.isEmpty())
                                continue;
                            String method = MedItem.nvl(d.getString("method"));
                            Long times = d.getLong("timesPerDay");
                            String freq = (times != null ? times + " lần/ngày" : "")
                                    + (method.isEmpty() ? "" : " – " + method);
                            String guide = MedItem.nvl(d.getString("guide"));
                            String drName = "BS. " + MedItem.nvl(d.getString("doctorName"));
                            allMeds.add(new MedItem(medicine, guide, method, freq, drName));
                        }
                    }
                    applyFilter();
                    updateProgress();
                });
    }

    private void applyFilter() {
        filtered.clear();
        for (MedItem m : allMeds) {
            // Thuốc bác sĩ kê luôn hiển thị trong tab ALL, ẩn ở các tab buổi
            if (m.fromDoctor) {
                if ("ALL".equals(currentFilter))
                    filtered.add(m);
            } else {
                if ("ALL".equals(currentFilter) || currentFilter.equals(m.timeKey))
                    filtered.add(m);
            }
        }
        txtEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void updateProgress() {
        long total = 0, done = 0;
        for (MedItem m : allMeds) {
            if (m.fromDoctor)
                continue;
            total++;
            if (m.doc != null && Boolean.TRUE.equals(m.doc.getBoolean("doneToday_" + TODAY)))
                done++;
        }
        txtTodayProgress.setText(done + "/" + total + " liều đã uống");
        int pct = total == 0 ? 0 : (int) (done * 100 / total);
        txtPercent.setText(pct + "%");
    }

    private static final String[] TIME_LABELS = {
            "Buổi sáng (Morning)", "Buổi trưa (Noon)", "Buổi tối (Evening)" };
    private static final String[] TIME_KEYS = { "MORNING", "NOON", "EVENING" };

    private void setupTimeSpinner(Spinner spinner) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, TIME_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private boolean validateData(View view) {
        EditText edtName = view.findViewById(R.id.edtMedName);
        TextInputLayout tilName = view.findViewById(R.id.tilMedName);
        tilName.setError(null);
        if (App.value(edtName).isEmpty()) {
            tilName.setError("Vui lòng nhập tên thuốc");
            return false;
        }
        return true;
    }

    private void showAddDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_medication, null);
        EditText edtName = view.findViewById(R.id.edtMedName);
        EditText edtDosage = view.findViewById(R.id.edtDosage);
        EditText edtFrequency = view.findViewById(R.id.edtFrequency);
        Spinner spinner = view.findViewById(R.id.spinnerTime);
        setupTimeSpinner(spinner);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Thêm thuốc")
                .setView(view)
                .setPositiveButton("Thêm", null)
                .setNegativeButton("Hủy", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(
                android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(button -> {
                    if (!validateData(view))
                        return;
                    String name = App.value(edtName);
                    int pos = spinner.getSelectedItemPosition();
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", name);
                    data.put("dosage", App.value(edtDosage).isEmpty() ? "Theo chỉ dẫn bác sĩ" : App.value(edtDosage));
                    data.put("timeOfDay", TIME_KEYS[pos]);
                    data.put("timeLabel", TIME_LABELS[pos]);
                    data.put("frequency", App.value(edtFrequency).isEmpty() ? "Hàng ngày" : App.value(edtFrequency));
                    data.put("createdAt", FieldValue.serverTimestamp());
                    data.put("createdBy", App.uid());
                    if (doctorMode)
                        data.put("appointmentId", appointmentId);
                    App.db().collection("medications").document(patientId)
                            .collection("list").add(data)
                            .addOnSuccessListener(ref -> App.note(this, "Đã thêm " + name))
                            .addOnFailureListener(error -> App.error(this, error));
                    dialog.dismiss();
                }));
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        if (listener != null)
            listener.remove();
        super.onDestroy();
    }

    // ──────────────────────────────────────────────────────────────────
    private class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.Holder> {
        class Holder extends RecyclerView.ViewHolder {
            TextView chk, name, dosage, time, freq, del, badge;

            Holder(View v) {
                super(v);
                chk = v.findViewById(R.id.chkDone);
                name = v.findViewById(R.id.txtMedName);
                dosage = v.findViewById(R.id.txtDosage);
                time = v.findViewById(R.id.txtTime);
                freq = v.findViewById(R.id.txtFrequency);
                del = v.findViewById(R.id.btnDelete);
                badge = v.findViewById(R.id.txtBadge); // nullable – chỉ trong layout mới
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new Holder(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_medication, p, false));
        }

        @Override
        public int getItemCount() {
            return filtered.size();
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int pos) {
            MedItem m = filtered.get(pos);
            h.name.setText(m.name);
            h.dosage.setText(m.dosage.isEmpty() ? "Theo chỉ dẫn bác sĩ" : m.dosage);
            h.freq.setText(m.frequency);

            if (m.fromDoctor) {
                h.time.setText(m.doctorName != null ? m.doctorName : "Bác sĩ kê đơn");
                if (h.badge != null) {
                    h.badge.setVisibility(View.VISIBLE);
                    h.badge.setText("🩺 Bác sĩ kê");
                }
                h.chk.setText("📋");
                h.chk.setOnClickListener(null);
                h.name.setPaintFlags(h.name.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                if (h.del != null)
                    h.del.setVisibility(View.INVISIBLE);
                h.itemView.setOnLongClickListener(null);
            } else {
                String lbl = m.timeLabel;
                h.time.setText(lbl.replace(" (Morning)", "").replace(" (Noon)", "").replace(" (Evening)", ""));
                if (h.badge != null)
                    h.badge.setVisibility(View.GONE);

                Boolean doneFlag = m.doc != null ? m.doc.getBoolean("doneToday_" + TODAY) : null;
                boolean isDone = Boolean.TRUE.equals(doneFlag);
                h.chk.setText(isDone ? "✅" : "⬜");
                h.name.setPaintFlags(isDone
                        ? h.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                        : h.name.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

                h.chk.setOnClickListener(doctorMode ? null : v -> {
                    if (m.doc == null)
                        return;
                    Map<String, Object> upd = new HashMap<>();
                    upd.put("doneToday_" + TODAY, !isDone);
                    m.doc.getReference().update(upd).addOnSuccessListener(x -> updateProgress());
                });

                if (h.del != null) {
                    h.del.setVisibility(doctorMode ? View.VISIBLE : View.GONE);
                    if (doctorMode) {
                        h.del.setOnClickListener(v -> new AlertDialog.Builder(MedicationActivity.this)
                                .setMessage("Xóa thuốc " + m.name + "?")
                                .setPositiveButton("Xóa", (dlg, w) -> {
                                    if (m.doc != null)
                                        m.doc.getReference().delete()
                                                .addOnSuccessListener(x -> App.note(MedicationActivity.this, "Đã xóa"));
                                })
                                .setNegativeButton("Hủy", null).show());
                    }
                }

                if (doctorMode) {
                    h.itemView.setOnLongClickListener(v -> {
                        showEditDialog(m.doc);
                        return true;
                    });
                } else {
                    h.itemView.setOnLongClickListener(null);
                }
            }
        }
    }

    private void showEditDialog(DocumentSnapshot document) {
        if (document == null)
            return;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_medication, null);
        EditText edtName = view.findViewById(R.id.edtMedName);
        EditText edtDosage = view.findViewById(R.id.edtDosage);
        EditText edtFrequency = view.findViewById(R.id.edtFrequency);
        Spinner spinner = view.findViewById(R.id.spinnerTime);

        edtName.setText(document.getString("name"));
        edtDosage.setText(document.getString("dosage"));
        edtFrequency.setText(document.getString("frequency"));
        setupTimeSpinner(spinner);
        for (int i = 0; i < TIME_KEYS.length; i++) {
            if (TIME_KEYS[i].equals(document.getString("timeOfDay"))) {
                spinner.setSelection(i);
                break;
            }
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Sửa thông tin thuốc")
                .setView(view)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(
                android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(button -> {
                    if (!validateData(view))
                        return;
                    String name = App.value(edtName);
                    int pos = spinner.getSelectedItemPosition();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", name);
                    updates.put("dosage",
                            App.value(edtDosage).isEmpty() ? "Theo chỉ dẫn bác sĩ" : App.value(edtDosage));
                    updates.put("timeOfDay", TIME_KEYS[pos]);
                    updates.put("timeLabel", TIME_LABELS[pos]);
                    updates.put("frequency", App.value(edtFrequency).isEmpty() ? "Hàng ngày" : App.value(edtFrequency));
                    document.getReference().update(updates)
                            .addOnSuccessListener(ref -> App.note(this, "Đã cập nhật " + name))
                            .addOnFailureListener(error -> App.error(this, error));
                    dialog.dismiss();
                }));
        dialog.show();
    }

}
