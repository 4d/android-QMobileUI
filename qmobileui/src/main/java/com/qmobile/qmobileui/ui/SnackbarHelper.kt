/*
 * Created by qmarciset on 20/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.getColorFromAttr

object SnackbarHelper {

    const val UNDO_ACTION_DURATION = 5000

    fun show(
        activity: FragmentActivity?,
        message: String?,
        type: ToastMessage.Type = ToastMessage.Type.NEUTRAL,
        duration: Int? = null,
        behavior: BaseTransientBottomBar.Behavior? = null
    ) {
        if (!message.isNullOrEmpty()) {
            build(activity, message, type, duration, behavior)?.showSnackbar()
        }
    }

    fun showAction(
        activity: FragmentActivity?,
        message: String?,
        actionText: String,
        onActionClick: () -> Unit,
        type: ToastMessage.Type = ToastMessage.Type.NEUTRAL,
        duration: Int? = null,
        behavior: BaseTransientBottomBar.Behavior? = null
    ) {
        if (!message.isNullOrEmpty()) {
            build(activity, message, type, duration, behavior)?.apply {
                setAction(actionText) {
                    onActionClick()
                }
                show()
            }
        }
    }

    fun build(
        activity: FragmentActivity?,
        message: String,
        type: ToastMessage.Type = ToastMessage.Type.NEUTRAL,
        duration: Int? = null,
        behavior: BaseTransientBottomBar.Behavior? = null
    ): Snackbar? {
        activity?.apply {
            val snackbar = Snackbar.make(findViewById(R.id.main_container), message, Snackbar.LENGTH_LONG)
            if (!noTabLayoutUI) {
                findViewById<TabLayout>(R.id.scrollable_tab_layout)?.let { tabLayout ->
                    snackbar.anchorView = tabLayout
                }
            }
            snackbar.setColors(this, type)
            duration?.let {
                snackbar.setDuration(duration)
            }
            behavior?.let {
                snackbar.behavior = it
            }
            return snackbar
        }
        return null
    }

    private fun Snackbar.showSnackbar() {
        this.show()
    }

    private fun Snackbar.setColors(context: Context, type: ToastMessage.Type) {
        when (type) {
            ToastMessage.Type.SUCCESS -> {
                setBackgroundTint(ContextCompat.getColor(context, R.color.snackbar_success))
                setTextColor(ContextCompat.getColor(context, R.color.on_snackbar_success))
            }
            ToastMessage.Type.WARNING -> {
                setBackgroundTint(ContextCompat.getColor(context, R.color.snackbar_warning))
                setTextColor(ContextCompat.getColor(context, R.color.on_snackbar_warning))
            }
            ToastMessage.Type.ERROR -> {
                setBackgroundTint(context.getColorFromAttr(R.attr.colorError))
                setTextColor(context.getColorFromAttr(R.attr.colorOnError))
            }
            else -> {}
        }
    }
}
