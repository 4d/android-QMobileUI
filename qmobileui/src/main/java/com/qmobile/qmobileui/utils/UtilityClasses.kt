/*
 * Created by Quentin Marciset on 22/1/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import com.qmobile.qmobileui.model.AppUtilities
import org.json.JSONObject

internal open class FileUtilsUp(var context: Context){ // scope restricted to this module
    var readContentFromFile = {fileName: String -> context.assets.open(fileName).bufferedReader().use {
        it.readText()
    }}

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

internal class BridgeUtility(context: Context) : FileUtilsUp(context) {  // scope restricted to this module
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
            JSONObject(readContentFromFile("queries.json"))
        )
    }
}