/*
 * Created by qmarciset on 8/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.fragment.app.FragmentActivity

interface ActivityResultController {

    val activityResultControllerImpl: ActivityResultControllerImpl

    /**
     * This method is accessible for Input Controls
     */
    fun launch(type: ActivityResultContract<*, *>, input: Any?, callback: Any?) {
        activityResultControllerImpl.launch(type, input, callback)
    }
}

class ActivityResultControllerImpl(fragmentActivity: FragmentActivity) {

    private var pickContactCallback: (contactUri: Uri?) -> Unit = {}

    private var getContentCallback: (contentUri: Uri?) -> Unit = {}

    private var captureVideoCallback: (success: Boolean) -> Unit = {}

    private var takePictureCallback: (success: Boolean) -> Unit = {}

    private var takePicturePreviewCallback: (bitmap: Bitmap?) -> Unit = {}

    private var requestPermissionCallback: (granted: Boolean) -> Unit = {}

    private val pickContact =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.PickContact()) { contactUri: Uri? ->
            pickContactCallback(contactUri)
        }

    private val getContent =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.GetContent()) { contentUri: Uri? ->
            getContentCallback(contentUri)
        }

    private val captureVideo =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success: Boolean ->
            captureVideoCallback(success)
        }

    private val takePicture =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            takePictureCallback(success)
        }

    private val takePicturePreview =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            takePicturePreviewCallback(bitmap)
        }

    private val requestPermission =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            requestPermissionCallback(granted)
        }

    @Suppress("UNCHECKED_CAST")
    fun launch(type: ActivityResultContract<*, *>, input: Any?, callback: Any?) {
        when (type) {
            is ActivityResultContracts.PickContact -> { // ActivityResultContract<Void?, Uri?>
                pickContactCallback = callback as (Uri?) -> Unit
                pickContact.launch()
            }
            is ActivityResultContracts.GetContent -> { // ActivityResultContract<String, Uri?>
                getContentCallback = callback as (Uri?) -> Unit
                if (input is String) {
                    getContent.launch(input)
                }
            }
            is ActivityResultContracts.CaptureVideo -> { // ActivityResultContract<Uri, Boolean>
                captureVideoCallback = callback as (Boolean) -> Unit
                if (input is Uri) {
                    captureVideo.launch(input)
                }
            }
            is ActivityResultContracts.TakePicture -> { // ActivityResultContract<Uri, Boolean>
                takePictureCallback = callback as (Boolean) -> Unit
                if (input is Uri) {
                    takePicture.launch(input)
                }
            }
            is ActivityResultContracts.TakePicturePreview -> { // ActivityResultContract<Void?, Bitmap?>
                takePicturePreviewCallback = callback as (Bitmap?) -> Unit
                takePicturePreview.launch()
            }
            is ActivityResultContracts.RequestPermission -> { // ActivityResultContract<String, Boolean>
                requestPermissionCallback = callback as (Boolean) -> Unit
                if (input is String) {
                    requestPermission.launch(input)
                }
            }
        }
    }
}
