package com.example.btl_android.core_he_thong.manager;

import com.example.btl_android.R;
import com.example.btl_android.core_he_thong.*;
import com.example.btl_android.core_he_thong.manager.*;
import com.example.btl_android.data_du_lieu.*;
import com.example.btl_android.data_du_lieu.model.*;
import com.example.btl_android.utils_tien_ich.*;
import com.example.btl_android.ui_giao_dien.auth_dang_nhap.*;
import com.example.btl_android.ui_giao_dien.dashboard_trang_chu.*;
import com.example.btl_android.ui_giao_dien.appointment_lich_hen.*;
import com.example.btl_android.ui_giao_dien.medical_kham_benh.*;
import com.example.btl_android.ui_giao_dien.medical_kham_benh.adapter.*;
import com.example.btl_android.ui_giao_dien.payment_thanh_toan.*;
import com.example.btl_android.ui_giao_dien.payment_thanh_toan.adapter.*;
import com.example.btl_android.ui_giao_dien.info_tin_tuc_ho_tro.*;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.Date;

/**
 * Quản lý thông báo đẩy (Heads-up Push Notifications) hiển thị ở phía trên màn hình.
 * Lắng nghe theo thời gian thực các sự kiện khám bệnh, đặt lịch hẹn và đánh giá
 * cho cả Người bệnh và Bác sĩ.
 */
public final class AppNotificationManager {

    public static final String CHANNEL_ID = "unicare_push_channel";
    private static final String CHANNEL_NAME = "Thông báo UniCare";
    private static final String CHANNEL_DESC = "Cập nhật lịch khám, kết quả khám và đơn thuốc";

    private static ListenerRegistration apptListener;
    private static ListenerRegistration ratingListener;
    private static String currentListeningUid = "";

    private AppNotificationManager() {}

