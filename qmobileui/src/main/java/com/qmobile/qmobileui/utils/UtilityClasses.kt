/*
 * Created by Quentin Marciset on 22/1/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import android.os.Build
import android.util.Log
import com.qmobile.qmobileapi.auth.AuthInfoHelper
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeBoolean
import com.qmobile.qmobileapi.utils.getSafeInt
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.getStringList
import com.qmobile.qmobileui.model.AppUtilities
import com.qmobile.qmobileui.model.DeviceUtility
import com.qmobile.qmobileui.model.HardwareUtil
import com.qmobile.qmobileui.model.QMobileUiConstants
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import kotlin.collections.ArrayList

// Kotlin File To Hold Utility classes
internal open class FileUtilsUp(var context: Context) { // scope restricted to this module
    var readContentFromFile = { fileName: String ->
        try {
            context.assets.open(fileName).bufferedReader().use {
                it.readText()
            }
        } catch (e: IOException) {
            Log.e("FileUtilsUp", "Missing file \"$fileName\" in assets")
            ""
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

internal class BridgeUtility(context: Context) :
    FileUtilsUp(context) { // scope restricted to this module

    companion object {
        const val DEFAULT_LOG_LEVEL = 4
    }

    fun getAppUtil(): AppUtilities {
        val appInfoJsonObj = JSONObject(readContentFromFile("app_info.json"))
        val customFormattersJsonObj = JSONObject(readContentFromFile("custom_formatters.json"))
        val searchableFieldsJsonObj = JSONObject(readContentFromFile("searchable_fields.json"))

        val sdkVersion = readContentFromFile("sdkVersion")

        return AppUtilities(
            initialGlobalStamp = appInfoJsonObj.getSafeInt("initialGlobalStamp") ?: 0,
            guestLogin = appInfoJsonObj.getSafeBoolean("guestLogin") ?: true,
            remoteUrl = appInfoJsonObj.getSafeString("remoteUrl") ?: "",
            teams = JSONObject().apply {
                val newTeam = appInfoJsonObj.getSafeObject("team")
                this.put("id", newTeam?.getSafeString("TeamName") ?: "")
                this.put("name", newTeam?.getSafeString("TeamID") ?: "")
            },
            queryJson = JSONObject(readContentFromFile("queries.json")),
            searchField = searchableFieldsJsonObj,
            sdkVersion = sdkVersion,
            logLevel = appInfoJsonObj.getSafeInt("logLevel") ?: DEFAULT_LOG_LEVEL,
            dumpedTables = appInfoJsonObj.getSafeArray("dumpedTables").getStringList().joinToString(),
            relationAvailable = appInfoJsonObj.getSafeBoolean("relations") ?: true,
            customFormatters = buildCustomFormatterBinding(customFormattersJsonObj)
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
