package com.example.btl_android.ai_tro_ly;

import com.example.btl_android.core_he_thong.App;
import com.example.btl_android.utils_tien_ich.PriceText;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Thu thập toàn bộ dữ liệu y tế của bệnh nhân đang đăng nhập từ Firestore:
 * - Thông tin cá nhân & tiền sử bệnh (users)
 * - Bệnh án & chẩn đoán của bác sĩ (medical_records)
 * - Kế hoạch điều trị & đơn thuốc chi tiết (treatment_plans)
 * - Danh mục thuốc đang theo dõi tại nhà (medications)
 * - Các chỉ số sức khoẻ sinh tồn gần nhất: Huyết áp, Nhịp tim, Đường huyết, Cân nặng, SpO2, Thân nhiệt (health_vitals)
 * - Chi phí khám, hóa đơn & viện phí (orders)
 * - Lịch hẹn khám (appointments)
 * 
 * Giúp AI có đầy đủ bối cảnh để trả lời chính xác mọi câu hỏi của người bệnh.
 */
public final class AiPatientDataHelper {

    public interface Callback {
        void onLoaded(String patientContext);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void loadPatientContext(String uid, Callback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onLoaded("Chưa có thông tin bệnh nhân (chưa đăng nhập).");
            return;
        }

        Task<DocumentSnapshot> userTask = db.collection("users").document(uid).get();
        Task<QuerySnapshot> recordsTask = db.collection("medical_records").whereEqualTo("patientId", uid).get();
        Task<QuerySnapshot> plansTask = db.collection("treatment_plans").whereEqualTo("patientId", uid).get();
        Task<QuerySnapshot> medsTask = db.collection("medications").document(uid).collection("list").get();
        Task<QuerySnapshot> vitalsTask = db.collection("health_vitals").document(uid).collection("records").get();
        Task<QuerySnapshot> ordersTask = db.collection("orders").whereEqualTo("patientId", uid).get();
        Task<QuerySnapshot> apptsTask = db.collection("appointments").whereEqualTo("patientId", uid).get();

