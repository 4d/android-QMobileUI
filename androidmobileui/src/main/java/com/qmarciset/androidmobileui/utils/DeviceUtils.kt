/*
 * Created by Quentin Marciset on 19/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.utils

import android.content.Context
import android.os.Build
import com.qmarciset.androidmobileapi.auth.AuthInfoHelper
import org.json.JSONObject
import java.lang.IllegalArgumentException
import java.util.Locale

object DeviceUtils {

    private const val LANGUAGE_ID = "id"
    private const val LANGUAGE_CODE = "code"
    private const val LANGUAGE_REGION = "region"

    private const val DEVICE_ID = "id"
    private const val DEVICE_SIMULATOR = "simulator"
    private const val DEVICE_DESCRIPTION = "description"
    private const val DEVICE_VERSION = "version"
    private const val DEVICE_OS = "os"

    fun getLanguageInfo(): JSONObject {
        val locale = Locale.getDefault()
        return JSONObject().apply {
            put(LANGUAGE_ID, locale.toString()) // fr_FR
            put(LANGUAGE_CODE, locale.language) // fr
            put(LANGUAGE_REGION, locale.country) // FR
        }
    }

    fun getDeviceInfo(context: Context): JSONObject {
        return JSONObject().apply {
            put(DEVICE_ID, AuthInfoHelper.getInstance(context).deviceUUID)
            put(DEVICE_SIMULATOR, isEmulator) // false
            put(DEVICE_DESCRIPTION, Build.MODEL) // SM-G950F
            put(DEVICE_VERSION, Build.VERSION.SDK_INT) // 28
            put(DEVICE_OS, versionName()) // Android P
        }
    }

    /**
     * Checks if device is an emulator or a real device
     */
    private val isEmulator: Boolean = Build.FINGERPRINT.startsWith("generic") ||
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

    /**
     * Gets OS version name
     */
    private fun versionName(): String {
        val fields = Build.VERSION_CODES::class.java.fields
        var codeName = "UNKNOWN"
        fields.filter {
            var buildVersion = -1
            try {
                buildVersion = it.getInt(Build.VERSION_CODES::class)
            } catch (e: IllegalArgumentException) {
            }
            buildVersion == Build.VERSION.SDK_INT
        }
            .forEach { codeName = it.name }
        return "Android $codeName"
    }
}