    /**
     * Tạo Notification Channel với mức ưu tiên cao (IMPORTANCE_HIGH)
     * để thông báo xuất hiện nổi (heads-up banner) ở phía trên màn hình.
     */
    public static void initChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            channel.setSound(null, null);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Hiển thị thông báo đẩy nổi ở thanh trạng thái trên cùng của điện thoại.
     */
    public static void showNotification(Context context, int notificationId, String title, String message, Intent intent) {
        if (context == null) return;
        initChannel(context);

        // Kiểm tra quyền trên Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        PendingIntent pendingIntent = null;
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            pendingIntent = PendingIntent.getActivity(context, notificationId, intent, flags);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(0)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            // Không có quyền thì bỏ qua an toàn
        }
    }

    /**
     * Bắt đầu lắng nghe thời gian thực các sự kiện của tài khoản hiện tại.
     * Khi bác sĩ khám xong hoặc bệnh nhân đặt lịch, thông báo sẽ nổi ở phía trên điện thoại.
     */
    public static void startListening(Context context, String uid, String role) {
        if (context == null || uid == null || uid.isEmpty()) return;

        // Nếu đang lắng nghe tài khoản này rồi thì không tạo trùng listener
        if (apptListener != null && uid.equals(currentListeningUid)) {
            return;
        }

        stopListening();
        currentListeningUid = uid;
        initChannel(context);

        final Context appContext = context.getApplicationContext();
        final SharedPreferences prefs = appContext.getSharedPreferences("unicare_notif_" + uid, Context.MODE_PRIVATE);

        // 1. Lắng nghe lịch khám (Appointments)
        String field = "doctor".equals(role) ? "doctorId" : "patientId";
        apptListener = App.db().collection("appointments")
                .whereEqualTo(field, uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    processAppointments(appContext, snap, role, prefs);
                });

        // 2. Nếu là bác sĩ, lắng nghe thêm đánh giá từ người bệnh (Ratings)
        if ("doctor".equals(role)) {
            ratingListener = App.db().collection("ratings")
                    .whereEqualTo("doctorId", uid)
                    .addSnapshotListener((snap, e) -> {
                        if (e != null || snap == null) return;
                        processRatings(appContext, snap, prefs);
                    });
        }
    }

    /**
     * Dùng để chặn thông báo khi chính user đang đăng nhập thực hiện hành động.
     */
    public static void muteLocalAction(Context context, String apptId, String status) {
        if (context == null || currentListeningUid.isEmpty()) return;
        SharedPreferences prefs = context.getSharedPreferences("unicare_notif_" + currentListeningUid, Context.MODE_PRIVATE);
        prefs.edit().putBoolean("notif_" + apptId + "_" + status, true).apply();
    }

    private static void processAppointments(Context context, QuerySnapshot snap, String role, SharedPreferences prefs) {
        long now = System.currentTimeMillis();
        long maxAgeMillis = 3L * 24 * 60 * 60 * 1000L; // Giới hạn thông báo các sự kiện trong vòng 3 ngày gần nhất

        for (DocumentSnapshot d : snap.getDocuments()) {
            String apptId = d.getId();
            String status = App.str(d, "status");
            String doctorName = App.str(d, "doctorName");
            String patientName = App.str(d, "patientName");
            Timestamp startAt = d.getTimestamp("startAt");
            Timestamp updatedAt = d.getTimestamp("updatedAt");
            Timestamp createdAt = d.getTimestamp("createdAt");

            // Chọn mốc thời gian cập nhật gần nhất
            Timestamp eventTs = updatedAt != null ? updatedAt : (createdAt != null ? createdAt : startAt);
            if (eventTs == null) continue;

            Date eventDate = eventTs.toDate();
            // Nếu sự kiện quá cũ (trên 3 ngày), chỉ lưu cờ đã xem để không spam
            boolean isRecent = (now - eventDate.getTime()) <= maxAgeMillis;

            // Chỉ sử dụng thêm eventTs (giây) vào key với trạng thái COMPLETED 
            // để nếu bác sĩ cập nhật lại phiếu khám thì bệnh nhân mới nhận lại thông báo.
            // Các trạng thái khác (đặt lịch, hủy lịch) chỉ thông báo 1 lần duy nhất 
            // để tránh bị lặp 2 thông báo (do cơ chế đồng bộ Local/Server của Firebase).
            String key = "notif_" + apptId + "_" + status;
            if ("COMPLETED".equals(status)) {
                key += "_" + eventTs.getSeconds();
            }
            
            if (prefs.getBoolean(key, false)) {
                continue; // Đã thông báo rồi thì bỏ qua
            }

            // Đánh dấu đã xử lý
            prefs.edit().putBoolean(key, true).apply();

            if (!isRecent) {
                continue;
            }

            int notifId = Math.abs(key.hashCode());

            if ("patient".equals(role)) {
                // Người bệnh nhận thông báo từ Bác sĩ
                if ("COMPLETED".equals(status)) {
                    // BÁC SĨ KHÁM XONG HOẶC SỬA LẠI PHIẾU KHÁM
                    Intent intent = new Intent(context, RecordsActivity.class);
                    showNotification(
                            context,
                            notifId,
                            "🩺 Cập nhật kết quả khám",
                            "Bác sĩ " + (doctorName.isEmpty() ? "phụ trách" : doctorName)
                                    + " đã cập nhật kết quả khám và đơn thuốc cho bạn. Chạm để xem chi tiết.",
                            intent
                    );
                } else if ("CONFIRMED".equals(status)) {
                    // BÁC SĨ XÁC NHẬN LỊCH KHÁM
                    Intent intent = new Intent(context, AppointmentsActivity.class);
                    showNotification(
                            context,
                            notifId,
                            "✅ Lịch khám đã được xác nhận",
                            "Bác sĩ " + (doctorName.isEmpty() ? "phụ trách" : doctorName)
                                    + " đã xác nhận lịch hẹn vào lúc " + App.date(startAt) + ". Vui lòng đến đúng giờ.",
                            intent
                    );
                } else if ("CANCELLED".equals(status)) {
                    // BÁC SĨ TỪ CHỐI LỊCH
                    Intent intent = new Intent(context, AppointmentsActivity.class);
                    showNotification(
                            context,
                            notifId,
                            "⚠️ Lịch hẹn bị từ chối",
                            "Bác sĩ " + (doctorName.isEmpty() ? "phụ trách" : doctorName)
                                    + " đã từ chối lịch khám vào lúc " + App.date(startAt) + ". Vui lòng đặt lại lịch khác.",
                            intent
                    );
                }
            } else if ("doctor".equals(role)) {
                // Bác sĩ nhận thông báo từ Bệnh nhân
                if ("PENDING".equals(status)) {
                    // BỆNH NHÂN ĐẶT LỊCH KHÁM MỚI
                    Intent intent = new Intent(context, AppointmentsActivity.class);
                    showNotification(
                            context,
                            notifId,
                            "📅 Lịch hẹn khám mới",
                            "Bệnh nhân " + (patientName.isEmpty() ? "Người bệnh" : patientName)
                                    + " vừa đặt lịch khám lúc " + App.date(startAt) + ". Chạm để xem và xác nhận.",
                            intent
                    );
                } else if ("CANCELLED".equals(status)) {
                    // BỆNH NHÂN HỦY LỊCH
                    Intent intent = new Intent(context, AppointmentsActivity.class);
                    showNotification(
                            context,
                            notifId,
                            "⚠️ Lịch hẹn bị hủy",
                            "Bệnh nhân " + (patientName.isEmpty() ? "Người bệnh" : patientName)
                                    + " đã chủ động hủy lịch khám lúc " + App.date(startAt) + ".",
                            intent
                    );
                }
            }
        }
    }

    private static void processRatings(Context context, QuerySnapshot snap, SharedPreferences prefs) {
        long now = System.currentTimeMillis();
        long maxAgeMillis = 3L * 24 * 60 * 60 * 1000L;

        for (DocumentSnapshot d : snap.getDocuments()) {
            String ratingId = d.getId();
            Timestamp ratedAt = d.getTimestamp("ratedAt");
            Long stars = d.getLong("stars");
            if (ratedAt == null) continue;

            boolean isRecent = (now - ratedAt.toDate().getTime()) <= maxAgeMillis;
            String key = "notif_rating_" + ratingId;
            if (prefs.getBoolean(key, false)) {
                continue;
            }

            prefs.edit().putBoolean(key, true).apply();

            if (!isRecent) continue;

            int notifId = Math.abs(key.hashCode());
            Intent intent = new Intent(context, DoctorStatsActivity.class);
            showNotification(
                    context,
                    notifId,
                    "⭐ Đánh giá mới từ người bệnh",
                    "Bạn vừa nhận được đánh giá " + (stars != null ? stars : 5)
                            + " sao từ bệnh nhân. Chạm để xem thống kê hoạt động.",
                    intent
            );
        }
    }

    /**
     * Dừng lắng nghe khi đăng xuất hoặc thoát ứng dụng.
     */
    public static void stopListening() {
        if (apptListener != null) {
            apptListener.remove();
            apptListener = null;
        }
        if (ratingListener != null) {
            ratingListener.remove();
            ratingListener = null;
        }
        currentListeningUid = "";
    }
}

