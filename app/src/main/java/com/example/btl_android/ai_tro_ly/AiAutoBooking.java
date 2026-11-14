package com.example.btl_android.ai_tro_ly;

import com.example.btl_android.core_he_thong.App;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Source;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Reserves a real Firestore appointment after the patient confirms a candidate.
 * AI NEVER writes data directly; only verified Firestore data is used.
 * Mirrors BookingActivity's appointment + slot transaction and PENDING status.
 */
public final class AiAutoBooking {
    private static final TimeZone VIETNAM = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");

    public interface Callback {
        void success(String appointmentId);
        void failure(String message);
    }

    public void book(AiBookingHelper.Candidate c, Callback cb) {
        if (c == null || empty(c.doctorId) || empty(c.scheduleSlotId)
                || empty(c.date) || empty(c.time) || empty(c.reason)) {
            cb.failure("Thiếu thông tin lịch khám. Hãy tìm giờ khám lại.");
            return;
        }
        if (App.user() == null) {
            cb.failure("Vui lòng đăng nhập tài khoản bệnh nhân trước khi đặt lịch.");
            return;
        }
        if (c.reason.trim().length() < 3 || c.reason.length() > 1000) {
            cb.failure("Lý do khám phải có 3–1000 ký tự.");
            return;
        }

        final Calendar appointmentTime = Calendar.getInstance(VIETNAM, Locale.US);
        final SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        parser.setLenient(false);
        parser.setTimeZone(VIETNAM);
        final Date selected;
        try {
            if (!c.date.matches("\\d{4}-\\d{2}-\\d{2}") ||
                    !c.time.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
                throw new ParseException("Invalid date or time", 0);
            }
            selected = parser.parse(c.date + " " + c.time);
            if (selected == null) throw new ParseException("Invalid date", 0);
            appointmentTime.setTime(selected);
        } catch (ParseException e) {
            cb.failure("Ngày hoặc giờ khám không hợp lệ.");
            return;
        }
        if (selected.getTime() < System.currentTimeMillis() + 5 * 60_000L) {
            cb.failure("Giờ khám phải cách hiện tại ít nhất 5 phút. Hãy tìm lịch khác.");
            return;
        }

        // Must match the same ID format used by BookingActivity in UniCare.
        SimpleDateFormat idDate = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US);
        idDate.setTimeZone(VIETNAM);
        final String expectedSlotId = c.doctorId + "_" + idDate.format(selected);
        final int weekday = appointmentTime.get(Calendar.DAY_OF_WEEK);
        final int hour = appointmentTime.get(Calendar.HOUR_OF_DAY);
        final int minute = appointmentTime.get(Calendar.MINUTE);
        final String uid = App.uid();
        if (empty(uid)) { cb.failure("Phiên đăng nhập đã hết hạn."); return; }

        // Never assume a user is a patient merely because they opened the AI screen.
        App.db().collection("users").document(uid).get(Source.SERVER)
            .addOnSuccessListener(profile -> {
                if (!profile.exists() || !"patient".equals(profile.getString("role"))) {
                    cb.failure("Chỉ tài khoản bệnh nhân mới có thể đặt lịch.");
                    return;
                }
                // Prevent writing for a different account if the user changes mid-request.
                if (App.user() == null || !uid.equals(App.uid())) {
                    cb.failure("Tài khoản đã thay đổi. Vui lòng đăng nhập và thực hiện lại.");
                    return;
                }
                final String patientName = profile.getString("name") == null ? "" : profile.getString("name");
                final DocumentReference doctorRef = App.db().collection("doctors").document(c.doctorId);
                final DocumentReference scheduleRef = App.db().collection("doctor_schedules")
                        .document(c.doctorId).collection("slots").document(c.scheduleSlotId);
                final DocumentReference takenSlot = App.db().collection("slots").document(expectedSlotId);
                final DocumentReference appointmentRef = App.db().collection("appointments").document();

                App.db().runTransaction(tr -> {
                    // All relevant reads happen BEFORE writes and are retried atomically if needed.
                    DocumentSnapshot doctor = tr.get(doctorRef);
                    DocumentSnapshot schedule = tr.get(scheduleRef);
                    DocumentSnapshot booked = tr.get(takenSlot);
                    if (!doctor.exists()) throw new IllegalStateException("Bác sĩ không còn trong hệ thống.");
                    if (!schedule.exists()) throw new IllegalStateException("Lịch làm việc đã bị xóa. Hãy chọn giờ khác.");
                    Long day = schedule.getLong("dayOfWeek");
                    Long h = schedule.getLong("startHour");
                    Long m = schedule.getLong("startMin");
                    if (day == null || h == null || m == null || day.intValue() != weekday ||
                            h.intValue() != hour || m.intValue() != minute ||
                            Boolean.TRUE.equals(schedule.getBoolean("isBooked"))) {
                        throw new IllegalStateException("Bác sĩ không còn làm việc vào giờ đó.");
                    }
                    if (booked.exists()) {
                        throw new IllegalStateException("Khung giờ đã có người đặt. Hãy chọn giờ khác.");
                    }
                    if (selected.getTime() < System.currentTimeMillis() + 5 * 60_000L) {
                        throw new IllegalStateException("Giờ khám đã quá gần. Hãy chọn giờ khác.");
                    }

                    // Doctor name/specialty come from Firestore, not AI generated strings.
                    String doctorName = safe(doctor.getString("name"));
                    String specialty = safe(doctor.getString("specialty"));
                    if (doctorName.isEmpty() || specialty.isEmpty()) {
                        throw new IllegalStateException("Hồ sơ bác sĩ thiếu thông tin.");
                    }
                    Map<String,Object> a = new HashMap<>();
                    a.put("patientId", uid);
                    a.put("patientName", patientName);
                    a.put("doctorId", c.doctorId);
                    a.put("doctorName", doctorName);
                    a.put("specialty", specialty);
                    a.put("startAt", new Timestamp(selected));
                    a.put("reason", c.reason.trim());
                    a.put("slotId", expectedSlotId);
                    a.put("status", "PENDING"); // DO NOT claim doctor-confirmed booking
                    a.put("createdAt", FieldValue.serverTimestamp());
                    a.put("updatedAt", FieldValue.serverTimestamp());

                    Map<String,Object> sl = new HashMap<>();
                    sl.put("appointmentId", appointmentRef.getId());
                    sl.put("doctorId", c.doctorId);
                    sl.put("createdAt", FieldValue.serverTimestamp());
                    tr.set(appointmentRef, a);
                    tr.set(takenSlot, sl);
                    return appointmentRef.getId();
                }).addOnSuccessListener(cb::success)
                  .addOnFailureListener(err -> cb.failure(
                      err.getLocalizedMessage() == null ? "Không đặt được lịch. Vui lòng thử lại."
                          : err.getLocalizedMessage()));
            })
            .addOnFailureListener(e -> cb.failure("Không kiểm tra được tài khoản bệnh nhân trên Firebase."));
    }

    private static String safe(String x) { return x == null ? "" : x.trim(); }
    private static boolean empty(String x) { return x == null || x.trim().isEmpty(); }
}
