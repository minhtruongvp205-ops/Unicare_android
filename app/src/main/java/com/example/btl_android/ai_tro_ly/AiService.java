package com.example.btl_android.ai_tro_ly;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.Schema;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Gemini client. Chat and booking JSON use separate model configurations. */
public final class AiService {
    private static final String MODEL_NAME = "gemini-3.5-flash-lite";
    private final GenerativeModelFutures chatModel;
    private final GenerativeModelFutures bookingJsonModel;

    public interface Callback {
        void onSuccess(String text);
        void onFailure(String message);
    }

    public AiService() {
        FirebaseAI firebaseAI = FirebaseAI.getInstance(GenerativeBackend.googleAI());
        GenerativeModel base = firebaseAI.generativeModel(MODEL_NAME);
        chatModel = GenerativeModelFutures.from(base);

        // Official Firebase AI Logic Java Structured Output API.
        Map<String, Schema> properties = new HashMap<>();
        properties.put("specialty", Schema.str());
        properties.put("specialtyReason", Schema.str());
        properties.put("period", Schema.enumeration(Arrays.asList("morning", "afternoon", "evening", "any")));
        properties.put("time", Schema.str());
        Schema schema = Schema.obj(properties, Collections.emptyList());
        GenerationConfig.Builder cfg = new GenerationConfig.Builder();
        cfg.responseMimeType = "application/json";
        cfg.responseSchema = schema;
        bookingJsonModel = GenerativeModelFutures.from(firebaseAI.generativeModel(MODEL_NAME, cfg.build()));
    }

    /** Normal health chat and summary remain unchanged for AiAssistantActivity / AiRecordHelper. */
    public void ask(String question, Callback callback) {
        generate(chatModel, question, callback);
    }

    /** Guaranteed JSON shape by the backend (still validate semantic values in Java). */
    public void askBookingIntent(String prompt, Callback callback) {
        generate(bookingJsonModel, prompt, callback);
    }

    private static void generate(GenerativeModelFutures model, String prompt, Callback cb) {
        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> pending = model.generateContent(content);
        Futures.addCallback(pending, new FutureCallback<GenerateContentResponse>() {
            @Override public void onSuccess(GenerateContentResponse response) {
                String text = response == null ? null : response.getText();
                if (text == null || text.trim().isEmpty()) {
                    cb.onFailure("Gemini chưa trả lời, vui lòng thử lại.");
                } else {
                    // Loại bỏ các ký tự dấu sao (*) từ phản hồi của Gemini
                    String cleanText = text.replace("*", "").trim();
                    cb.onSuccess(cleanText);
                }
            }
            @Override public void onFailure(Throwable error) {
                cb.onFailure("Gemini tạm thời không xử lý được yêu cầu. " +
                        (error == null ? "" : String.valueOf(error.getLocalizedMessage())));
            }
        }, MoreExecutors.directExecutor());
    }
}
