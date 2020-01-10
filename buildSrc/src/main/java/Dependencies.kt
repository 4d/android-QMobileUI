import org.gradle.api.JavaVersion

object Versions {
    val androidmobileapi = "0.0.1@aar"
    val androidmobiledatastore = "0.0.1"
    val androidmobileui = "0.0.1"

    val android_gradle_plugin = "3.5.2"
    val arch_core = "2.0.0"
    val artifactory = "4.13.0"
    val atsl_junit = "1.1.1"
    val constraint_layout = "2.0.0-beta3"
    val design = "1.0.0"
    val espresso = "3.2.0"
    val glide = "4.9.0"
    val junit = "4.12"
    val kotlin = "1.3.61"
    val mockito = "3.2.0"
    val multidex = "1.0.3"
    val navigation = "2.2.0-rc03"
    val okhttp = "4.2.2"
    val retrofit = "2.6.2"
    val robolectric = "4.3.1"
    val room = "2.2.2"
    val rules = "1.1.0"
    val runner = "1.1.0"
    val rx_android = "2.1.1"
    val rxjava2 = "2.1.3"
    val support = "1.1.0"
    val timber = "4.7.1"
}

object Config {
    val minSdk = 19
    val compileSdk = 29
    val targetSdk = 29
    val buildTools = "29.0.2"
    val javaVersion = JavaVersion.VERSION_1_8
}

object Tools {
    val gradle = "com.android.tools.build:gradle:${Versions.android_gradle_plugin}"
    val kotlin_gradle_plugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    val navigation_safe_args_gradle_plugin =
        "androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.navigation}"
    val artifactory = "org.jfrog.buildinfo:build-info-extractor-gradle:${Versions.artifactory}"
}

object AndroidMobileLibs {
    val androidmobileapi =
        "com.qmarciset.androidmobileapi:androidmobileapi:${Versions.androidmobileapi}"
    val androidmobiledatastore =
        "com.qmarciset.androidmobiledatastore:androidmobiledatastore:${Versions.androidmobiledatastore}"
    val androidmobileui =
        "com.qmarciset.androidmobileui:androidmobileui:${Versions.androidmobileui}"
}

object Libs {
    // Support
    val androidx_appcompat = "androidx.appcompat:appcompat:${Versions.support}"
    val androidx_recyclerview = "androidx.recyclerview:recyclerview:${Versions.support}"
    val androidx_core = "androidx.core:core-ktx:${Versions.support}"

    // Room
    val androidx_room_runtime = "androidx.room:room-runtime:${Versions.room}"
    val androidx_room = "androidx.room:room-ktx:${Versions.room}"
    val androidx_room_compiler = "androidx.room:room-compiler:${Versions.room}"
    val androidx_room_testing = "androidx.room:room-testing:${Versions.room}"

    // Arch core
    val androidx_core_testing = "androidx.arch.core:core-testing:${Versions.arch_core}"

    // Retrofit
    val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
    val retrofit_converter_gson = "com.squareup.retrofit2:converter-gson:${Versions.retrofit}"
    val retrofit_adapter_rxjava2 = "com.squareup.retrofit2:adapter-rxjava2:${Versions.retrofit}"

    // OkHttp
    val okhttp = "com.squareup.okhttp3:okhttp:${Versions.okhttp}"
    val okhttp_logging_intercepter = "com.squareup.okhttp3:logging-interceptor:${Versions.okhttp}"
    val okhttp_mockwebserver = "com.squareup.okhttp3:mockwebserver:${Versions.okhttp}"

    // Espresso
    val androidx_espresso = "androidx.test.espresso:espresso-core:${Versions.espresso}"
    val espresso_contrib = "com.android.support.test.espresso:espresso-contrib:${Versions.espresso}"
    val androidx_runner = "androidx.test:runner:${Versions.runner}"
    val androidx_rules = "androidx.test:rules:${Versions.rules}"

    // Atsl
    val androidx_junit_ktx = "androidx.test.ext:junit-ktx:${Versions.atsl_junit}"
    val androidx_junit = "androidx.test.ext:junit:${Versions.atsl_junit}"

    // Mockito
    val mockito = "org.mockito:mockito-core:${Versions.mockito}"
    val mockito_android = "org.mockito:mockito-android:${Versions.mockito}"
    val mockito_inline = "org.mockito:mockito-inline:${Versions.mockito}"

    // Kotlin
    val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
    val kotlin_reflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"

    // Navigation
    val androidx_navigation_fragment =
        "androidx.navigation:navigation-fragment-ktx:${Versions.navigation}"
    val androidx_navigation_ui = "androidx.navigation:navigation-ui-ktx:${Versions.navigation}"

    // Glide
    val glide = "com.github.bumptech.glide:glide:${Versions.glide}"

    val androidx_constraintlayout =
        "androidx.constraintlayout:constraintlayout:${Versions.constraint_layout}"
    val rxjava = "io.reactivex.rxjava2:rxjava:${Versions.rxjava2}"
    val rxandroid = "io.reactivex.rxjava2:rxandroid:${Versions.rx_android}"
    val timber = "com.jakewharton.timber:timber:${Versions.timber}"
    val androidx_preference_ktx = "androidx.preference:preference-ktx:1.1.0"
    val material = "com.google.android.material:material:${Versions.design}"
    val databinding_compiler = "com.android.databinding:compiler:3.2.0-alpha10"
    val multidex = "com.android.support:multidex:${Versions.multidex}"
    val junit = "junit:junit:${Versions.junit}"
    val robolectric = "org.robolectric:robolectric:${Versions.robolectric}"
}