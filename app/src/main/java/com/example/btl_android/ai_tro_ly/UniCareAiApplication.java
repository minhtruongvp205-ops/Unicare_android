package com.example.btl_android.ai_tro_ly;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

/** Register App Check before calling the Gemini Firebase SDK. */
public class UniCareAiApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        if (FirebaseApp.initializeApp(this) == null) return;
        FirebaseAppCheck appCheck = FirebaseAppCheck.getInstance();
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            // Dev only. Add the Logcat debug token in Firebase Console > App Check.
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance());
        } else {
            // Register Play Integrity for release in Firebase Console before publishing.
            appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance());
        }
    }
}
