/*
 * Created by Quentin Marciset on 19/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.utils

import android.content.Context

/**
 * Utility class for actions related to files
 */
object FileUtils {

    /**
     * Reads content from assets json files
     */
    fun readContentFromFilePath(context: Context, filename: String): String {
        return context.assets.open(filename).bufferedReader().use {
            it.readText()
        }
    }
}
