package com.example.btl_android.ai_tro_ly;

import java.text.Normalizer;
import java.util.Locale;

/** Only intentional booking/search requests are routed away from ordinary chat. */
public final class BookingIntentRouter {
    private BookingIntentRouter() {}
    public static boolean isAppointmentRequest(String raw) {
        if (raw == null) return false;
        String s = Normalizer.normalize(raw.toLowerCase(Locale.ROOT).replace('đ','d'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
        if (s.isEmpty()) return false;
        // These should receive a normal explanation, not trigger booking.
        if (s.contains("huong dan") || s.contains("cach dat") || s.contains("lam sao") ||
                s.contains("gioi thieu") || s.contains("tai sao") || s.contains("co the dat lich khong")) return false;
        return s.contains("dat lich") || s.contains("dat hen") || s.contains("hen kham") ||
                s.contains("tim lich kham") || s.contains("tim gio kham") ||
                s.contains("tim bac si") || s.contains("goi y bac si") ||
                s.contains("toi muon kham") || s.contains("minh muon kham") ||
                s.contains("co lich kham") || s.contains("con lich kham");
    }
}
