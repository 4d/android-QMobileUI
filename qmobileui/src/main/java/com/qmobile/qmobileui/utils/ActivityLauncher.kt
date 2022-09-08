/*
 * Created by qmarciset on 8/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.fragment.app.FragmentActivity

interface ActivityLauncher {

    val activityLauncherImpl: ActivityLauncherImpl

    /**
     * This method is accessible for Custom formatters and Input Controls
     */
    fun launchContactPhoneNumber(callback: (contactUri: Uri?) -> Unit) {
        activityLauncherImpl.launchContactPhoneNumber(callback)
    }
}

class ActivityLauncherImpl(private val context: Context) {

    private var contactPhoneNumberCallback: (contactUri: Uri?) -> Unit = {}

    private val getContactPhoneNumber: ActivityResultLauncher<Void?> = registerContactPhoneNumber()

    private fun registerContactPhoneNumber(): ActivityResultLauncher<Void?> {
        return (context as FragmentActivity).registerForActivityResult(ActivityResultContracts.PickContact()) { contactUri ->
            contactPhoneNumberCallback(contactUri)
        }
    }

    fun launchContactPhoneNumber(callback: (contactUri: Uri?) -> Unit) {
        contactPhoneNumberCallback = callback
        getContactPhoneNumber.launch()
    }
}
