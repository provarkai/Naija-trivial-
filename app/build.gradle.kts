import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

// Local-only backend config (never committed -- see local.properties.example).
// Falls back to empty strings, which makes AppContainer pick the mock
// generator; set these once you've deployed /server to get real AI output.
val localProperties = Properties().apply {
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { load(it) }
    }
}

// Release signing (never committed -- see keystore.properties.example and
// the "Release build / Play Store" section in the root README). Absent for
// contributors who don't have the upload key; release builds then fall
// back to being unsigned rather than failing the build outright.
val keystoreProperties = Properties().apply {
    val keystorePropsFile = rootProject.file("keystore.properties")
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystoreProperties.getProperty("storeFile")
    ?.let { rootProject.file(it).exists() } == true

// Google's published test AdMob App ID -- safe to use as a fallback in any
// build, never serves real ads. See https://developers.google.com/admob/android/test-ads
val GOOGLE_TEST_ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"

android {
    namespace = "com.ai4biz.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ai4biz.app"
        // Adaptive icons only ship for API 26+ in this scaffold; bump this down
        // once legacy raster launcher icons are added for older devices.
        minSdk = 26
        // Play Console requires targeting the current API level (35 as of
        // this writing) for new releases -- bump this each year Google
        // raises the bar, matching compileSdk above.
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "AI4BIZ_BACKEND_URL",
            "\"${localProperties.getProperty("ai4biz.backend.url", "")}\""
        )
        buildConfigField(
            "String",
            "AI4BIZ_BACKEND_SECRET",
            "\"${localProperties.getProperty("ai4biz.backend.secret", "")}\""
        )

        // AdMob. Google's public test IDs are used as the fallback for
        // *everything* here (app ID and all three ad unit IDs) so the app
        // never ships with a blank/invalid ad config -- see ads/AdsConfig.kt
        // for how debug builds always use test IDs regardless of what's
        // configured here.
        val admobAppId = localProperties.getProperty("admob.app_id", GOOGLE_TEST_ADMOB_APP_ID)
        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField(
            "String",
            "ADMOB_BANNER_UNIT_ID",
            "\"${localProperties.getProperty("admob.banner_unit_id", "")}\""
        )
        buildConfigField(
            "String",
            "ADMOB_INTERSTITIAL_UNIT_ID",
            "\"${localProperties.getProperty("admob.interstitial_unit_id", "")}\""
        )
        buildConfigField(
            "String",
            "ADMOB_REWARDED_UNIT_ID",
            "\"${localProperties.getProperty("admob.rewarded_unit_id", "")}\""
        )
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
