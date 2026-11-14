package com.example.btl_android.ai_tro_ly;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import androidx.fragment.app.Fragment;
import com.example.btl_android.R;
import java.util.WeakHashMap;

/** Floating robot button on HomeFragment; snap only to left / right edge. */
public final class AiFloatingBubble {
    private static final WeakHashMap<Fragment, View> ACTIVE = new WeakHashMap<>();
    private static final String PREFS = "unicare_ai_bubble_position";
    private AiFloatingBubble() {}

    private static int dp(Context context, float value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + .5f);
    }
    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(v, Math.max(min, max)));
    }

    public static void attach(Fragment fragment) {
        if (!fragment.isAdded() || fragment.getView() == null) return;
        if (ACTIVE.containsKey(fragment)) return;
        Activity activity = fragment.requireActivity();
        FrameLayout parent = activity.findViewById(android.R.id.content);
        if (parent == null) return;

        View bubble = LayoutInflater.from(activity).inflate(R.layout.view_ai_floating_bubble, parent, false);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            dp(activity, 84), dp(activity, 86), Gravity.LEFT | Gravity.TOP);
        parent.addView(bubble, lp);
        ACTIVE.put(fragment, bubble);

        SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        final float margin = dp(activity, 8);
        final float topReserve = dp(activity, 12);
        final float bottomReserve = dp(activity, 92); // Avoid covering bottom navigation.

        bubble.post(() -> {
            if (bubble.getParent() != parent) return;
            float left = margin;
            float right = Math.max(left, parent.getWidth() - bubble.getWidth() - margin);
            float bottom = Math.max(topReserve,
                parent.getHeight() - bubble.getHeight() - bottomReserve);
            float fraction = prefs.getFloat("yFraction", .78f);
            bubble.setX(prefs.getBoolean("leftSide", false) ? left : right);
            bubble.setY(topReserve + clamp(fraction, 0f, 1f) * (bottom - topReserve));
        });

        bubble.setOnClickListener(v -> {
            if (fragment.isAdded()) {
                fragment.startActivity(new Intent(activity, AiAssistantActivity.class));
            }
        });

        final int slop = ViewConfiguration.get(activity).getScaledTouchSlop();
        bubble.setOnTouchListener(new View.OnTouchListener() {
            private float downRawX, downRawY, downX, downY;
            private boolean moved;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().cancel();
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        downX = v.getX();
                        downY = v.getY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - downRawX;
                        float deltaY = event.getRawY() - downRawY;
                        if (!moved && Math.hypot(deltaX, deltaY) > slop) moved = true;
                        if (moved) {
                            float maxX = Math.max(margin, parent.getWidth() - v.getWidth() - margin);
                            float maxY = Math.max(topReserve,
                                parent.getHeight() - v.getHeight() - bottomReserve);
                            v.setX(clamp(downX + deltaX, margin, maxX));
                            v.setY(clamp(downY + deltaY, topReserve, maxY));
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            v.performClick(); // Open only on real tap, NEVER on drag.
                        } else {
                            snap(v);
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        snap(v);
                        return true;
                    default:
                        return false;
                }
            }

            private void snap(View v) {
                float left = margin;
                float right = Math.max(left, parent.getWidth() - v.getWidth() - margin);
                float mid = (parent.getWidth() - v.getWidth()) * .5f;
                boolean toLeft = v.getX() < mid;
                float maxY = Math.max(topReserve,
                    parent.getHeight() - v.getHeight() - bottomReserve);
                float targetY = clamp(v.getY(), topReserve, maxY);
                float fraction = (maxY > topReserve)
                    ? (targetY - topReserve) / (maxY - topReserve) : .5f;
                prefs.edit().putBoolean("leftSide", toLeft)
                    .putFloat("yFraction", fraction).apply();
                v.animate().x(toLeft ? left : right).y(targetY)
                    .setDuration(190).setInterpolator(new DecelerateInterpolator()).start();
            }
        });
    }

    public static void detach(Fragment fragment) {
        View bubble = ACTIVE.remove(fragment);
        if (bubble == null) return;
        bubble.animate().cancel();
        if (bubble.getParent() instanceof ViewGroup) {
            ((ViewGroup) bubble.getParent()).removeView(bubble);
        }
    }
}
