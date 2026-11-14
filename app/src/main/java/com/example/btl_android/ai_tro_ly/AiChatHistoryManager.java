package com.example.btl_android.ai_tro_ly;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý lưu trữ và tải lịch sử các đoạn chat của người dùng.
 * Lưu trữ bền vững theo uid người dùng trong SharedPreferences.
 */
public final class AiChatHistoryManager {
    private static final String PREF_NAME = "unicare_ai_chat_pref";

    public static class ChatMessage {
        public final boolean fromUser;
        public final String text;
        public final long timestamp;

        public ChatMessage(boolean fromUser, String text, long timestamp) {
            this.fromUser = fromUser;
            this.text = text;
            this.timestamp = timestamp;
        }

        public JSONObject toJson() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("fromUser", fromUser);
                obj.put("text", text);
                obj.put("timestamp", timestamp);
                return obj;
            } catch (Exception e) {
                return null;
            }
        }

        public static ChatMessage fromJson(JSONObject obj) {
            if (obj == null) return null;
            boolean fromUser = obj.optBoolean("fromUser", false);
            String text = obj.optString("text", "");
            long timestamp = obj.optLong("timestamp", System.currentTimeMillis());
            return new ChatMessage(fromUser, text, timestamp);
        }
    }

    public static class ChatSession {
        public final String id;
        public String title;
        public long updatedAt;
        public final List<ChatMessage> messages = new ArrayList<>();

        public ChatSession(String id, String title, long updatedAt) {
            this.id = id;
            this.title = title;
            this.updatedAt = updatedAt;
        }

        public JSONObject toJson() {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", id);
                obj.put("title", title);
                obj.put("updatedAt", updatedAt);
                JSONArray arr = new JSONArray();
                for (ChatMessage m : messages) {
                    JSONObject mObj = m.toJson();
                    if (mObj != null) arr.put(mObj);
                }
                obj.put("messages", arr);
                return obj;
            } catch (Exception e) {
                return null;
            }
        }

        public static ChatSession fromJson(JSONObject obj) {
            if (obj == null) return null;
            String id = obj.optString("id", String.valueOf(System.currentTimeMillis()));
            String title = obj.optString("title", "Cuộc trò chuyện");
            long updatedAt = obj.optLong("updatedAt", System.currentTimeMillis());
            ChatSession session = new ChatSession(id, title, updatedAt);
            JSONArray arr = obj.optJSONArray("messages");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject mObj = arr.optJSONObject(i);
                    ChatMessage m = ChatMessage.fromJson(mObj);
                    if (m != null) session.messages.add(m);
                }
            }
            return session;
        }
    }

    public static List<ChatSession> getSessions(Context context, String uid) {
        List<ChatSession> list = new ArrayList<>();
        if (context == null || uid == null || uid.isEmpty()) return list;
        SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String raw = pref.getString("sessions_" + uid, null);
        if (raw == null || raw.trim().isEmpty()) {
            return list;
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                ChatSession s = ChatSession.fromJson(obj);
                if (s != null) list.add(s);
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static void saveSession(Context context, String uid, ChatSession session) {
        if (context == null || uid == null || session == null) return;
        List<ChatSession> list = getSessions(context, uid);
        int foundIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(session.id)) {
                foundIndex = i;
                break;
            }
        }
        if (foundIndex >= 0) {
            list.remove(foundIndex);
        }
        // Thêm vào đầu danh sách (mới nhất lên trên)
        list.add(0, session);

        // Giới hạn lưu tối đa 30 đoạn chat gần nhất
        while (list.size() > 30) {
            list.remove(list.size() - 1);
        }

        persist(context, uid, list);
    }

    public static void deleteSession(Context context, String uid, String sessionId) {
        if (context == null || uid == null || sessionId == null) return;
        List<ChatSession> list = getSessions(context, uid);
        boolean removed = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(sessionId)) {
                list.remove(i);
                removed = true;
                break;
            }
        }
        if (removed) {
            persist(context, uid, list);
        }
    }

    private static void persist(Context context, String uid, List<ChatSession> list) {
        try {
            JSONArray arr = new JSONArray();
            for (ChatSession s : list) {
                JSONObject obj = s.toJson();
                if (obj != null) arr.put(obj);
            }
            SharedPreferences pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            pref.edit().putString("sessions_" + uid, arr.toString()).apply();
        } catch (Exception ignored) {}
    }
}

