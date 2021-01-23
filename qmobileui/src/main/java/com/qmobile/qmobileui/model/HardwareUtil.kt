/*
 * Created by Quentin Marciset on 23/1/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.model

import android.os.Build
import timber.log.Timber

internal  object HardwareUtil { // scope to internal module

     val isEmulator: Boolean = Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.PRODUCT.contains("google_sdk") ||
            Build.PRODUCT.contains("vbox86p") ||
            Build.PRODUCT.contains("emulator") ||
            Build.PRODUCT.contains("simulator") ||
            Build.PRODUCT.contains("sdk_x86") ||
            Build.PRODUCT.contains("sdk_google") ||
            Build.BOARD == "QC_Reference_Phone" || // bluestacks
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.HOST.startsWith("Build") || // MSI App Player
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))

     fun versionName(): String {
        val fields = Build.VERSION_CODES::class.java.fields
        var codeName = "UNKNOWN"
        fields.filter {
            var buildVersion = -1
            try {
                buildVersion = it.getInt(Build.VERSION_CODES::class)
            } catch (e: IllegalArgumentException) {
                Timber.d("Couldn't get Build.VERSION_CODES")
            }
            buildVersion == Build.VERSION.SDK_INT
        }
            .forEach { codeName = it.name }
        return "Android $codeName"
    }
}