/*
 * Created by Quentin Marciset on 19/6/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context

/**
 * Utility class for actions related to files
 */
object FileUtils {

    const val EMBEDDED_PICTURES_PARENT = "Assets.xcassets"
    const val EMBEDDED_PICTURES = "Pictures"
    const val JSON_EXT = ".json"

    /**
     * Reads content from assets json files
     */
    fun readContentFromFilePath(context: Context, filename: String): String {
        return context.assets.open(filename).bufferedReader().use {
            it.readText()
        }
    }

    /**
     * Returns the list of asset files found at [path]
     */
    fun listAssetFiles(path: String, context: Context): List<String> {
        val result = ArrayList<String>()
        context.assets.list(path)?.forEach { file ->
            val innerFiles = listAssetFiles("$path/$file", context)
            if (innerFiles.isNotEmpty()) {
                result.addAll(innerFiles)
            } else {
                result.add("$path/$file")
            }
        }
        return result
    }
}
