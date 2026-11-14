package com.example.btl_android.ai_tro_ly;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * AI đọc mô tả triệu chứng hoặc nhu cầu của người bệnh, suy luận và gợi ý chuyên khoa phù hợp.
 * Sau đó truy vấn danh sách bác sĩ và khung giờ làm việc thực tế từ Firestore để người dùng chọn và xác nhận.
 */
public final class AiBookingHelper {
    public static class Candidate {
        public final String doctorId, doctorName, specialty, date, time, reason, scheduleSlotId;
        public Candidate(String id, String name, String specialty, String date, String time, String reason, String scheduleSlotId) {
            this.doctorId = id; this.doctorName = name; this.specialty = specialty;
            this.date = date; this.time = time; this.reason = reason; this.scheduleSlotId = scheduleSlotId;
        }
        @Override public String toString() {
            return doctorName + " • " + specialty + "\n" + date + " lúc " + time;
        }
    }
    private static class Doctor {
        String id, name, specialty;
        Doctor(String i, String n, String s) { id=i; name=n; specialty=s; }
    }
    private static class AvailabilityCheck {
        Candidate candidate;
        Task<DocumentSnapshot> task;
        AvailabilityCheck(Candidate c, Task<DocumentSnapshot> t) { candidate=c; task=t; }
    }
    public interface Callback {
        void status(String msg);
        void found(String specialtySuggested, String reason, List<Candidate> choices);
        void failure(String msg);
    }
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final AiService ai;
    public AiBookingHelper(AiService ai) { this.ai = ai; }

    public void suggest(String request, Callback cb) {
        if (request == null || request.trim().length() < 3 || request.length() > 1000) {
            cb.failure("Hãy nhập mô tả triệu chứng hoặc nhu cầu khám từ 3 đến 1000 ký tự.");
            return;
        }
        // Xử lý ngày hẹn bằng bộ giải mã thông minh (nếu không nói ngày thì tự chọn ngày gần nhất)
        BookingDateParser.Result selectedDate = BookingDateParser.parse(request);
        if (!selectedDate.isValid()) {
            cb.failure(selectedDate.error);
            return;
        }
        String prompt =
                "Bạn là trợ lý y tế thông minh của Bệnh viện UniCare.\n" +
                "Hãy đọc mô tả nhu cầu hoặc triệu chứng bệnh của người dùng và trả về JSON theo schema:\n" +
                "1. specialty: Nếu người dùng nêu chuyên khoa (ví dụ: Mắt, Tim mạch, Tai Mũi Họng, Răng Hàm Mặt, Da liễu, Cơ Xương Khớp, Tiêu hóa, Thần kinh, Nhi, Nội tổng quát...), hãy trích xuất chuyên khoa đó.\n" +
                "NẾU NGƯỜI DÙNG MÔ TẢ TRIỆU CHỨNG HOẶC NHU CẦU (ví dụ đau mắt, mờ mắt -> Mắt; đau ngực, khó thở, hồi hộp -> Tim mạch; đau họng, ho, sổ mũi -> Tai Mũi Họng; đau răng, chảy máu chân răng -> Răng Hàm Mặt; đau khớp gối, đau lưng -> Cơ Xương Khớp; ngứa da, mẩn đỏ -> Da liễu; đau bụng, ợ chua, dạ dày -> Tiêu hóa; sốt cao, mệt mỏi -> Nội; trẻ em bị sốt -> Nhi), hãy TỰ ĐỘNG SUY LUẬN và GỢI Ý chuyên khoa y tế phù hợp nhất.\n" +
                "2. specialtyReason: 1 câu giải thích ngắn gọn, ân cần vì sao gợi ý chuyên khoa đó dựa trên triệu chứng (ví dụ: 'Dựa trên triệu chứng đau mắt và nhìn mờ của bạn, tôi gợi ý bạn nên khám chuyên khoa Mắt để kiểm tra thị lực và đáy mắt.').\n" +
                "3. period: 'morning' (sáng), 'afternoon' (chiều), 'evening' (tối), hoặc 'any' nếu không nói rõ.\n" +
                "4. time: giờ cụ thể HH:mm nếu có, nếu không để chuỗi rỗng.\n" +
                "Yêu cầu người dùng: " + request;

        cb.status("AI đang phân tích triệu chứng và gợi ý chuyên khoa...");
        ai.askBookingIntent(prompt, new AiService.Callback() {
            @Override public void onSuccess(String answer) {
                try {
                    JSONObject o = new JSONObject(answer);
                    String specialty = o.optString("specialty", "").trim();
                    String specialtyReason = o.optString("specialtyReason", "").trim();
                    String period = o.optString("period", "any").trim();
                    String time = o.optString("time", "").trim();

                    if (specialty.isEmpty()) {
                        specialty = "Nội";
                    }
                    if (specialtyReason.isEmpty()) {
                        specialtyReason = "Dựa trên mô tả của bạn, tôi gợi ý bạn khám chuyên khoa " + specialty + ".";
                    }

                    int from = 6, to = 22;
                    if (period.equals("morning")) { from = 6; to = 12; }
                    else if (period.equals("afternoon")) { from = 12; to = 18; }
                    else if (period.equals("evening")) { from = 18; to = 22; }

                    int hour = -1, minute = -1;
                    if (!time.isEmpty() && time.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
                        hour = Integer.parseInt(time.substring(0,2));
                        minute = Integer.parseInt(time.substring(3,5));
                        if (hour < from || hour >= to) {
                            from = 6; to = 22; // Nới lỏng khung giờ nếu giờ chỉ định nằm ngoài
                        }
                    }
                    String reason = request.trim();
                    findSlots(specialty, specialtyReason, selectedDate.date, selectedDate.weekday,
                            from, to, hour, minute, reason, cb);
                } catch (Exception e) {
                    cb.failure("Không phân tích được chuyên khoa từ yêu cầu. Vui lòng thử lại.");
                }
            }
            @Override public void onFailure(String message) { cb.failure(message); }
        });
    }

