/*
 * Created by htemanni on 13/9/2021.
 * 4D SAS
 * Copyright (c) 2021 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui.action.model

import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.utils.ResourcesHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

open class Action(
    val name: String = "",
    val shortLabel: String? = null,
    val label: String? = null,
    val icon: String? = null,
    val preset: String? = null,
    val scope: Scope? = null,
    val parameters: JSONArray,
    val uuid: String
) {

    fun getIconDrawablePath(): String? =
        ResourcesHelper.correctIconPath(icon)

    fun getPreferredName(): String {
        return if (!label.isNullOrEmpty())
            label
        else if (!shortLabel.isNullOrEmpty())
            shortLabel
        else
            name
    }

    fun getPreferredShortName(): String {
        return if (!shortLabel.isNullOrEmpty())
            shortLabel
        else if (!label.isNullOrEmpty())
            label
        else
            name
    }

    fun isOfflineCompatible() = preset?.lowercase(Locale.getDefault()) != "share"

    enum class Type {
        TAKE_PICTURE_CAMERA, PICK_PHOTO_GALLERY, SCAN, SIGN
    }

    enum class Scope {
        TABLE, CURRENT_RECORD
    }

    fun getSortFields(): HashMap<String, String> {

        val fieldsToSortBy: Map<String, String>
        fieldsToSortBy = HashMap()
        for (i in 0 until parameters.length()) {
            var format = (parameters.get(i) as JSONObject).getSafeString("format")
            format = when (format) {
                "ascending" -> "ASC"
                "descending" -> "DESC"
                else -> {
                    ""
                }
            }
            val attribute =
                (parameters.get(i) as JSONObject).getSafeString("name")?.lowercase()
                    ?.filter { !it.isWhitespace() }

            if ((!format.isNullOrEmpty()) && (!attribute.isNullOrEmpty())) {
                fieldsToSortBy[attribute] = format
            }
        }
        return fieldsToSortBy
    }

    class ActionException(message: String) : Exception(message)
}
