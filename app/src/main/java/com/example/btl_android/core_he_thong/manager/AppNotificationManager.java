package com.example.btl_android.core_he_thong.manager;

import android.content.Context;

/**
 * Lớp điều hướng tương thích ngược.
 * Mã nguồn chính đặt tại com.example.btl_android.core_he_thong.manager_quan_ly.AppNotificationManager.
 */
public final class AppNotificationManager {
    public static final String CHANNEL_ID = com.example.btl_android.core_he_thong.manager_quan_ly.AppNotificationManager.CHANNEL_ID;

    private AppNotificationManager() {}

    public static void startListening(Context context, String uid, String role) {
        com.example.btl_android.core_he_thong.manager_quan_ly.AppNotificationManager.startListening(context, uid, role);
    }

    public static void stopListening() {
        com.example.btl_android.core_he_thong.manager_quan_ly.AppNotificationManager.stopListening();
    }

    public static void muteLocalAction(Context context, String documentId, String actionType) {
        com.example.btl_android.core_he_thong.manager_quan_ly.AppNotificationManager.muteLocalAction(context, documentId, actionType);
    }
}
