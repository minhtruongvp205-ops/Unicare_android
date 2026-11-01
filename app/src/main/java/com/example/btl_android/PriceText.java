package com.example.btl_android;

import java.text.NumberFormat;
import java.util.Locale;

public final class PriceText {
    private PriceText() {}
    public static String vnd(long amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount) + " đ";
    }
}
