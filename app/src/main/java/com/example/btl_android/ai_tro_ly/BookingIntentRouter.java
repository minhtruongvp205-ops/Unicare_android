package com.example.btl_android.ai_tro_ly;

import java.text.Normalizer;
import java.util.Locale;

/** 
 * Phân luồng câu hỏi của người dùng:
 * 1. Nếu hỏi về thông tin cá nhân (thuốc, liều lượng, bệnh tật, chi phí, chỉ số sức khỏe) -> Trả về false để AI trả lời từ hồ sơ bệnh nhân.
 * 2. Nếu hỏi về đặt lịch, tìm bác sĩ, hoặc mô tả triệu chứng muốn khám -> Trả về true để AI gợi ý chuyên khoa và giờ khám thật từ Firestore.
 */
public final class BookingIntentRouter {
    private BookingIntentRouter() {}

    public static boolean isAppointmentRequest(String raw) {
        if (raw == null) return false;
        String s = normalize(raw);
        if (s.isEmpty()) return false;

        // Nếu là các câu hỏi hướng dẫn chung
        if (s.contains("huong dan") || s.contains("cach dat") || s.contains("lam sao de dat") ||
                s.contains("gioi thieu") || s.contains("tai sao")) return false;

        // Nếu là câu hỏi về dữ liệu y tế cá nhân của bệnh nhân -> KHÔNG chuyển sang đặt lịch
        if (isPersonalDataQuestion(s)) {
            return false;
        }

        // Các từ khóa đặt lịch, tìm bác sĩ hoặc hỏi chuyên khoa
        if (s.contains("dat lich") || s.contains("dat hen") || s.contains("hen kham") ||
                s.contains("tim lich kham") || s.contains("tim gio kham") ||
                s.contains("tim bac si") || s.contains("goi y bac si") || s.contains("goi y chuyen khoa") ||
                s.contains("toi muon kham") || s.contains("minh muon kham") || s.contains("can kham") ||
                s.contains("kham chuyen khoa") || s.contains("kham o dau") || s.contains("kham bac si nao") ||
                s.contains("bac si nao") || s.contains("co lich kham") || s.contains("con lich kham")) {
            return true;
        }

        // Mô tả triệu chứng cụ thể muốn đi khám hoặc nhờ tư vấn bác sĩ
        if (s.contains("kham") || s.contains("bac si") || s.contains("benh vien")) {
            if (s.contains("dau ") || s.contains("sot") || s.contains("ho ") || s.contains("viem") ||
                    s.contains("ngua") || s.contains("kho tho") || s.contains("chong mat") ||
                    s.contains("met moi") || s.contains("trieu chung")) {
                return true;
            }
        }

        return false;
    }

    private static boolean isPersonalDataQuestion(String s) {
        // Hỏi về thuốc & liều lượng
        if (s.contains("lieu luong") || s.contains("uong thuoc") || s.contains("thuoc cua toi") ||
                s.contains("cach uong") || s.contains("uong nhu the nao") || s.contains("uong ra sao") ||
                s.contains("don thuoc") || s.contains("uong khi nao")) {
            return true;
        }
        // Hỏi về bệnh đang mắc
        if (s.contains("bi benh gi") || s.contains("mac benh gi") || s.contains("benh cua toi") ||
                s.contains("chuan doan") || s.contains("ket qua kham") || s.contains("benh an cua toi")) {
            return true;
        }
        // Hỏi về chi phí & viện phí
        if (s.contains("chi phi") || s.contains("vien phi") || s.contains("tien kham") ||
                s.contains("bao nhieu tien") || s.contains("hoa don") || s.contains("thanh toan")) {
            return true;
        }
        // Hỏi về chỉ số sức khỏe & độ an toàn / duy trì
        if (s.contains("chi so") || s.contains("an toan") || s.contains("duy tri") ||
                s.contains("huyet ap cua toi") || s.contains("nhip tim cua toi") || s.contains("duong huyet cua toi") ||
                s.contains("suc khoe cua toi co")) {
            return true;
        }
        return false;
    }

    private static String normalize(String raw) {
        return Normalizer.normalize(raw.toLowerCase(Locale.ROOT).replace('đ','d'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
    }
}
