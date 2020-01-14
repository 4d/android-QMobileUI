import org.gradle.api.JavaVersion

object Versions {
    // AndroidMobile libraries
    const val androidmobileapi = "0.0.1"
    const val androidmobiledatastore = "0.0.1"

    const val android_gradle_plugin = "3.5.2"
    const val artifactory = "4.13.0"
    const val constraint_layout = "2.0.0-beta3"
    const val design = "1.0.0"
    const val junit = "4.12"
    const val kotlin = "1.3.61"
    const val navigation = "2.2.0-rc03"
    const val okhttp = "4.2.2"
    const val retrofit = "2.6.2"
    const val rxjava2 = "2.1.3"
    const val support = "1.1.0"
    const val timber = "4.7.1"
}

object Config {
    const val minSdk = 19
    const val compileSdk = 29
    const val targetSdk = 29
    const val buildTools = "29.0.2"
    val javaVersion = JavaVersion.VERSION_1_8
}

object Tools {
    const val gradle = "com.android.tools.build:gradle:${Versions.android_gradle_plugin}"
    const val kotlin_gradle_plugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val artifactory =
        "org.jfrog.buildinfo:build-info-extractor-gradle:${Versions.artifactory}"
}

object AndroidMobileLibs {
    const val androidmobileapi =
        "com.qmarciset.androidmobileapi:androidmobileapi:${Versions.androidmobileapi}"
    const val androidmobiledatastore =
        "com.qmarciset.androidmobiledatastore:androidmobiledatastore:${Versions.androidmobiledatastore}"
}

object Libs {

    // Common
    const val androidx_appcompat = "androidx.appcompat:appcompat:${Versions.support}"
    const val androidx_constraintlayout =
        "androidx.constraintlayout:constraintlayout:${Versions.constraint_layout}"
    const val androidx_core = "androidx.core:core-ktx:${Versions.support}"
    const val androidx_recyclerview = "androidx.recyclerview:recyclerview:${Versions.support}"
    const val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"

    // Utils
    const val material = "com.google.android.material:material:${Versions.design}"
    const val okhttp = "com.squareup.okhttp3:okhttp:${Versions.okhttp}"
    const val retrofit_converter_gson = "com.squareup.retrofit2:converter-gson:${Versions.retrofit}"
    const val rxjava = "io.reactivex.rxjava2:rxjava:${Versions.rxjava2}"
    const val timber = "com.jakewharton.timber:timber:${Versions.timber}"

    // Navigation
    const val androidx_navigation_fragment =
        "androidx.navigation:navigation-fragment-ktx:${Versions.navigation}"
    const val androidx_navigation_ui =
        "androidx.navigation:navigation-ui-ktx:${Versions.navigation}"

    // Testing
    const val junit = "junit:junit:${Versions.junit}"
}