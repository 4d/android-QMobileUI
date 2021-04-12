/*
 * Created by Quentin Marciset on 11/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.glide

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.qmobile.qmobileui.utils.QMobileUiUtil
import java.io.InputStream

@GlideModule
class AppGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val level = QMobileUiUtil.appUtilities.logLevel
        when {
            level <= Log.VERBOSE -> builder.setLogLevel(Log.DEBUG)
            level <= Log.DEBUG -> builder.setLogLevel(Log.WARN)
            else -> builder.setLogLevel(Log.ERROR)
        }
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.append(
            String::class.java,
            InputStream::class.java,
            HeaderLoaderFactory(context.applicationContext)
        )
    }
}
