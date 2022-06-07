/*
 * Created by qmarciset on 7/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.utils

import android.net.Uri

@Suppress("UNCHECKED_CAST")
object UriHelper {

    fun HashMap<String, Uri>.uriToString(): HashMap<String, String> = this.mapValues { entry ->
        entry.value.path
    } as HashMap<String, String>

    fun HashMap<String, String>.stringToUri(): HashMap<String, Uri> = this.mapValues { entry ->
        Uri.parse(entry.value)
    } as HashMap<String, Uri>
}
