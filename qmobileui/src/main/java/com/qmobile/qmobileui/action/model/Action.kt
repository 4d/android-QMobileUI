/*
 * Created by htemanni on 13/9/2021.
 * 4D SAS
 * Copyright (c) 2021 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui.action.model

import com.qmobile.qmobileui.utils.ResourcesHelper
import org.json.JSONArray
import java.util.Locale

class Action(
    val name: String = "",
    val shortLabel: String? = null,
    val label: String? = null,
    val icon: String? = null,
    val preset: String? = null,
    val scope: Scope? = null,
    val parameters: JSONArray,
    val id: String
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

    class ActionException(message: String) : Exception(message)
}
