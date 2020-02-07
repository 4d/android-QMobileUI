/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

import org.gradle.api.JavaVersion

object Versions {
    // AndroidMobile libraries
    val androidmobileapi = "0.0.1"
    val androidmobiledatastore = "0.0.1"

    val android_gradle_plugin = "3.5.2"
    val artifactory = "4.13.0"
    val atsl_junit = "1.1.1"
    val constraint_layout = "2.0.0-beta4"
    val design = "1.0.0"
    val junit = "4.13"
    val kotlin = "1.3.61"
    val mockito = "3.2.4"
    val navigation = "2.2.0"
    val okhttp = "4.3.1"
    val preference = "1.1.0"
    val retrofit = "2.7.1"
    val robolectric = "4.3.1"
    val room = "2.2.3"
    val rx_android = "2.1.1"
    val rxjava2 = "2.2.17"
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

    // For AndroidMobileAPI
    val androidx_junit = "androidx.test.ext:junit:${Versions.atsl_junit}"
    val androidx_preference_ktx = "androidx.preference:preference-ktx:${Versions.preference}"
    val mockito = "org.mockito:mockito-core:${Versions.mockito}"
    val okhttp = "com.squareup.okhttp3:okhttp:${Versions.okhttp}"
    val okhttp_logging_interceptor = "com.squareup.okhttp3:logging-interceptor:${Versions.okhttp}"
    val okhttp_mockwebserver = "com.squareup.okhttp3:mockwebserver:${Versions.okhttp}"
    val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
    val retrofit_adapter_rxjava2 = "com.squareup.retrofit2:adapter-rxjava2:${Versions.retrofit}"
    val retrofit_converter_gson = "com.squareup.retrofit2:converter-gson:${Versions.retrofit}"
    val robolectric = "org.robolectric:robolectric:${Versions.robolectric}"
    val rxandroid = "io.reactivex.rxjava2:rxandroid:${Versions.rx_android}"
    val rxjava = "io.reactivex.rxjava2:rxjava:${Versions.rxjava2}"

    // For AndroidMobileDataStore
    val androidx_room = "androidx.room:room-ktx:${Versions.room}"
    val androidx_room_runtime = "androidx.room:room-runtime:${Versions.room}"
    val androidx_room_compiler = "androidx.room:room-compiler:${Versions.room}"
}