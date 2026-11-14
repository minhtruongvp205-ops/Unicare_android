package com.example.btl_android.ai_tro_ly;

/**
 * Trợ lý phân luồng ý định người dùng cho AI (chuẩn hóa tên với tiền tố Ai).
 */
public final class AiBookingIntentRouter {
    private AiBookingIntentRouter() {}

    public static boolean isAppointmentRequest(String raw) {
        return BookingIntentRouter.isAppointmentRequest(raw);
    }
}

