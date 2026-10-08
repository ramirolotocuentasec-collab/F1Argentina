plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android { namespace = "com.ramiro.f1argentina"; compileSdk = 36
    defaultConfig { applicationId = "com.ramiro.f1argentina"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "1.0" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.core:core:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui:1.9.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.2")
    implementation("androidx.compose.material3:material3:1.4.0")
}
