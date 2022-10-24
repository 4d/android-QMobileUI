/*
 * Created by qmarciset on 12/10/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.transition.TransitionManager
import com.google.android.material.transition.MaterialFade
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.qmobile.qmobileui.R

fun Fragment.setSharedAxisXEnterTransition() {
    this.setSharedAxisEnterTransition(MaterialSharedAxis.X)
}

fun Fragment.setSharedAxisZEnterTransition() {
    this.setSharedAxisEnterTransition(MaterialSharedAxis.Z)
}

fun Fragment.setSharedAxisXExitTransition() {
    this.setSharedAxisExitTransition(MaterialSharedAxis.X)
}

fun Fragment.setSharedAxisZExitTransition() {
    this.setSharedAxisExitTransition(MaterialSharedAxis.Z)
}

private fun Fragment.setSharedAxisEnterTransition(@MaterialSharedAxis.Axis axis: Int) {
    enterTransition = MaterialSharedAxis(axis, true).apply {
        duration = resources.getInteger(R.integer.motion_duration_large).toLong()
    }
    returnTransition = MaterialSharedAxis(axis, false).apply {
        duration = resources.getInteger(R.integer.motion_duration_large).toLong()
    }
}

private fun Fragment.setSharedAxisExitTransition(@MaterialSharedAxis.Axis axis: Int) {
    exitTransition = MaterialSharedAxis(axis, true).apply {
        duration = resources.getInteger(R.integer.motion_duration_large).toLong()
    }
    reenterTransition = MaterialSharedAxis(axis, false).apply {
        duration = resources.getInteger(R.integer.motion_duration_large).toLong()
    }
}

fun Fragment.setFadeThroughEnterTransition() {
    enterTransition = MaterialFadeThrough().apply {
        duration = resources.getInteger(R.integer.motion_duration_large).toLong()
    }
    returnTransition = MaterialFadeThrough().apply {
        duration = resources.getInteger(R.integer.motion_duration_large).toLong()
    }
}

fun Fragment.setFadeThroughExitTransition() {
    exitTransition = MaterialFadeThrough().apply {
        duration = resources.getInteger(R.integer.motion_duration_large).toLong()
    }
    reenterTransition = MaterialFadeThrough().apply {
        duration = resources.getInteger(R.integer.motion_duration_large).toLong()
    }
}

fun setMaterialFadeTransition(viewGroup: ViewGroup, slowLoading: Boolean = false) {
    val materialFade = MaterialFade().apply {
        if (slowLoading) {
            duration = viewGroup.resources.getInteger(R.integer.motion_duration_large).toLong()
        }
    }
    TransitionManager.beginDelayedTransition(viewGroup, materialFade)
}
