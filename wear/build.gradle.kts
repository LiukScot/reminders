plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.liukscot.reminders.wear"
    compileSdk = 37

    defaultConfig {
        // Same applicationId as the phone app: that is what pairs the two halves, so the
        // watch app installs alongside its phone counterpart instead of as a stranger.
        applicationId = "com.liukscot.reminders"
        // Wear OS 3 and up. Nothing below it runs Compose for Wear well enough to matter.
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    // Two files are shared with :app rather than duplicated. Both are dependency-free — pure
    // java.time logic and raw color constants — which is the whole reason this works without a
    // third module. ponytail: srcDir sharing, extract a :shared module the moment a third file
    // needs it or either file grows an Android dependency.
    sourceSets["main"].kotlin.directories.addAll(
        listOf(
            "../app/src/main/java/com/liukscot/reminders/data/SnoozeOption.kt",
            "../app/src/main/java/com/liukscot/reminders/ui/theme/Color.kt",
        ),
    )

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.wear.compose:compose-material3:1.6.2")
    implementation("androidx.wear.compose:compose-foundation:1.6.2")
    implementation("androidx.wear.compose:compose-navigation:1.6.2")
    implementation("androidx.wear:wear-tooling-preview:1.0.0")

    // Complication that launches voice capture from the watch face.
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.3.0")

    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
