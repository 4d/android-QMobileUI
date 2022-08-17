/*
 * Created by qmarciset on 20/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.getColorFromAttr

object SnackbarHelper {

    const val UNDO_ACTION_DURATION = 5000

    fun show(
        activity: FragmentActivity?,
        message: String?,
        type: ToastMessage.Type = ToastMessage.Type.NEUTRAL,
        duration: Int? = null
    ) {
        build(activity, message, type, duration)?.showSnackbar()
    }

    fun showAction(
        activity: FragmentActivity?,
        message: String?,
        actionText: String,
        onActionClick: () -> Unit,
        type: ToastMessage.Type = ToastMessage.Type.NEUTRAL,
        duration: Int? = null
    ) {
        build(activity, message, type, duration)?.apply {
            setAction(actionText) {
                onActionClick()
            }
            show()
        }
    }

    private fun build(
        activity: FragmentActivity?,
        message: String?,
        type: ToastMessage.Type = ToastMessage.Type.NEUTRAL,
        duration: Int?
    ): Snackbar? {
        if (!message.isNullOrEmpty()) {
            activity?.apply {
                val snackbar = Snackbar.make(findViewById(R.id.main_container), message, Snackbar.LENGTH_LONG)
                findViewById<BottomNavigationView>(R.id.bottom_nav)?.let { bottomNav ->
                    snackbar.anchorView = bottomNav
                }
                if (BaseApp.nightMode()) {
                    snackbar.setNightModeColors(this, type)
                } else {
                    snackbar.setLightModeColors(this, type)
                }
                duration?.let {
                    snackbar.setDuration(duration)
                }
                return snackbar
            }
        }
        return null
    }

    private fun Snackbar.showSnackbar() {
        this.show()
    }

    private fun Snackbar.setNightModeColors(context: Context, type: ToastMessage.Type) {
        when (type) {
            ToastMessage.Type.SUCCESS -> {
                setBackgroundTint(ContextCompat.getColor(context, R.color.theme_dark_snackbar_success))
                setTextColor(ContextCompat.getColor(context, R.color.theme_dark_on_snackbar_success))
            }
            ToastMessage.Type.WARNING -> {
                setBackgroundTint(ContextCompat.getColor(context, R.color.theme_dark_snackbar_warning))
                setTextColor(ContextCompat.getColor(context, R.color.theme_dark_on_snackbar_warning))
            }
            ToastMessage.Type.ERROR -> {
                setBackgroundTint(context.getColorFromAttr(R.attr.colorError))
                setTextColor(context.getColorFromAttr(R.attr.colorOnError))
            }
            else -> {}
        }
    }

    private fun Snackbar.setLightModeColors(context: Context, type: ToastMessage.Type) {
        when (type) {
            ToastMessage.Type.SUCCESS -> {
                setBackgroundTint(ContextCompat.getColor(context, R.color.theme_light_snackbar_success))
                setTextColor(ContextCompat.getColor(context, R.color.theme_light_on_snackbar_success))
            }
            ToastMessage.Type.WARNING -> {
                setBackgroundTint(ContextCompat.getColor(context, R.color.theme_light_snackbar_warning))
                setTextColor(ContextCompat.getColor(context, R.color.theme_light_on_snackbar_warning))
            }
            ToastMessage.Type.ERROR -> {
                setBackgroundTint(context.getColorFromAttr(R.attr.colorError))
                setTextColor(context.getColorFromAttr(R.attr.colorOnError))
            }
            else -> {}
        }
    }
}