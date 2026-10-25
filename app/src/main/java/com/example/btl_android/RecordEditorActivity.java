package com.example.btl_android;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Một lượt khám: thông tin bệnh nhân -> chỉ số -> chẩn đoán -> đơn thuốc -> hoàn tất.
 *  Lưu bệnh án và đơn thuốc cùng lúc để ứng dụng bệnh nhân đọc được.
 */
public class RecordEditorActivity extends AppCompatActivity {
    private String appointmentId;
    private DocumentSnapshot appointment;
    private boolean hasRecord, hasPlan, legacyOnly, originallyHadPricedItems;
    private String oldPrescription = "", oldMedicine = "";

    private TextInputEditText diagnosis, notes;
    private TextInputLayout tilDiagnosis;
    private TextView message, txtTotal, txtVitals, txtCatalog, txtLegacy;
    private MaterialButton save, addMedicine, btnVitals;

    private final List<MedicineCatalog.Medicine> medicines = new ArrayList<>();
    private final List<RxItem> prescriptions = new ArrayList<>();
    private PrescriptionAdapter adapter;
    private boolean usingDemoPrices = true;

    private static class RxItem {
        String id, name, unit, dose, instruction;
        long price;
        int quantity, days;
        boolean morning, noon, evening;

        String schedule() {
            List<String> shifts = new ArrayList<>();
            if (morning) shifts.add("Sáng");
            if (noon) shifts.add("Trưa");
            if (evening) shifts.add("Tối");
            return android.text.TextUtils.join(" • ", shifts);
        }
        int timesPerDay() { return (morning ? 1 : 0) + (noon ? 1 : 0) + (evening ? 1 : 0); }
        Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("medicineId", id);
            m.put("name", name);
            m.put("unit", unit);
            m.put("unitPriceVnd", price);
            m.put("quantity", quantity);
            m.put("lineTotalVnd", price * quantity);
            m.put("dose", dose);
            m.put("days", days);
            m.put("instruction", instruction);
            m.put("schedule", schedule());
            m.put("timesPerDay", timesPerDay());
            List<String> times = new ArrayList<>();
            if (morning) times.add("MORNING");
            if (noon) times.add("NOON");
            if (evening) times.add("EVENING");
            m.put("timeKeys", times);
            return m;
        }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        App.requireLogin(this);
        if (App.user() == null) return;
        setContentView(R.layout.activity_record_editor);
        App.applyInsets(this);
        mapping();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        appointmentId = getIntent().getStringExtra("appointmentId");
        btnVitals.setEnabled(false);
        addMedicine.setEnabled(false);
        save.setEnabled(false);
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            message.setText("Không có mã lượt khám. Vui lòng mở từ Quản lý lịch hẹn.");
            return;
        }
        btnVitals.setOnClickListener(v -> openVitals());
        addMedicine.setOnClickListener(v -> showCatalog());
        save.setOnClickListener(v -> saveRecord());
        MedicineCatalog.load((items, demo) -> {
            medicines.clear();
            medicines.addAll(items);
            usingDemoPrices = demo;
            // Ẩn đoạn hướng dẫn giá thuốc dài trên giao diện phiếu khám.
            txtCatalog.setVisibility(View.GONE);
            if (appointment != null) addMedicine.setEnabled(true);
        });
        App.db().collection("users").document(App.uid()).get()
                .addOnSuccessListener(profile -> {
                    if (!"doctor".equals(App.str(profile, "role"))) {
                        message.setText("Chỉ tài khoản bác sĩ được lập phiếu khám.");
                        return;
                    }
                    loadAppointment();
                })
                .addOnFailureListener(e -> App.error(this, e));
    }

    private void mapping() {
        diagnosis = findViewById(R.id.edtDiagnosis);
        notes = findViewById(R.id.edtNotes);
        tilDiagnosis = findViewById(R.id.tilDiagnosis);
        message = findViewById(R.id.txtMessage);
        txtTotal = findViewById(R.id.txtPrescriptionTotal);
        txtVitals = findViewById(R.id.txtVisitVitals);
        txtCatalog = findViewById(R.id.txtCatalogDisclaimer);
        txtLegacy = findViewById(R.id.txtOldPrescription);
        save = findViewById(R.id.btnSaveRecord);
        addMedicine = findViewById(R.id.btnAddMedicine);
        btnVitals = findViewById(R.id.btnVisitVitals);
        RecyclerView rv = findViewById(R.id.recyclerPrescription);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PrescriptionAdapter();
        rv.setAdapter(adapter);
        refreshPrescription();
    }

    private void loadAppointment() {
        App.db().collection("appointments").document(appointmentId).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists() || !App.uid().equals(App.str(d, "doctorId"))) {
                        message.setText("Bạn không được phân công lịch khám này.");
                        return;
                    }
                    String st = App.str(d, "status");
                    if (!"CONFIRMED".equals(st) && !"COMPLETED".equals(st)) {
                        message.setText("Cần xác nhận lịch trước khi khám.");
                        return;
                    }
                    appointment = d;
                    ((TextView)findViewById(R.id.txtAppointment)).setText(
                            "Họ tên: " + App.str(d, "patientName")
                            + "\nChuyên khoa: " + App.str(d, "specialty")
                            + "\nThời gian: " + App.date(d.getTimestamp("startAt"))
                            + "\nLý do khám: " + App.str(d, "reason"));
                    btnVitals.setEnabled(true);
                    addMedicine.setEnabled(!medicines.isEmpty());
                    loadVitalsSummary();
                    App.db().collection("medical_records").document(appointmentId).get()
                            .addOnSuccessListener(r -> {
                                hasRecord = r.exists();
                                if (r.exists()) {
                                    diagnosis.setText(App.str(r, "diagnosis"));
                                    notes.setText(App.str(r, "notes"));
                                    oldPrescription = App.str(r, "prescription");
                                }
                                loadPrescription();
                            })
                            .addOnFailureListener(e -> message.setText("Lỗi tải bệnh án: " + e.getLocalizedMessage()));
                })
                .addOnFailureListener(e -> message.setText("Lỗi tải lịch khám: " + e.getLocalizedMessage()));
    }

    private void loadPrescription() {
        App.db().collection("treatment_plans").document(appointmentId).get()
                .addOnSuccessListener(plan -> {
                    hasPlan = plan.exists();
                    prescriptions.clear();
                    Object raw = plan.get("items");
                    originallyHadPricedItems = raw instanceof List && !((List<?>) raw).isEmpty();
                    if (raw instanceof List) {
                        for (Object obj : (List<?>) raw) {
                            if (!(obj instanceof Map)) continue;
                            @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) obj;
                            RxItem item = new RxItem();
                            item.id = str(map.get("medicineId"));
                            item.name = str(map.get("name"));
                            item.unit = str(map.get("unit"));
                            item.dose = str(map.get("dose"));
                            item.instruction = str(map.get("instruction"));
                            item.price = num(map.get("unitPriceVnd"));
                            item.quantity = (int) num(map.get("quantity"));
                            item.days = (int) num(map.get("days"));
                            Object keys = map.get("timeKeys");
                            if (keys instanceof List) {
                                item.morning = ((List<?>) keys).contains("MORNING");
                                item.noon = ((List<?>) keys).contains("NOON");
                                item.evening = ((List<?>) keys).contains("EVENING");
                            }
                            if (!item.name.isEmpty()) prescriptions.add(item);
                        }
                    }
                    oldMedicine = App.str(plan, "medicine");
                    if (oldPrescription.isEmpty() && !oldMedicine.isEmpty())
                        oldPrescription = oldMedicine;
                    legacyOnly = prescriptions.isEmpty() && !oldMedicine.isEmpty();
                    // Giữ dữ liệu đơn thuốc cũ trong bộ nhớ, không hiện chú thích dài.
                    txtLegacy.setVisibility(View.GONE);
                    refreshPrescription();
                    save.setEnabled(true);
                    message.setText("");
                })
                .addOnFailureListener(e -> message.setText("Lỗi tải phác đồ: " + e.getLocalizedMessage()));
    }

    private void openVitals() {
        if (appointment == null) return;
        Intent i = new Intent(this, HealthVitalsActivity.class);
        i.putExtra("patientId", App.str(appointment, "patientId"));
        i.putExtra("appointmentId", appointmentId);
        startActivity(i);
    }

    @Override protected void onResume() {
        super.onResume();
        if (appointment != null) loadVitalsSummary();
    }

    private void loadVitalsSummary() {
        String patientId = App.str(appointment, "patientId");
        App.db().collection("health_vitals").document(patientId).collection("records")
                .whereEqualTo("appointmentId", appointmentId).get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        txtVitals.setText("Chưa nhập chỉ số sức khỏe cho lượt khám này.");
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (QueryDocumentSnapshot d : snap) {
                        if (sb.length() > 0) sb.append("  •  ");
                        sb.append(App.str(d, "label")).append(": ").append(App.str(d, "value"));
                    }
                    txtVitals.setText(sb.toString());
                })
                .addOnFailureListener(e -> txtVitals.setText("Chưa đọc được chỉ số: " + e.getLocalizedMessage()));
    }

    /** Chọn nhiều thuốc cùng lúc; chỉ nhập liều sau khi thuốc đã ở danh sách. */
    private void showCatalog() {
        if (medicines.isEmpty()) {
            App.note(this, "Chưa tải được danh mục thuốc.");
            return;
        }
        // Chỉ hiển thị thuốc chưa thêm, tránh thêm trùng vào cùng một đơn.
        List<MedicineCatalog.Medicine> available = new ArrayList<>();
        for (MedicineCatalog.Medicine med : medicines) {
            boolean alreadyAdded = false;
            for (RxItem rx : prescriptions) {
                if (med.id.equals(rx.id)) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) available.add(med);
        }
        if (available.isEmpty()) {
            App.note(this, "Bạn đã chọn tất cả thuốc trong danh mục.");
            return;
        }

        String[] labels = new String[available.size()];
        boolean[] selected = new boolean[available.size()];
        for (int i = 0; i < available.size(); i++) {
            labels[i] = available.get(i).toString();
        }
        new AlertDialog.Builder(this)
                .setTitle("Chọn nhiều loại thuốc")
                .setMultiChoiceItems(labels, selected, (dlg, which, checked) ->
                        selected[which] = checked)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Thêm vào đơn", (dlg, which) -> {
                    int added = 0;
                    for (int i = 0; i < available.size(); i++) {
                        if (!selected[i]) continue;
                        MedicineCatalog.Medicine med = available.get(i);
                        RxItem pending = new RxItem();
                        pending.id = med.id;
                        pending.name = med.name;
                        pending.unit = med.unit;
                        pending.price = med.price;
                        pending.quantity = 0;
                        pending.days = 0;
                        pending.dose = "";
                        pending.instruction = "";
                        // Bác sĩ tự chọn liều và thời điểm; không điền ngầm định.
                        prescriptions.add(pending);
                        added++;
                    }
                    if (added == 0) {
                        App.note(this, "Bạn chưa chọn thuốc nào.");
                        return;
                    }
                    legacyOnly = false;
                    txtLegacy.setVisibility(View.GONE);
                    refreshPrescription();
                    App.note(this, "Đã thêm " + added
                            + " loại thuốc. Nhấn từng thuốc hoặc 'Nhập liều' để kê đơn.");
                })
                .show();
    }

    private boolean isConfigured(RxItem item) {
        return item.quantity > 0 && item.days > 0
                && item.dose != null && !item.dose.trim().isEmpty()
                && (item.morning || item.noon || item.evening);
    }

    private void editRxAt(int index) {
        if (index < 0 || index >= prescriptions.size()) return;
        RxItem old = prescriptions.get(index);
        // Giữ giá đang gắn với đơn thuốc, không thay bằng giá catalog mới.
        showRxDialog(new MedicineCatalog.Medicine(old.id, old.name, old.unit, old.price), old, index);
    }

    private void showRxDialog(MedicineCatalog.Medicine med, RxItem original, int index) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_prescribe_medicine, null);
        ((TextView) form.findViewById(R.id.txtChosenMedicine)).setText(med.name);
        ((TextView) form.findViewById(R.id.txtChosenPrice)).setText(
                "Đơn giá niêm yết: " + PriceText.vnd(med.price) + "/" + med.unit);
        EditText qty = form.findViewById(R.id.edtRxQuantity);
        EditText dose = form.findViewById(R.id.edtRxDose);
        EditText days = form.findViewById(R.id.edtRxDays);
        EditText instr = form.findViewById(R.id.edtRxInstruction);
        CheckBox morning = form.findViewById(R.id.chkRxMorning);
        CheckBox noon = form.findViewById(R.id.chkRxNoon);
        CheckBox evening = form.findViewById(R.id.chkRxEvening);
        TextView warning = form.findViewById(R.id.txtRxValidation);
        TextInputLayout tilQty = form.findViewById(R.id.tilRxQuantity);
        TextInputLayout tilDose = form.findViewById(R.id.tilRxDose);
        TextInputLayout tilDays = form.findViewById(R.id.tilRxDays);
        if (original != null) {
            if (original.quantity > 0) qty.setText(String.valueOf(original.quantity));
            if (original.dose != null) dose.setText(original.dose);
            if (original.days > 0) days.setText(String.valueOf(original.days));
            instr.setText(original.instruction);
            morning.setChecked(original.morning);
            noon.setChecked(original.noon);
            evening.setChecked(original.evening);
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(original == null ? "Thêm vào đơn thuốc"
                        : (isConfigured(original) ? "Chỉnh sửa liều lượng" : "Nhập liều lượng"))
                .setView(form)
                .setPositiveButton("Lưu thuốc", null)
                .setNegativeButton("Hủy", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    tilQty.setError(null); tilDose.setError(null); tilDays.setError(null);
                    warning.setText("");
                    int quantity = parsePositive(qty, tilQty, 1000);
                    int duration = parsePositive(days, tilDays, 365);
                    String doseText = App.value(dose);
                    if (doseText.isEmpty()) tilDose.setError("Bác sĩ phải ghi liều dùng.");
                    if (!morning.isChecked() && !noon.isChecked() && !evening.isChecked())
                        warning.setText("Chọn ít nhất một thời điểm uống thuốc.");
                    if (quantity <= 0 || duration <= 0 || doseText.isEmpty()
                            || (!morning.isChecked() && !noon.isChecked() && !evening.isChecked())) return;
                    RxItem item = new RxItem();
                    item.id = med.id; item.name = med.name; item.unit = med.unit;
                    item.price = med.price; item.quantity = quantity; item.days = duration;
                    item.dose = doseText; item.instruction = App.value(instr);
                    item.morning = morning.isChecked(); item.noon = noon.isChecked();
                    item.evening = evening.isChecked();
                    if (index >= 0) prescriptions.set(index, item);
                    else prescriptions.add(item);
                    legacyOnly = false;
                    txtLegacy.setVisibility(View.GONE);
                    refreshPrescription();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private int parsePositive(EditText field, TextInputLayout layout, int max) {
        try {
            int value = Integer.parseInt(App.value(field));
            if (value >= 1 && value <= max) return value;
        } catch (NumberFormatException ignored) { }
        layout.setError("Nhập số từ 1 đến " + max);
        return 0;
    }

    private void refreshPrescription() {
        adapter.notifyDataSetChanged();
        long total = 0;
        int pendingCount = 0;
        for (RxItem r : prescriptions) {
            if (isConfigured(r)) total += r.price * r.quantity;
            else pendingCount++;
        }
        txtTotal.setText("Tổng tiền thuốc dự kiến: " + PriceText.vnd(total)
                + (pendingCount > 0 ? "\nCòn " + pendingCount + " thuốc chưa kê liều lượng." : ""));
    }

    private boolean validateData() {
        tilDiagnosis.setError(null);
        if (App.value(diagnosis).length() < 3) {
            tilDiagnosis.setError("Chẩn đoán phải có ít nhất 3 ký tự.");
            return false;
        }
        for (RxItem item : prescriptions) {
            if (!isConfigured(item)) {
                new AlertDialog.Builder(this)
                        .setTitle("Đơn thuốc chưa hoàn chỉnh")
                        .setMessage("Thuốc " + item.name
                                + " chưa có đủ số lượng, liều, số ngày và thời điểm uống. "
                                + "Hãy nhấn vào thuốc để nhập hoặc xóa thuốc khỏi đơn.")
                        .setPositiveButton("Đã hiểu", null)
                        .show();
                return false;
            }
        }
        return true;
    }

    private void saveRecord() {
        if (appointment == null || !validateData()) return;
        new AlertDialog.Builder(this)
                .setTitle("Hoàn tất lượt khám")
                .setMessage("Lưu kết quả khám, đơn thuốc và đánh dấu lịch khám đã hoàn thành?")
                .setNegativeButton("Kiểm tra lại", null)
                .setPositiveButton("Lưu và hoàn tất", (dlg, which) -> commitRecord())
                .show();
    }

    private void commitRecord() {
        save.setEnabled(false);
        message.setText("Đang lưu phiếu khám...");
        DocumentReference record = App.db().collection("medical_records").document(appointmentId);
        DocumentReference plan = App.db().collection("treatment_plans").document(appointmentId);
        DocumentReference appt = App.db().collection("appointments").document(appointmentId);
        String patientId = App.str(appointment, "patientId");
        String doctorName = App.str(appointment, "doctorName");

        List<Map<String, Object>> items = new ArrayList<>();
        long total = 0;
        StringBuilder description = new StringBuilder();
        for (RxItem item : prescriptions) {
            items.add(item.toMap());
            total += item.price * item.quantity;
            if (description.length() > 0) description.append("\n");
            description.append(item.name).append(" x").append(item.quantity).append(" ").append(item.unit)
                    .append(" • ").append(item.dose).append(" • ").append(item.schedule())
                    .append(" • ").append(item.days).append(" ngày")
                    .append(" • ").append(PriceText.vnd(item.price * item.quantity));
            if (!item.instruction.isEmpty()) description.append("\n   ").append(item.instruction);
        }
        String prescriptionText = prescriptions.isEmpty() && !originallyHadPricedItems
                ? oldPrescription : description.toString();
        Map<String, Object> r = new HashMap<>();
        r.put("appointmentId", appointmentId);
        r.put("patientId", patientId);
        r.put("doctorId", App.uid());
        r.put("patientName", App.str(appointment, "patientName"));
        r.put("doctorName", doctorName);
        r.put("visitAt", appointment.getTimestamp("startAt"));
        r.put("diagnosis", App.value(diagnosis));
        r.put("prescription", prescriptionText);
        r.put("notes", App.value(notes));
        r.put("updatedAt", FieldValue.serverTimestamp());
        if (!hasRecord) r.put("createdAt", FieldValue.serverTimestamp());

        Map<String, Object> t = new HashMap<>();
        t.put("appointmentId", appointmentId);
        t.put("patientId", patientId);
        t.put("doctorId", App.uid());
        t.put("doctorName", doctorName);
        t.put("updatedAt", FieldValue.serverTimestamp());
        if (!hasPlan) t.put("createdAt", FieldValue.serverTimestamp());
        if (!legacyOnly || !prescriptions.isEmpty()) {
            t.put("items", items);
            t.put("totalAmountVnd", total);
            t.put("isDemoPrice", usingDemoPrices);
            // Giữ trường cũ để các màn hình và bệnh án phiên bản trước không lỗi.
            if (!prescriptions.isEmpty()) {
                RxItem first = prescriptions.get(0);
                t.put("medicine", first.name);
                t.put("method", first.dose + " • " + first.schedule());
                t.put("timesPerDay", first.timesPerDay());
                t.put("guide", first.instruction);
                t.put("purpose", "Theo kết quả khám");
            } else {
                t.put("medicine", ""); t.put("method", "");
                t.put("timesPerDay", 0); t.put("guide", ""); t.put("purpose", "");
            }
        }

        WriteBatch batch = App.db().batch();
        batch.set(record, r, SetOptions.merge());
        batch.set(plan, t, SetOptions.merge());
        if ("CONFIRMED".equals(App.str(appointment, "status")))
            batch.update(appt, "status", "COMPLETED", "updatedAt", FieldValue.serverTimestamp());
        batch.commit()
                .addOnSuccessListener(x -> {
                    App.note(this, "Đã lưu phiếu khám, đơn thuốc và hoàn thành lượt khám.");
                    finish();
                })
                .addOnFailureListener(e -> {
                    save.setEnabled(true);
                    if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException
                            && ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode()
                            == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        message.setText("Firebase từ chối lưu (PERMISSION_DENIED). "
                                + "Kiểm tra Rules của medical_records, treatment_plans và appointments "
                                + "trên Firebase Console, và quyền bác sĩ được phân công.");
                    } else {
                        message.setText("Lưu thất bại: " + e.getLocalizedMessage());
                    }
                });
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }
    private static long num(Object o) { return o instanceof Number ? ((Number) o).longValue() : 0; }

    private class PrescriptionAdapter extends RecyclerView.Adapter<PrescriptionAdapter.Holder> {
        class Holder extends RecyclerView.ViewHolder {
            TextView name, instruction, total, edit, delete;
            Holder(View view) {
                super(view);
                name = view.findViewById(R.id.txtRxName);
                instruction = view.findViewById(R.id.txtRxInstruction);
                total = view.findViewById(R.id.txtRxTotal);
                edit = view.findViewById(R.id.btnRxEdit);
                delete = view.findViewById(R.id.btnRxDelete);
            }
        }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prescription, parent, false));
        }
        @Override public int getItemCount() { return prescriptions.size(); }
        @Override public void onBindViewHolder(@NonNull Holder h, int position) {
            RxItem item = prescriptions.get(position);
            h.name.setText(item.name + " • " + item.quantity + " " + item.unit);
            boolean configured = isConfigured(item);
            if (configured) {
                h.instruction.setText("Liều: " + item.dose + "  |  " + item.schedule()
                        + "  |  " + item.days + " ngày"
                        + (item.instruction == null || item.instruction.isEmpty()
                            ? "" : "\n" + item.instruction));
                h.total.setText(PriceText.vnd(item.price) + "/" + item.unit
                        + "  →  " + PriceText.vnd(item.price * item.quantity));
            } else {
                h.instruction.setText("Chưa nhập liều lượng • Chạm vào thuốc để kê đơn");
                h.total.setText("Giá niêm yết: " + PriceText.vnd(item.price) + "/" + item.unit);
            }
            h.edit.setText(configured ? "Sửa" : "Nhập liều");
            h.itemView.setOnClickListener(v -> {
                int p = h.getBindingAdapterPosition();
                if (p != RecyclerView.NO_POSITION) editRxAt(p);
            });
            h.edit.setOnClickListener(v -> {
                int p = h.getBindingAdapterPosition();
                if (p != RecyclerView.NO_POSITION) editRxAt(p);
            });
            h.delete.setOnClickListener(v -> {
                int p = h.getBindingAdapterPosition();
                if (p == RecyclerView.NO_POSITION) return;
                new AlertDialog.Builder(RecordEditorActivity.this)
                        .setMessage("Xóa " + prescriptions.get(p).name + " khỏi đơn thuốc?")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Xóa", (d, which) -> {
                            prescriptions.remove(p);
                            refreshPrescription();
                        }).show();
            });
        }
    }
}
