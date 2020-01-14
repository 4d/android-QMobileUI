import org.gradle.api.JavaVersion

object Versions {
    // AndroidMobile libraries
    val androidmobileapi = "0.0.1"
    val androidmobiledatastore = "0.0.1"

    val android_gradle_plugin = "3.5.2"
    val artifactory = "4.13.0"
    val constraint_layout = "2.0.0-beta3"
    val design = "1.0.0"
    val junit = "4.12"
    val kotlin = "1.3.61"
    val navigation = "2.2.0-rc03"
    val support = "1.1.0"
    val timber = "4.7.1"
}

object Config {
    val buildTools = "29.0.2"
    val compileSdk = 29
    val minSdk = 19
    val targetSdk = 29
    val javaVersion = JavaVersion.VERSION_1_8
}

object Tools {
    val artifactory =
        "org.jfrog.buildinfo:build-info-extractor-gradle:${Versions.artifactory}"
    val gradle = "com.android.tools.build:gradle:${Versions.android_gradle_plugin}"
    val kotlin_gradle_plugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
}

object AndroidMobileLibs {
    val androidmobileapi =
        "com.qmarciset.androidmobileapi:androidmobileapi:${Versions.androidmobileapi}"
    val androidmobiledatastore =
        "com.qmarciset.androidmobiledatastore:androidmobiledatastore:${Versions.androidmobiledatastore}"
}

object Libs {

    // Common
    val androidx_appcompat = "androidx.appcompat:appcompat:${Versions.support}"
    val androidx_constraintlayout =
        "androidx.constraintlayout:constraintlayout:${Versions.constraint_layout}"
    val androidx_core = "androidx.core:core-ktx:${Versions.support}"
    val androidx_recyclerview = "androidx.recyclerview:recyclerview:${Versions.support}"
    val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"

    // Utils
    val material = "com.google.android.material:material:${Versions.design}"
    val timber = "com.jakewharton.timber:timber:${Versions.timber}"

    // Navigation
    val androidx_navigation_fragment =
        "androidx.navigation:navigation-fragment-ktx:${Versions.navigation}"
    val androidx_navigation_ui =
        "androidx.navigation:navigation-ui-ktx:${Versions.navigation}"

    // Testing
    val junit = "junit:junit:${Versions.junit}"
}