        Tasks.whenAllComplete(userTask, recordsTask, plansTask, medsTask, vitalsTask, ordersTask, apptsTask)
                .addOnCompleteListener(all -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("=== DỮ LIỆU Y TẾ THỰC TẾ CỦA BỆNH NHÂN ĐANG ĐĂNG NHẬP (TRÍCH XUẤT TỪ UNICARE) ===\n\n");

                    // 1. Thông tin cá nhân
                    sb.append("[1. THÔNG TIN CÁ NHÂN & TIỀN SỬ BỆNH]\n");
                    if (userTask.isSuccessful() && userTask.getResult() != null && userTask.getResult().exists()) {
                        DocumentSnapshot u = userTask.getResult();
                        sb.append("- Họ tên: ").append(safe(u.getString("name"))).append("\n");
                        sb.append("- Giới tính: ").append(safe(u.getString("gender"))).append("\n");
                        sb.append("- Ngày sinh: ").append(safe(u.getString("dob"))).append("\n");
                        String history = safe(u.getString("medicalHistory"));
                        sb.append("- Tiền sử bệnh: ").append(history.isEmpty() ? "Không ghi nhận tiền sử đặc biệt" : history).append("\n");
                    } else {
                        sb.append("- Chưa cập nhật thông tin cá nhân đầy đủ.\n");
                    }
                    sb.append("\n");

                    // 2. Chẩn đoán & Bệnh án gần đây
                    sb.append("[2. HỒ SƠ BỆNH ÁN & CHẨN ĐOÁN CỦA BÁC SĨ]\n");
                    boolean hasRecords = false;
                    if (recordsTask.isSuccessful() && recordsTask.getResult() != null && !recordsTask.getResult().isEmpty()) {
                        hasRecords = true;
                        List<DocumentSnapshot> recordList = new ArrayList<>(recordsTask.getResult().getDocuments());
                        // Sắp xếp bản ghi mới nhất lên đầu
                        Collections.sort(recordList, (a, b) -> {
                            Timestamp ta = a.getTimestamp("visitAt");
                            Timestamp tb = b.getTimestamp("visitAt");
                            if (ta == null || tb == null) return 0;
                            return tb.compareTo(ta);
                        });

                        for (int i = 0; i < Math.min(recordList.size(), 3); i++) {
                            DocumentSnapshot r = recordList.get(i);
                            Timestamp ts = r.getTimestamp("visitAt");
                            String visitStr = ts != null ? App.date(ts) : "Gần đây";
                            sb.append("• Lượt khám ngày ").append(visitStr).append(":\n");
                            sb.append("  - Bệnh chẩn đoán: ").append(safe(r.getString("diagnosis"))).append("\n");
                            sb.append("  - Bác sĩ điều trị: ").append(safe(r.getString("doctorName"))).append("\n");
                            String notes = safe(r.getString("notes"));
                            if (!notes.isEmpty()) sb.append("  - Lời dặn / Ghi chú bác sĩ: ").append(notes).append("\n");
                            String rx = safe(r.getString("prescription"));
                            if (!rx.isEmpty()) sb.append("  - Đơn thuốc ghi: ").append(rx).append("\n");
                        }
                    }
                    if (!hasRecords) {
                        sb.append("- Chưa có hồ sơ bệnh án nào được ghi nhận trên hệ thống.\n");
                    }
                    sb.append("\n");

                    // 3. Kế hoạch điều trị & Hướng dẫn dùng thuốc
                    sb.append("[3. HƯỚNG DẪN ĐIỀU TRỊ & ĐƠN THUỐC CHI TIẾT TỪ BÁC SĨ]\n");
                    boolean hasPlans = false;
                    if (plansTask.isSuccessful() && plansTask.getResult() != null && !plansTask.getResult().isEmpty()) {
                        hasPlans = true;
                        for (DocumentSnapshot p : plansTask.getResult().getDocuments()) {
                            String med = safe(p.getString("medicine"));
                            String method = safe(p.getString("method"));
                            String guide = safe(p.getString("guide"));
                            Long times = p.getLong("timesPerDay");
                            if (!med.isEmpty()) {
                                sb.append("• Thuốc: ").append(med).append("\n");
                                if (!method.isEmpty()) sb.append("  - Cách dùng: ").append(method).append("\n");
                                if (times != null && times > 0) sb.append("  - Số lần uống: ").append(times).append(" lần/ngày\n");
                                if (!guide.isEmpty()) sb.append("  - Hướng dẫn chi tiết: ").append(guide).append("\n");
                            }
                            Object rawItems = p.get("items");
                            if (rawItems instanceof List<?>) {
                                for (Object item : (List<?>) rawItems) {
                                    if (item instanceof Map<?, ?>) {
                                        Map<?, ?> m = (Map<?, ?>) item;
                                        sb.append("  + Tên thuốc: ").append(safe(m.get("name")))
                                                .append(" | Liều lượng: ").append(safe(m.get("dose")))
                                                .append(" | Lịch uống: ").append(safe(m.get("schedule")))
                                                .append(" | Số ngày: ").append(safe(m.get("days")))
                                                .append(" | Số lượng: ").append(safe(m.get("quantity"))).append("\n");
                                    }
                                }
                            }
                        }
                    }
                    if (!hasPlans) {
                        sb.append("- Hiện chưa có kế hoạch điều trị chi tiết nào từ bác sĩ.\n");
                    }
                    sb.append("\n");

                    // 4. Thuốc theo dõi hàng ngày tại nhà
                    sb.append("[4. LỊCH UỐNG THUỐC THEO DÕI HÀNG NGÀY (TỦ THUỐC TẠI NHÀ)]\n");
                    boolean hasMeds = false;
                    if (medsTask.isSuccessful() && medsTask.getResult() != null && !medsTask.getResult().isEmpty()) {
                        hasMeds = true;
                        for (DocumentSnapshot m : medsTask.getResult().getDocuments()) {
                            sb.append("• ").append(safe(m.getString("name")))
                                    .append(" - Liều: ").append(safe(m.getString("dosage")))
                                    .append(" - Thời gian: ").append(safe(m.getString("timeLabel")))
                                    .append(" - Tần suất: ").append(safe(m.getString("frequency"))).append("\n");
                        }
                    }
                    if (!hasMeds) {
                        sb.append("- Bệnh nhân chưa thêm thuốc vào lịch uống tại nhà.\n");
                    }
                    sb.append("\n");

                    // 5. Chỉ số sức khỏe sinh tồn gần nhất
                    sb.append("[5. CÁC CHỈ SỐ SỨC KHỎE SINH TỒN GẦN NHẤT]\n");
                    boolean hasVitals = false;
                    if (vitalsTask.isSuccessful() && vitalsTask.getResult() != null && !vitalsTask.getResult().isEmpty()) {
                        hasVitals = true;
                        // Nhóm theo loại chỉ số mới nhất
                        Map<String, DocumentSnapshot> latestByType = new HashMap<>();
                        for (DocumentSnapshot doc : vitalsTask.getResult().getDocuments()) {
                            String type = doc.getString("type");
                            if (type == null) continue;
                            DocumentSnapshot existing = latestByType.get(type);
                            if (existing == null) {
                                latestByType.put(type, doc);
                            } else {
                                Timestamp te = existing.getTimestamp("recordedAt");
                                Timestamp td = doc.getTimestamp("recordedAt");
                                if (td != null && (te == null || td.compareTo(te) > 0)) {
                                    latestByType.put(type, doc);
                                }
                            }
                        }

                        appendVital(sb, "Huyết áp tâm thu (Systolic)", latestByType.get("bp_systolic"), "mmHg");
                        appendVital(sb, "Huyết áp tâm trương (Diastolic)", latestByType.get("bp_diastolic"), "mmHg");
                        appendVital(sb, "Nhịp tim (Heart Rate)", latestByType.get("heart_rate"), "bpm (nhịp/phút)");
                        appendVital(sb, "Đường huyết (Glucose)", latestByType.get("glucose"), "mg/dL");
                        appendVital(sb, "Cân nặng (Weight)", latestByType.get("weight"), "kg");
                        appendVital(sb, "Nhiệt độ (Temperature)", latestByType.get("temperature"), "°C");
                        appendVital(sb, "Nồng độ oxy SpO2", latestByType.get("spo2"), "%");
                    }
                    if (!hasVitals) {
                        sb.append("- Bệnh nhân chưa ghi nhận chỉ số sức khoẻ nào trên hệ thống.\n");
                    }
                    sb.append("\n");

                    // 6. Chi phí khám & Viện phí / Hóa đơn
                    sb.append("[6. CHI PHÍ KHÁM & VIỆN PHÍ / HÓA ĐƠN]\n");
                    boolean hasOrders = false;
                    if (ordersTask.isSuccessful() && ordersTask.getResult() != null && !ordersTask.getResult().isEmpty()) {
                        hasOrders = true;
                        for (DocumentSnapshot o : ordersTask.getResult().getDocuments()) {
                            Long total = o.getLong("totalAmountVnd");
                            String status = safe(o.getString("status"));
                            String method = safe(o.getString("paymentMethod"));
                            String stText = "PAID".equals(status) ? "ĐÃ THANH TOÁN" : ("PENDING".equals(status) ? "CHỜ THANH TOÁN" : status);
                            sb.append("• Đơn hóa đơn #").append(o.getId()).append(":\n");
                            sb.append("  - Tổng chi phí: ").append(total != null ? PriceText.vnd(total) : "0 đ").append("\n");
                            sb.append("  - Trạng thái thanh toán: ").append(stText).append("\n");
                            sb.append("  - Hình thức: ").append("CASH".equals(method) ? "Tiền mặt tại viện" : method).append("\n");
                            Object rawItems = o.get("items");
                            if (rawItems instanceof List<?>) {
                                sb.append("  - Chi tiết các khoản: ");
                                List<?> list = (List<?>) rawItems;
                                for (int i = 0; i < list.size(); i++) {
                                    if (list.get(i) instanceof Map<?, ?>) {
                                        Map<?, ?> itemMap = (Map<?, ?>) list.get(i);
                                        sb.append(safe(itemMap.get("name")));
                                        Object qty = itemMap.get("quantity");
                                        if (qty != null) sb.append(" (SL: ").append(qty).append(")");
                                        if (i < list.size() - 1) sb.append(", ");
                                    }
                                }
                                sb.append("\n");
                            }
                        }
                    }
                    if (!hasOrders) {
                        sb.append("- Hiện không có hóa đơn hoặc chi phí phát sinh nào cần thanh toán.\n");
                    }
                    sb.append("\n");

                    // 7. Lịch hẹn khám
                    sb.append("[7. LỊCH HẸN KHÁM BỆNH]\n");
                    boolean hasAppts = false;
                    if (apptsTask.isSuccessful() && apptsTask.getResult() != null && !apptsTask.getResult().isEmpty()) {
                        hasAppts = true;
                        for (DocumentSnapshot a : apptsTask.getResult().getDocuments()) {
                            String st = safe(a.getString("status"));
                            String stName = "PENDING".equals(st) ? "Chờ bác sĩ duyệt" : ("CONFIRMED".equals(st) ? "Đã xác nhận" : ("COMPLETED".equals(st) ? "Đã khám xong" : st));
                            Timestamp start = a.getTimestamp("startAt");
                            sb.append("• Bác sĩ: ").append(safe(a.getString("doctorName")))
                                    .append(" (Khoa: ").append(safe(a.getString("specialty"))).append(")")
                                    .append(" - Thời gian: ").append(start != null ? App.date(start) : "")
                                    .append(" - Trạng thái: ").append(stName);
                            String reason = safe(a.getString("reason"));
                            if (!reason.isEmpty()) sb.append(" - Lý do: ").append(reason);
                            sb.append("\n");
                        }
                    }
                    if (!hasAppts) {
                        sb.append("- Hiện không có lịch hẹn khám nào.\n");
                    }

                    callback.onLoaded(sb.toString());
                });
    }

    private void appendVital(StringBuilder sb, String label, DocumentSnapshot doc, String defaultUnit) {
        if (doc == null) return;
        Object val = doc.get("value");
        if (val == null) return;
        Timestamp ts = doc.getTimestamp("recordedAt");
        String dateStr = ts != null ? new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(ts.toDate()) : "Gần nhất";
        String unit = safe(doc.getString("unit"));
        if (unit.isEmpty()) unit = defaultUnit;
        sb.append("• ").append(label).append(": ").append(val).append(" ").append(unit)
                .append(" (Đo lúc: ").append(dateStr).append(")\n");
    }

    private static String safe(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}

