/*
 * Created by htemanni on 13/9/2021.
 * 4D SAS
 * Copyright (c) 2021 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui.action

import com.qmobile.qmobileui.utils.ResourcesHelper
import org.json.JSONArray

class Action(
    val name: String = "",
    val shortLabel: String? = null,
    val label: String? = null,
    val scope: String? = null,
    val tableNumber: Int? = null,
    val icon: String? = null,
    val preset: String? = null,
    val style: String? = null,
    val parameters: JSONArray
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

    enum class Type {
        TAKE_PICTURE_CAMERA, PICK_PHOTO_GALLERY, SCAN, SIGN
    }
}
