/*
 * Created by qmarciset on 8/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.mainactivity.ActivityResultController

interface PermissionChecker {

    val permissionCheckerImpl: PermissionCheckerImpl

    /**
     * This method is accessible for Custom formatters and Kotlin Input Controls
     */
    fun askPermission(
        context: Context? = null,
        permission: String,
        rationale: String,
        callback: (isGranted: Boolean) -> Unit
    ) {
        permissionCheckerImpl.askPermission(permission, rationale, callback)
    }
}

class PermissionCheckerImpl(private val fragmentActivity: FragmentActivity) {

    fun askPermission(permission: String, rationale: String, callback: (isGranted: Boolean) -> Unit) {
        fragmentActivity.apply {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(resources.getString(R.string.permission_dialog_title))
                        .setMessage(rationale)
                        .setPositiveButton(resources.getString(R.string.permission_dialog_positive)) { _, _ ->
                            requestPermission(permission, callback)
                        }
                        .setNegativeButton(resources.getString(R.string.permission_dialog_negative)) { dialog, _ ->
                            dialog.cancel()
                        }
                        .show()
                } else {
                    requestPermission(permission, callback)
                }
            } else {
                callback(true)
            }
        }
    }

    private fun requestPermission(permission: String, callback: (isGranted: Boolean) -> Unit) {
        (fragmentActivity as? ActivityResultController)?.launch(
            type = ActivityResultContracts.RequestPermission(),
            input = permission,
            callback = callback
        )
    }
}
