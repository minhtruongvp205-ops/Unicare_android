plugins { alias(libs.plugins.android.application) }
// Before google-services.json is added, the IDE can open and sync the project.
// After adding app/google-services.json, Sync Gradle once again.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
android {
    namespace = "com.example.btl_android"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.btl_android"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "3.0-features"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(platform("com.google.firebase:firebase-bom:35.2.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
// Gemini AI
    implementation("com.google.firebase:firebase-ai")
// Dung cho ListenableFuture trong Java
    implementation("com.google.guava:guava:31.0.1-android")
// App Check, chi su dung debug provider khi test
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.cardview:cardview:1.0.0")
}
