plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.fitcoachpro.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fitcoachpro.app"
        // minSdk 28 per CLAUDE.md / IMPLEMENTATION_STEPS.md - reliable Health
        // Connect support for later phases, even though Phase 1 doesn't use it yet.
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-phase1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            // Allows cleartext HTTP to a LAN backend during local development
            // (e.g. http://192.168.x.x:3000 before you've set up HTTPS via a
            // tunnel/VPS). Release builds do NOT get this - see network_security_config.
            isDebuggable = true
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
    }
    // No composeOptions/kotlinCompilerExtensionVersion block - that scheme
    // was retired in Kotlin 2.0. The org.jetbrains.kotlin.plugin.compose
    // plugin (applied above) picks the matching Compose compiler
    // automatically based on the Kotlin version.

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Networking - talks to the FitCoach Pro backend's /checkin route.
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Local key-value storage for backend URL / shared secret / reminder time.
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Local notification scheduling (Phase 1: daily check-in reminder).
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
