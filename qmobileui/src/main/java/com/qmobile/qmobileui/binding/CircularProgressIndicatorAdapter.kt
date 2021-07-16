/*
 * Created by qmarciset on 16/7/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import androidx.databinding.BindingAdapter
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlin.math.roundToInt

@BindingAdapter("progress")
fun bindCircularProgressIndicator(view: CircularProgressIndicator, progress: Any?) {
    view.progress = when (progress) {
        is Int -> progress
        is Float -> progress.roundToInt()
        else -> 0
    }
}
