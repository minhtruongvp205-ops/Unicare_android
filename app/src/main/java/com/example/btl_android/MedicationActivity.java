package com.example.btl_android;

import android.app.AlertDialog;
import android.graphics.Paint;
import android.os.Bundle;
import com.google.firebase.Timestamp;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
        final List<String> timeKeys = new ArrayList<>();
        String priceLabel = "";
        String diagnosis = "", visitAt = "", notes = "";
        String appointmentId = "", instruction = "", dose = "", schedule = "";
        String quantity = "", unit = "", days = "", orderStatus = "";
        long unitPriceVnd = -1, lineTotalVnd = -1;


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

        /** Một thuốc do bác sĩ kê, gắn với đúng lịch khám và phiếu yêu cầu thanh toán. */
        MedItem(Map<String, Object> item, String doctorName, boolean demoPrice) {
            this.doc = null;
            this.fromDoctor = true;
            this.doctorName = normalizeDoctor(doctorName);
            this.name = value(item.get("name"));
            this.dose = value(item.get("dose"));
            this.instruction = value(item.get("instruction"));
            this.dosage = dose.isEmpty() ? "Chưa có liều dùng" : "Liều dùng: " + dose;
            this.schedule = value(item.get("schedule"));
            this.timeLabel = schedule;
            this.timeKey = "";
            this.days = value(item.get("days"));
            this.quantity = value(item.get("quantity"));
            this.unit = value(item.get("unit"));
            this.frequency = (days.isEmpty() ? "" : days + " ngày")
                    + (quantity.isEmpty() ? "" : " • SL: " + quantity + " " + unit);
            Object keys = item.get("timeKeys");
            if (keys instanceof List) {
                for (Object key : (List<?>) keys) {
                    if (key instanceof String) timeKeys.add((String) key);
                }
            }
            if (timeKeys.isEmpty()) {
                if (schedule.contains("Sáng")) timeKeys.add("MORNING");
                if (schedule.contains("Trưa")) timeKeys.add("NOON");
                if (schedule.contains("Tối")) timeKeys.add("EVENING");
            }
            if (item.get("unitPriceVnd") instanceof Number) {
                unitPriceVnd = ((Number) item.get("unitPriceVnd")).longValue();
            }
            if (item.get("lineTotalVnd") instanceof Number) {
                lineTotalVnd = ((Number) item.get("lineTotalVnd")).longValue();
                priceLabel = PriceText.vnd(lineTotalVnd);
            }
        }

        static String value(Object o) { return o == null ? "" : o.toString(); }
        static String normalizeDoctor(String name) {
            String trimmed = nvl(name).trim();
            if (trimmed.isEmpty()) return "Chưa có thông tin bác sĩ";
            return trimmed.startsWith("BS.") ? trimmed : "BS. " + trimmed;
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
    private ListenerRegistration listener, ordersListener, plansListener, recordsListener;
    private final Set<String> requestedAppointmentIds = new HashSet<>();
    private final Map<String, String> orderStatuses = new HashMap<>();
    private final List<DocumentSnapshot> patientPlans = new ArrayList<>();
    private final Map<String, DocumentSnapshot> medicalRecords = new HashMap<>();
    private boolean ordersLoaded, plansLoaded, recordsLoaded;
    private String loadError = "";

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

        if (doctorMode) {
            loadDoctorMedications();
        } else {
            txtEmpty.setVisibility(View.VISIBLE);
            txtEmpty.setText("Đang tải phiếu yêu cầu thanh toán và đơn thuốc...");
            loadPatientMedications();
        }
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

    /** Bệnh nhân chỉ thấy đơn thuốc sau khi tự tạo phiếu yêu cầu thanh toán. */
    private void loadPatientMedications() {
        ordersListener = App.db().collection("orders")
                .whereEqualTo("patientId", patientId)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        showLoadError("Không đọc được phiếu yêu cầu thanh toán", error);
                        return;
                    }
                    requestedAppointmentIds.clear();
                    orderStatuses.clear();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String id = doc.getString("appointmentId");
                            if (id != null && !id.isEmpty()) {
                                requestedAppointmentIds.add(id);
                                String status = doc.getString("status");
                                orderStatuses.put(id, "PENDING".equals(status)
                                        ? "Phiếu yêu cầu: Chờ thanh toán" : "Trạng thái phiếu: " + MedItem.nvl(status));
                            }
                        }
                    }
                    ordersLoaded = true;
                    loadError = "";
                    rebuildPatientMedications();
                });

        plansListener = App.db().collection("treatment_plans")
                .whereEqualTo("patientId", patientId)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        showLoadError("Không đọc được đơn thuốc", error);
                        return;
                    }
                    patientPlans.clear();
                    if (snap != null) patientPlans.addAll(snap.getDocuments());
                    plansLoaded = true;
                    rebuildPatientMedications();
                });

        recordsListener = App.db().collection("medical_records")
                .whereEqualTo("patientId", patientId)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        showLoadError("Không đọc được chẩn đoán", error);
                        return;
                    }
                    medicalRecords.clear();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            medicalRecords.put(doc.getId(), doc);
                        }
                    }
                    recordsLoaded = true;
                    rebuildPatientMedications();
                });
    }

    private void showLoadError(String label, Exception error) {
        loadError = label + ". Kiểm tra mạng và Firestore Rules.\n" + error.getLocalizedMessage();
        txtEmpty.setVisibility(View.VISIBLE);
        txtEmpty.setText(loadError);
    }

    private void rebuildPatientMedications() {
        if (!ordersLoaded || !plansLoaded || !recordsLoaded) return;
        loadError = "";
        allMeds.clear();
        List<DocumentSnapshot> sorted = new ArrayList<>(patientPlans);
        Collections.sort(sorted, (a, b) -> {
            Timestamp x = a.getTimestamp("createdAt");
            Timestamp y = b.getTimestamp("createdAt");
            if (x == null || y == null) return 0;
            return y.compareTo(x);
        });
        for (DocumentSnapshot plan : sorted) {
            String appointmentId = plan.getId();
            if (!requestedAppointmentIds.contains(appointmentId)) continue;
            addPlanItems(plan, medicalRecords.get(appointmentId), orderStatuses.get(appointmentId));
        }
        applyFilter();
        updateProgress();
    }

    private void addPlanItems(DocumentSnapshot plan, DocumentSnapshot record, String orderStatus) {
        String doctorName = MedItem.normalizeDoctor(plan.getString("doctorName"));
        if ((doctorName.equals("Chưa có thông tin bác sĩ")) && record != null) {
            doctorName = MedItem.normalizeDoctor(record.getString("doctorName"));
        }
        String diagnosis = record != null ? MedItem.nvl(record.getString("diagnosis")) : "";
        String notes = record != null ? MedItem.nvl(record.getString("notes")) : "";
        Timestamp visit = record != null ? record.getTimestamp("visitAt") : null;
        if (visit == null) visit = plan.getTimestamp("createdAt");
        String visitAt = visit == null ? "" : new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(visit.toDate());
        Object raw = plan.get("items");
        boolean hasItems = false;
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) {
                if (!(o instanceof Map)) continue;
                @SuppressWarnings("unchecked") Map<String, Object> item = (Map<String, Object>) o;
                MedItem med = new MedItem(item, doctorName, Boolean.TRUE.equals(plan.getBoolean("isDemoPrice")));
                if (med.name.isEmpty()) continue;
                med.appointmentId = plan.getId();
                med.diagnosis = diagnosis;
                med.notes = notes;
                med.visitAt = visitAt;
                med.orderStatus = orderStatus == null ? "" : orderStatus;
                allMeds.add(med);
                hasItems = true;
            }
        }
        if (!hasItems) {
            String medicine = plan.getString("medicine");
            if (medicine != null && !medicine.trim().isEmpty()) {
                String method = MedItem.nvl(plan.getString("method"));
                Long times = plan.getLong("timesPerDay");
                String freq = (times != null ? times + " lần/ngày" : "")
                        + (method.isEmpty() ? "" : " • " + method);
                MedItem med = new MedItem(medicine, MedItem.nvl(plan.getString("guide")), method, freq, doctorName);
                med.appointmentId = plan.getId();
                med.diagnosis = diagnosis;
                med.notes = notes;
                med.visitAt = visitAt;
                med.orderStatus = orderStatus == null ? "" : orderStatus;
                allMeds.add(med);
            }
        }
    }

    private void loadDoctorMedications() {
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
                            String drName = MedItem.normalizeDoctor(d.getString("doctorName"));
                            Object raw = d.get("items");
                            if (raw instanceof List) {
                                @SuppressWarnings("unchecked") List<Object> items = (List<Object>)raw;
                                if (!items.isEmpty()) {
                                    for (Object obj : items) {
                                        if (!(obj instanceof Map)) continue;
                                        @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) obj;
                                        allMeds.add(new MedItem(map, drName, Boolean.TRUE.equals(d.getBoolean("isDemoPrice"))));
                                    }
                                    continue;
                                }
                            }
                            // Tương thích đơn thuốc cũ, trước khi có danh mục nhiều thuốc.
                            String medicine = d.getString("medicine");
                            if (medicine == null || medicine.isEmpty()) continue;
                            String method = MedItem.nvl(d.getString("method"));
                            Long times = d.getLong("timesPerDay");
                            String freq = (times != null ? times + " lần/ngày" : "")
                                    + (method.isEmpty() ? "" : " – " + method);
                            String guide = MedItem.nvl(d.getString("guide"));
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
            // Thuốc bác sĩ kê hiển thị theo đúng các buổi mà bác sĩ đã lựa chọn.
            if (m.fromDoctor) {
                if ("ALL".equals(currentFilter) || m.timeKeys.contains(currentFilter))
                    filtered.add(m);
            } else {
                if ("ALL".equals(currentFilter) || currentFilter.equals(m.timeKey))
                    filtered.add(m);
            }
        }
        if (!doctorMode && !loadError.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            txtEmpty.setText(loadError);
        } else if (!doctorMode && (!ordersLoaded || !plansLoaded || !recordsLoaded)) {
            txtEmpty.setVisibility(View.VISIBLE);
            txtEmpty.setText("Đang tải phiếu yêu cầu thanh toán và đơn thuốc...");
        } else if (filtered.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            txtEmpty.setText(doctorMode ? "Chưa có thuốc cho lượt khám này."
                    : requestedAppointmentIds.isEmpty()
                      ? "Chưa có thuốc để nhắc. Vào Thanh toán, chọn đơn thuốc và nhấn tạo phiếu yêu cầu thanh toán trước."
                      : "Các phiếu đã tạo chưa có thuốc ở mục này.");
        } else {
            txtEmpty.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    private void updateProgress() {
        if (!doctorMode) {
            txtTodayProgress.setText(allMeds.size() + " loại thuốc thuộc "
                    + requestedAppointmentIds.size() + " phiếu yêu cầu thanh toán");
            txtPercent.setText(requestedAppointmentIds.size() + " phiếu");
            return;
        }
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
        if (listener != null) listener.remove();
        if (ordersListener != null) ordersListener.remove();
        if (plansListener != null) plansListener.remove();
        if (recordsListener != null) recordsListener.remove();
        super.onDestroy();
    }

    // ──────────────────────────────────────────────────────────────────
    private class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.Holder> {
        class Holder extends RecyclerView.ViewHolder {
            TextView chk, name, dosage, time, freq, del, badge, diagnosis, hint;

            Holder(View v) {
                super(v);
                chk = v.findViewById(R.id.chkDone);
                name = v.findViewById(R.id.txtMedName);
                dosage = v.findViewById(R.id.txtDosage);
                time = v.findViewById(R.id.txtTime);
                freq = v.findViewById(R.id.txtFrequency);
                del = v.findViewById(R.id.btnDelete);
                badge = v.findViewById(R.id.txtBadge);
                diagnosis = v.findViewById(R.id.txtDiagnosis);
                hint = v.findViewById(R.id.txtOpenDetails);
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
            h.dosage.setText(m.dosage.isEmpty() ? "Theo hướng dẫn bác sĩ" : m.dosage);
            h.freq.setText(m.frequency + (m.priceLabel.isEmpty() ? "" : " • " + m.priceLabel));
            h.diagnosis.setVisibility(m.fromDoctor ? View.VISIBLE : View.GONE);
            if (m.fromDoctor) {
                h.diagnosis.setText("Chẩn đoán: " + (m.diagnosis.isEmpty() ? "Chưa cập nhật" : m.diagnosis));
                h.time.setText((m.timeLabel.isEmpty() ? "Theo chỉ định" : m.timeLabel)
                        + " • " + m.doctorName);
                h.badge.setVisibility(View.VISIBLE);
                h.chk.setText("💊");
                h.chk.setOnClickListener(v -> showMedicationDetails(m));
                h.name.setPaintFlags(h.name.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                h.del.setVisibility(View.GONE);
                h.hint.setVisibility(View.VISIBLE);
                h.itemView.setOnClickListener(v -> showMedicationDetails(m));
                h.itemView.setOnLongClickListener(null);
            } else {
                h.time.setText(m.timeLabel.replace(" (Morning)", "")
                        .replace(" (Noon)", "").replace(" (Evening)", ""));
                h.badge.setVisibility(View.GONE);
                h.hint.setVisibility(View.GONE);
                h.itemView.setOnClickListener(v -> showMedicationDetails(m));
                Boolean doneFlag = m.doc != null ? m.doc.getBoolean("doneToday_" + TODAY) : null;
                boolean isDone = Boolean.TRUE.equals(doneFlag);
                h.chk.setText(isDone ? "✅" : "⬜");
                h.name.setPaintFlags(isDone
                        ? h.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                        : h.name.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                h.chk.setOnClickListener(doctorMode ? null : v -> {
                    if (m.doc == null) return;
                    Map<String, Object> upd = new HashMap<>();
                    upd.put("doneToday_" + TODAY, !isDone);
                    m.doc.getReference().update(upd)
                            .addOnSuccessListener(x -> updateProgress());
                });
                h.del.setVisibility(doctorMode ? View.VISIBLE : View.GONE);
                if (doctorMode) {
                    h.del.setOnClickListener(v -> new AlertDialog.Builder(MedicationActivity.this)
                            .setMessage("Xóa thuốc " + m.name + "?")
                            .setPositiveButton("Xóa", (dlg, w) -> {
                                if (m.doc != null) m.doc.getReference().delete()
                                        .addOnSuccessListener(x -> App.note(MedicationActivity.this, "Đã xóa"));
                            })
                            .setNegativeButton("Hủy", null).show());
                    h.itemView.setOnLongClickListener(v -> { showEditDialog(m.doc); return true; });
                } else {
                    h.itemView.setOnLongClickListener(null);
                }
            }
        }
    }

    /** Bấm vào bất kỳ dòng thuốc nào để xem đầy đủ kết quả khám và chỉ định. */
    private void showMedicationDetails(MedItem m) {
        StringBuilder sb = new StringBuilder();
        if (m.fromDoctor) {
            sb.append("CHẨN ĐOÁN / BỆNH LÝ\n")
                    .append(m.diagnosis.isEmpty() ? "Chưa có thông tin chẩn đoán" : m.diagnosis)
                    .append("\n\nBÁC SĨ KHÁM\n").append(m.doctorName);
            if (!m.visitAt.isEmpty()) sb.append("\nNgày khám: ").append(m.visitAt);
            if (!m.appointmentId.isEmpty()) sb.append("\nMã lượt khám: ").append(m.appointmentId);
            sb.append("\n\nTHÔNG TIN THUỐC\n")
                    .append("Tên thuốc: ").append(m.name)
                    .append("\nLiều dùng: ").append(m.dose.isEmpty() ? m.dosage : m.dose)
                    .append("\nLịch uống: ").append(m.schedule.isEmpty() ? m.timeLabel : m.schedule);
            if (!m.days.isEmpty()) sb.append("\nSố ngày: ").append(m.days);
            if (!m.quantity.isEmpty()) sb.append("\nSố lượng: ").append(m.quantity).append(" ").append(m.unit);
            if (m.unitPriceVnd >= 0) sb.append("\nĐơn giá: ").append(PriceText.vnd(m.unitPriceVnd));
            if (m.lineTotalVnd >= 0) sb.append("\nThành tiền: ").append(PriceText.vnd(m.lineTotalVnd));
            if (!m.instruction.isEmpty()) sb.append("\nHướng dẫn: ").append(m.instruction);
            if (!m.notes.isEmpty()) sb.append("\n\nDẶN DÒ SAU KHÁM\n").append(m.notes);
            if (!m.orderStatus.isEmpty()) sb.append("\n\n").append(m.orderStatus);
        } else {
            sb.append("Tên thuốc: ").append(m.name)
                    .append("\nLiều dùng: ").append(m.dosage)
                    .append("\nThời điểm: ").append(m.timeLabel)
                    .append("\nTần suất: ").append(m.frequency);
        }
        new AlertDialog.Builder(this)
                .setTitle("Chi tiết thuốc")
                .setMessage(sb.toString())
                .setPositiveButton("Đóng", null)
                .show();
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
