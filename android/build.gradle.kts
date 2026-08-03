// Top-level build file. Individual module build files declare their own
// plugin versions; this file just applies the version catalog aliases so
// Android Studio's project sync resolves plugin versions consistently.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    // Kotlin 2.0 moved the Compose compiler off the old per-library
    // "kotlinCompilerExtensionVersion" scheme (which only went up to 1.5.x
    // and is NOT compatible with Kotlin 2.0+) onto this Gradle plugin -
    // compiler version is now implied by the Kotlin version automatically.
    // See app/build.gradle.kts.
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
