plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.budsbattery"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.budsbattery"
        // === NOTE: === Android 14 is the first version with the companion presence APIs this app needs
        minSdk = 34
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}

// Every debug build lands on the Desktop, so the APK is always one tap from the phone
val copyApkToDesktop by tasks.registering(Copy::class) {
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    into(File(System.getProperty("user.home"), "Desktop"))
    rename { "buds-battery.apk" }
}

tasks.matching { it.name == "assembleDebug" }.configureEach { finalizedBy(copyApkToDesktop) }
