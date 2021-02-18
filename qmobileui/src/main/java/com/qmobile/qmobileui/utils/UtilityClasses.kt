/*
 * Created by Quentin Marciset on 22/1/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import android.os.Build
import com.qmobile.qmobileapi.auth.AuthInfoHelper
import com.qmobile.qmobileui.model.AppUtilities
import com.qmobile.qmobileui.model.DeviceUtility
import com.qmobile.qmobileui.model.HardwareUtil
import com.qmobile.qmobileui.model.QMobileUiConstants
import org.json.JSONObject
import java.util.Locale
import kotlin.collections.ArrayList
// Kotlin File To Hold Utility classes
internal open class FileUtilsUp(var context: Context) { // scope restricted to this module
    var readContentFromFile = { fileName: String ->
        context.assets.open(fileName).bufferedReader().use {
            it.readText()
        }
    }

    fun listAssetFiles(path: String): List<String> {
        val result = ArrayList<String>()
        context.assets.list(path)?.forEach { file ->
            val innerFiles = this.listAssetFiles("$path/$file")
            if (innerFiles.isNotEmpty()) {
                result.addAll(innerFiles)
            } else {
                result.add("$path/$file")
            }
        }
        return result
    }
}

internal class BridgeUtility(context: Context) : FileUtilsUp(context) { // scope restricted to this module
    fun getAppUtil(): AppUtilities {
        val jsonObj = JSONObject(readContentFromFile("appinfo.json"))
        return AppUtilities(
            (jsonObj.getString("initialGlobalStamp")).toInt(),
            (jsonObj.getString("guestLogin")).toBoolean(),
            (jsonObj.getString("embeddedData")).toBoolean(),
            jsonObj.getString("remoteUrl"),
            JSONObject().apply {
                val newTeam = jsonObj.getJSONObject("team")
                this.put("id", newTeam.getString("TeamName"))
                this.put("name", newTeam.getString("TeamID"))
            },
            JSONObject(readContentFromFile("queries.json")),
            jsonObj.getJSONObject("searchableField")
        )
    }
}

internal class DeviceUtilitiesGenerator(context: Context) {
    private val locale = Locale.getDefault()
    val getDeviceUtility =
        DeviceUtility(
            language = JSONObject().apply {
                put(QMobileUiConstants.Language.LANGUAGE_ID, locale.toString())
                put(QMobileUiConstants.Language.LANGUAGE_CODE, locale.language)
                put(QMobileUiConstants.Language.LANGUAGE_REGION, locale.country)
            },
            deviceInfo = JSONObject().apply {
                put(QMobileUiConstants.Device.ID, AuthInfoHelper.getInstance(context).deviceUUID)
                put(QMobileUiConstants.Device.SIMULATOR, HardwareUtil.isEmulator) // false
                put(QMobileUiConstants.Device.DESCRIPTION, Build.MODEL) // SM-G950F
                put(QMobileUiConstants.Device.VERSION, Build.VERSION.SDK_INT) // 28
                put(QMobileUiConstants.Device.OS, HardwareUtil.versionName()) // Android P
            }
        )
}