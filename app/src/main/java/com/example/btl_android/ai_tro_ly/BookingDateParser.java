package com.example.btl_android.ai_tro_ly;

import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves common Vietnamese booking dates locally, without asking an AI to guess.
 * All dates use the clinic's Vietnam timezone. Does not create any appointment.
 */
public final class BookingDateParser {
    private static final TimeZone VIETNAM = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
    private static final Pattern ISO = Pattern.compile("(?<!\\d)(\\d{4}-\\d{1,2}-\\d{1,2})(?!\\d)");
    private static final Pattern DAY_MONTH = Pattern.compile("(?<!\\d)(\\d{1,2})[/.-](\\d{1,2})(?:[/.-](\\d{4}))?(?!\\d)");
    private static final Pattern WEEKDAY = Pattern.compile("\\bthu\\s*(hai|ba|tu|nam|sau|bay|[2-7])\\b|\\bchu\\s*nhat\\b");

    private BookingDateParser() {}

    public static final class Result {
        public final String date;
        public final int weekday; // Calendar.SUNDAY = 1 ... SATURDAY = 7
        public final String error;
        private Result(String date, int weekday, String error) {
            this.date = date;
            this.weekday = weekday;
            this.error = error;
        }
        public boolean isValid() { return error == null; }
    }

    public static Result parse(String request) { return parse(request, new Date()); }

    /** Public overload allows deterministic tests. */
    public static Result parse(String request, Date now) {
        if (request == null || request.trim().isEmpty())
            return problem("Hãy nhập ngày khám mong muốn.");
        Calendar today = Calendar.getInstance(VIETNAM, Locale.forLanguageTag("vi-VN"));
        today.setTime(now);
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        String input = normalize(request);
        Calendar target = null;

        // A numeric date is less ambiguous than a natural-language weekday.
        Matcher iso = ISO.matcher(input);
        if (iso.find()) {
            target = parseExactDate(iso.group(1), "yyyy-M-d");
            if (target == null) return problem("Ngày không hợp lệ, hãy nhập ngày đúng định dạng dd/MM/yyyy.");
        } else {
            Matcher dmy = DAY_MONTH.matcher(input);
            if (dmy.find()) {
                int day = Integer.parseInt(dmy.group(1));
                int month = Integer.parseInt(dmy.group(2));
                String yearString = dmy.group(3);
                int year = yearString == null ? today.get(Calendar.YEAR) : Integer.parseInt(yearString);
                target = makeDate(year, month, day);
                if (target == null) return problem("Ngày không hợp lệ. Ví dụ: 19/11/2026.");
                if (yearString == null && target.before(today)) {
                    target = makeDate(year + 1, month, day);
                    if (target == null) return problem("Ngày không hợp lệ.");
                }
            }
        }

        if (target == null) {
            if (has(input, "ngay kia") || has(input, "mot ngay nua sau ngay mai")) {
                target = (Calendar) today.clone();
                target.add(Calendar.DAY_OF_MONTH, 2);
            } else if (has(input, "ngay mai")) {
                target = (Calendar) today.clone();
                target.add(Calendar.DAY_OF_MONTH, 1);
            } else if (has(input, "hom nay")) {
                target = (Calendar) today.clone();
            } else {
                Matcher w = WEEKDAY.matcher(input);
                if (w.find()) {
                    int wanted = w.group(1) == null ? Calendar.SUNDAY : weekday(w.group(1));
                    if (wanted == -1) return problem("Chưa xác định được thứ trong tuần.");
                    target = (Calendar) today.clone();
                    boolean nextWeek = has(input, "tuan toi") || has(input, "tuan sau") || has(input, "tuan ke");
                    boolean thisWeek = has(input, "tuan nay");
                    if (nextWeek || thisWeek) {
                        // Week starts Monday, ends Sunday in Vietnam.
                        int daysSinceMonday = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7;
                        target.add(Calendar.DAY_OF_MONTH, -daysSinceMonday + (nextWeek ? 7 : 0));
                        int fromMonday = (wanted + 5) % 7;
                        target.add(Calendar.DAY_OF_MONTH, fromMonday);
                    } else {
                        int delta = (wanted - today.get(Calendar.DAY_OF_WEEK) + 7) % 7;
                        target.add(Calendar.DAY_OF_MONTH, delta);
                    }
                }
            }
        }
        if (target == null)
            return problem("Bạn muốn khám ngày nào? Ví dụ: khám mắt chiều thứ Năm tuần tới hoặc 19/11/2026.");

        long diffDays = (target.getTimeInMillis() - today.getTimeInMillis()) / (24L * 60 * 60 * 1000);
        if (diffDays < 0) return problem("Ngày đã qua. Hãy chọn một ngày từ hôm nay trở đi.");
        if (diffDays > 90) return problem("Chỉ hỗ trợ tìm lịch trong 90 ngày tới.");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        fmt.setTimeZone(VIETNAM);
        return new Result(fmt.format(target.getTime()), target.get(Calendar.DAY_OF_WEEK), null);
    }

    private static boolean has(String input, String exact) {
        return Pattern.compile("(?:^|\\s)" + Pattern.quote(exact) + "(?:$|\\s|[.,!?])").matcher(input).find();
    }

    private static Calendar parseExactDate(String value, String format) {
        SimpleDateFormat df = new SimpleDateFormat(format, Locale.US);
        df.setTimeZone(VIETNAM);
        df.setLenient(false);
        try {
            Date parsed = df.parse(value);
            Calendar c = Calendar.getInstance(VIETNAM, Locale.US);
            c.setTime(parsed);
            return c;
        } catch (ParseException e) {
            return null;
        }
    }

    private static Calendar makeDate(int year, int month, int day) {
        Calendar c = Calendar.getInstance(VIETNAM, Locale.US);
        c.clear();
        c.setLenient(false);
        c.set(year, month - 1, day);
        try { c.getTime(); return c; } catch (IllegalArgumentException e) { return null; }
    }

    private static int weekday(String text) {
        switch (text) {
            case "hai": case "2": return Calendar.MONDAY;
            case "ba": case "3": return Calendar.TUESDAY;
            case "tu": case "4": return Calendar.WEDNESDAY;
            case "nam": case "5": return Calendar.THURSDAY;
            case "sau": case "6": return Calendar.FRIDAY;
            case "bay": case "7": return Calendar.SATURDAY;
            default: return -1;
        }
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
    }
    private static Result problem(String msg) { return new Result(null, 0, msg); }
}
