/*
 * Created by htemanni on 13/9/2021.
 * 4D SAS
 * Copyright (c) 2021 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui


class Action(
    val name: String,
    val icon: String?,
    private val label: String?,
    private val shortLabel: String?,
    val parameters: Array<Pair<String, Any>>
) {

    fun getPreferredName(): String {
        if (!label.isNullOrEmpty())
            return label
        else if (!shortLabel.isNullOrEmpty())
            return shortLabel
        return name

    }
}