    private void findSlots(String specialty, String specialtyReason, String date, int weekday, int from, int to,
                           int reqHour, int reqMinute, String reason, Callback cb) {
        cb.status("UniCare đang tìm bác sĩ và khung giờ thực tế từ hệ thống...");
        db.collection("doctors").get().addOnSuccessListener(all -> {
            List<Doctor> matching = new ArrayList<>();
            String wanted = normalize(specialty);
            for (QueryDocumentSnapshot d : all) {
                String s = d.getString("specialty");
                if (s != null && (normalize(s).contains(wanted) || wanted.contains(normalize(s)))) {
                    matching.add(new Doctor(d.getId(), safe(d.getString("name")), s));
                }
            }
            // Nếu không tìm thấy bác sĩ khớp chính xác, lấy bác sĩ Nội tổng quát hoặc bác sĩ bất kỳ có trong viện
            if (matching.isEmpty()) {
                for (QueryDocumentSnapshot d : all) {
                    matching.add(new Doctor(d.getId(), safe(d.getString("name")), safe(d.getString("specialty"))));
                }
            }
            if (matching.isEmpty()) {
                cb.failure("Hiện tại hệ thống chưa có thông tin bác sĩ trên Firestore.");
                return;
            }

            List<Task<QuerySnapshot>> scheduleTasks = new ArrayList<>();
            for (Doctor doc : matching) {
                scheduleTasks.add(db.collection("doctor_schedules").document(doc.id)
                        .collection("slots").whereEqualTo("dayOfWeek", weekday).get(com.google.firebase.firestore.Source.SERVER));
            }
            Tasks.whenAllComplete(scheduleTasks).addOnSuccessListener(ignored -> {
                List<AvailabilityCheck> checks = new ArrayList<>();
                boolean scheduleErrors = false;
                for (int i=0; i<scheduleTasks.size(); i++) {
                    Task<QuerySnapshot> t = scheduleTasks.get(i);
                    if (!t.isSuccessful()) { scheduleErrors = true; continue; }
                    Doctor doc = matching.get(i);
                    QuerySnapshot qs = t.getResult();
                    if (qs == null) continue;
                    for (DocumentSnapshot slot : qs.getDocuments()) {
                        Long h = slot.getLong("startHour"), m = slot.getLong("startMin");
                        if (h == null || m == null || Boolean.TRUE.equals(slot.getBoolean("isBooked"))) continue;
                        int hour = h.intValue(), minute = m.intValue();
                        if (hour < from || hour >= to || (reqHour >= 0 &&
                                (reqHour != hour || reqMinute != minute))) continue;

                        Calendar now = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                        java.text.SimpleDateFormat todayFmt = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        todayFmt.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                        if (date.equals(todayFmt.format(now.getTime())) &&
                                (hour < now.get(Calendar.HOUR_OF_DAY) ||
                                        (hour == now.get(Calendar.HOUR_OF_DAY) && minute <= now.get(Calendar.MINUTE)))) continue;
                        String hm = String.format(Locale.US,"%02d:%02d",hour,minute);
                        String slotId = doc.id + "_" + date.replace("-", "") + "_" + hm.replace(":", "");
                        Candidate candidate = new Candidate(doc.id, doc.name, doc.specialty, date, hm,
                                reason.length() >= 3 ? reason : ("Khám chuyên khoa " + specialty), slot.getId());
                        checks.add(new AvailabilityCheck(candidate, db.collection("slots").document(slotId).get(com.google.firebase.firestore.Source.SERVER)));
                        if (checks.size() >= 40) break;
                    }
                    if (checks.size() >= 40) break;
                }
                if (checks.isEmpty()) {
                    cb.failure(scheduleErrors ? "Không đọc được đầy đủ lịch bác sĩ. Kiểm tra kết nối Firestore."
                            : ("Chưa tìm được lịch trống cho chuyên khoa " + specialty + " vào ngày " + date + ". Hãy thử chọn ngày khác hoặc đặt thủ công."));
                    return;
                }
                List<Task<DocumentSnapshot>> bookings = new ArrayList<>();
                for (AvailabilityCheck c : checks) bookings.add(c.task);
                Tasks.whenAllComplete(bookings).addOnSuccessListener(allChecked -> {
                    List<Candidate> available = new ArrayList<>();
                    for (AvailabilityCheck c : checks) {
                        if (c.task.isSuccessful() && c.task.getResult() != null
                                && !c.task.getResult().exists()) available.add(c.candidate);
                    }
                    available.sort((a, b) -> {
                        int t = a.time.compareTo(b.time);
                        return t != 0 ? t : a.doctorName.compareToIgnoreCase(b.doctorName);
                    });
                    if (available.isEmpty()) cb.failure("Tất cả các khung giờ tìm được đã có người đặt trước. Vui lòng chọn ngày khác.");
                    else cb.found(specialty, specialtyReason, available.size() > 8 ? available.subList(0, 8) : available);
                }).addOnFailureListener(e -> cb.failure("Lỗi kiểm tra lịch đặt: " + e.getLocalizedMessage()));
            }).addOnFailureListener(e -> cb.failure("Lỗi tải lịch bác sĩ: " + e.getLocalizedMessage()));
        }).addOnFailureListener(e -> cb.failure("Lỗi tải danh sách bác sĩ: " + e.getLocalizedMessage()));
    }

    private static String safe(String s) { return s == null ? "Bác sĩ" : s; }
    private static String normalize(String s) {
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT).replace('đ','d'),Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").trim();
    }
}
