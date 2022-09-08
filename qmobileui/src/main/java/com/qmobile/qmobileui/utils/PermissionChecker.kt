/*
 * Created by qmarciset on 3/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.activity.mainactivity.BASE_PERMISSION_REQUEST_CODE

interface PermissionChecker {

    val permissionCheckerImpl: PermissionCheckerImpl

    /**
     * This method is accessible for Custom formatters and Input Controls
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

class PermissionCheckerImpl(private val context: Context) {

    private val requestPermissionMap: MutableMap<Int, (isGranted: Boolean) -> Unit> = mutableMapOf()

    fun askPermission(permission: String, rationale: String, callback: (isGranted: Boolean) -> Unit) {
        (context as FragmentActivity?)?.apply {
            val requestPermissionCode = BASE_PERMISSION_REQUEST_CODE + requestPermissionMap.size
            requestPermissionMap[requestPermissionCode] = callback

            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.permission_dialog_title))
                        .setMessage(rationale)
                        .setPositiveButton(getString(R.string.permission_dialog_positive)) { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(permission),
                                requestPermissionCode
                            )
                        }
                        .setNegativeButton(getString(R.string.permission_dialog_negative)) { dialog, _ -> dialog.cancel() }
                        .show()
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(permission), requestPermissionCode)
                }
            } else {
                callback(true)
            }
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestPermissionMap.containsKey(requestCode)) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissionMap[requestCode]?.invoke(true)
            } else {
                requestPermissionMap[requestCode]?.invoke(false)
            }
        }
    }
}
