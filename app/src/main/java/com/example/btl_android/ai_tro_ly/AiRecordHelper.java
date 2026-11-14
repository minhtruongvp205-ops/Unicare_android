package com.example.btl_android.ai_tro_ly;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Read only records owned by the signed-in patient. Never mutate medical data. */
public final class AiRecordHelper {
    public static class RecordChoice {
        public final String id;
        public final String label;
        RecordChoice(String id, String label) { this.id = id; this.label = label; }
    }

    public interface ListCallback {
        void success(List<RecordChoice> items);
        void failure(String message);
    }
    public interface TextCallback {
        void success(String clinicalText);
        void failure(String message);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String uid() {
        return FirebaseAuth.getInstance().getCurrentUser() == null ? ""
                : FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public void listOwnRecords(ListCallback cb) {
        String uid = uid();
        if (uid.isEmpty()) { cb.failure("Bạn cần đăng nhập."); return; }
        db.collection("medical_records").whereEqualTo("patientId", uid).get()
            .addOnSuccessListener(results -> {
                List<RecordChoice> choices = new ArrayList<>();
                for (QueryDocumentSnapshot record : results) {
                    Timestamp visitAt = record.getTimestamp("visitAt");
                    String date = visitAt == null ? "Không rõ ngày"
                        : new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(visitAt.toDate());
                    String diagnosis = safe(record.getString("diagnosis"));
                    if (diagnosis.length() > 55) diagnosis = diagnosis.substring(0, 55) + "...";
                    choices.add(new RecordChoice(record.getId(), date + " • " + diagnosis));
                }
                cb.success(choices);
            })
            .addOnFailureListener(e -> cb.failure("Không tải được bệnh án: " + e.getLocalizedMessage()));
    }

    /** Caller must obtain explicit patient consent before invoking this method. */
    public void loadOwnRecordForSummary(String recordId, TextCallback cb) {
        String uid = uid();
        if (uid.isEmpty() || recordId == null || recordId.isEmpty()) {
            cb.failure("Không thể xác thực hồ sơ."); return;
        }
        db.collection("medical_records").document(recordId).get()
            .addOnSuccessListener(record -> {
                if (!record.exists() || !uid.equals(record.getString("patientId"))) {
                    cb.failure("Bạn không có quyền truy cập bệnh án này.");
                    return;
                }
                db.collection("treatment_plans").document(recordId).get()
                    .addOnSuccessListener(plan -> cb.success(buildClinicalText(record, plan)))
                    .addOnFailureListener(e -> cb.failure("Không đọc được hướng dẫn điều trị: "
                            + e.getLocalizedMessage()));
            })
            .addOnFailureListener(e -> cb.failure("Không thể đọc hồ sơ: " + e.getLocalizedMessage()));
    }

    private String buildClinicalText(DocumentSnapshot record, DocumentSnapshot plan) {
        StringBuilder sb = new StringBuilder();
        // Intentionally omit patient/doctor names, UIDs, phone numbers and addresses.
        Timestamp ts = record.getTimestamp("visitAt");
        if (ts != null) sb.append("Ngày khám: ")
            .append(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ts.toDate()))
            .append('\n');
        sb.append("Chẩn đoán bác sĩ ghi: ").append(safe(record.getString("diagnosis"))).append('\n');
        sb.append("Ghi chú: ").append(safe(record.getString("notes"))).append('\n');
        sb.append("Đơn thuốc (văn bản): ").append(safe(record.getString("prescription"))).append('\n');
        if (plan.exists()) {
            sb.append("Thuốc: ").append(safe(plan.getString("medicine"))).append('\n');
            sb.append("Cách dùng: ").append(safe(plan.getString("method"))).append('\n');
            sb.append("Hướng dẫn: ").append(safe(plan.getString("guide"))).append('\n');
            Object raw = plan.get("items");
            if (raw instanceof List<?>) {
                int count = 0;
                for (Object item : (List<?>) raw) {
                    if (!(item instanceof Map<?, ?>) || count++ >= 20) continue;
                    Map<?, ?> m = (Map<?, ?>) item;
                    sb.append("Thuốc: ").append(safe(m.get("name")))
                      .append("; Liều: ").append(safe(m.get("dose")))
                      .append("; Lịch: ").append(safe(m.get("schedule")))
                      .append("; Số ngày: ").append(safe(m.get("days")))
                      .append("; Lưu ý: ").append(safe(m.get("instruction"))).append('\n');
                }
            }
        }
        String result = sb.toString();
        return result.length() > 12000 ? result.substring(0, 12000) : result;
    }
    private static String safe(Object obj) { return obj == null ? "Không có" : String.valueOf(obj); }
}
