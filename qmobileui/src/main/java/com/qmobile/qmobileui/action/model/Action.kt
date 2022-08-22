/*
 * Created by htemanni on 13/9/2021.
 * 4D SAS
 * Copyright (c) 2021 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui.action.model

import com.qmobile.qmobileapi.utils.getJSONObjectList
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.action.sort.SortFormat
import com.qmobile.qmobileui.utils.ResourcesHelper
import org.json.JSONArray
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

    fun getPreferredName(): String = when {
        !label.isNullOrEmpty() -> label
        !shortLabel.isNullOrEmpty() -> shortLabel
        else -> name
    }

    fun getPreferredShortName(): String = when {
        !shortLabel.isNullOrEmpty() -> shortLabel
        !label.isNullOrEmpty() -> label
        else -> name
    }

    fun isOfflineCompatible() = preset?.lowercase(Locale.getDefault()) != "share"

    enum class Type {
        TAKE_PICTURE_CAMERA, PICK_PHOTO_GALLERY, SCAN, SIGN
    }

    enum class Scope {
        TABLE, CURRENT_RECORD
    }

    fun getSortFields(): LinkedHashMap<String, String> {
        val fieldsToSortBy: LinkedHashMap<String, String> = LinkedHashMap()
        parameters.getJSONObjectList().forEach { parameter ->
            val format = when (parameter.getSafeString("format")) {
                "ascending" -> SortFormat.ASCENDING.value
                "descending" -> SortFormat.DESCENDING.value
                else -> ""
            }

            val type = parameter.getSafeString("type")
            parameter.getSafeString("name")?.lowercase()?.filter { !it.isWhitespace() }
                ?.let { name ->

                    // if the field is a time we have to convert it from string to int, otherwise the AM/PM sort will not work
                    // if type is string we make the sort case insensitive
                    val key = when (type) {
                        "type" -> "CAST ($name AS INT)"
                        "string" -> "$name COLLATE NOCASE "
                        else -> name
                    }

                    if (format.isNotEmpty()) {
                        fieldsToSortBy[key] = format
                    }
                }
        }
        return fieldsToSortBy
    }

    fun isSortAction() = preset == "sort"

    class ActionException(message: String) : Exception(message)
}
