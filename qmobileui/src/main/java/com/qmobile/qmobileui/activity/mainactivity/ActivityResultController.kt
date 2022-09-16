/*
 * Created by qmarciset on 8/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.activity.mainactivity

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.fragment.app.FragmentActivity

interface ActivityResultController {

    val activityResultControllerImpl: ActivityResultControllerImpl

    /**
     * This method is accessible for Kotlin Input Controls
     */
    fun launch(type: ActivityResultContract<*, *>, input: Any?, callback: Any?) {
        activityResultControllerImpl.launch(type, input, callback)
    }
}

@Suppress("MaxLineLength")
class ActivityResultControllerImpl(fragmentActivity: FragmentActivity) {

    private var startActivityForResultCallback: (activityResult: ActivityResult) -> Unit = {}

    private var startIntentSenderForResultCallback: (activityResult: ActivityResult) -> Unit = {}

    private var requestMultiplePermissionsCallback: (result: Map<String, Boolean>) -> Unit = {}

    private var requestPermissionCallback: (granted: Boolean) -> Unit = {}

    private var takePicturePreviewCallback: (bitmap: Bitmap?) -> Unit = {}

    private var takePictureCallback: (success: Boolean) -> Unit = {}

    private var captureVideoCallback: (success: Boolean) -> Unit = {}

    private var pickContactCallback: (contactUri: Uri?) -> Unit = {}

    private var getContentCallback: (contentUri: Uri?) -> Unit = {}

    private var getMultipleContentCallback: (uris: List<Uri>) -> Unit = {}

    private var openDocumentCallback: (uri: Uri?) -> Unit = {}

    private var openMultipleDocumentsCallback: (uris: List<Uri>) -> Unit = {}

    private var openDocumentTreeCallback: (uri: Uri?) -> Unit = {}

    private val startActivityForResult =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult: ActivityResult ->
            startActivityForResultCallback(activityResult)
        }

    private val startIntentSenderForResult =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult: ActivityResult ->
            startIntentSenderForResultCallback(activityResult)
        }

    private val requestMultiplePermissions =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result: Map<String, Boolean> ->
            requestMultiplePermissionsCallback(result)
        }

    private val requestPermission =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted: Boolean ->
            requestPermissionCallback(granted)
        }

    private val takePicturePreview =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            takePicturePreviewCallback(bitmap)
        }

    private val takePicture =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            takePictureCallback(success)
        }

    private val captureVideo =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success: Boolean ->
            captureVideoCallback(success)
        }

    private val pickContact =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.PickContact()) { contactUri: Uri? ->
            pickContactCallback(contactUri)
        }

    private val getContent =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.GetContent()) { contentUri: Uri? ->
            getContentCallback(contentUri)
        }

    private val getMultipleContent =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
            getMultipleContentCallback(uris)
        }

    private val openDocument =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            openDocumentCallback(uri)
        }

    private val openMultipleDocuments =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
            openMultipleDocumentsCallback(uris)
        }

    private val openDocumentTree =
        fragmentActivity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            openDocumentTreeCallback(uri)
        }

    @Suppress("UNCHECKED_CAST", "LongMethod")
    fun launch(type: ActivityResultContract<*, *>, input: Any?, callback: Any?) {
        when (type) {
            is ActivityResultContracts.StartActivityForResult -> {
                // ActivityResultContract<Intent, ActivityResult>
                startActivityForResultCallback = callback as (ActivityResult) -> Unit
                if (input is Intent) {
                    startActivityForResult.launch(input)
                }
            }
            is ActivityResultContracts.StartIntentSenderForResult -> {
                // ActivityResultContract<IntentSenderRequest, ActivityResult>
                startIntentSenderForResultCallback = callback as (ActivityResult) -> Unit
                if (input is IntentSenderRequest) {
                    startIntentSenderForResult.launch(input)
                }
            }
            is ActivityResultContracts.RequestMultiplePermissions -> {
                // ActivityResultContract<Array<String>, Map<String, Boolean>>
                requestMultiplePermissionsCallback = callback as (Map<String, Boolean>) -> Unit
                if (input is Array<*>) {
                    requestMultiplePermissions.launch(input as Array<String>)
                }
            }
            is ActivityResultContracts.RequestPermission -> {
                // ActivityResultContract<String, Boolean>
                requestPermissionCallback = callback as (Boolean) -> Unit
                if (input is String) {
                    requestPermission.launch(input)
                }
            }
            is ActivityResultContracts.TakePicturePreview -> {
                // ActivityResultContract<Void?, Bitmap?>
                takePicturePreviewCallback = callback as (Bitmap?) -> Unit
                takePicturePreview.launch()
            }
            is ActivityResultContracts.TakePicture -> {
                // ActivityResultContract<Uri, Boolean>
                takePictureCallback = callback as (Boolean) -> Unit
                if (input is Uri) {
                    takePicture.launch(input)
                }
            }
            is ActivityResultContracts.CaptureVideo -> {
                // ActivityResultContract<Uri, Boolean>
                captureVideoCallback = callback as (Boolean) -> Unit
                if (input is Uri) {
                    captureVideo.launch(input)
                }
            }
            is ActivityResultContracts.PickContact -> {
                // ActivityResultContract<Void?, Uri?>
                pickContactCallback = callback as (Uri?) -> Unit
                pickContact.launch()
            }
            is ActivityResultContracts.GetContent -> {
                // ActivityResultContract<String, Uri?>
                getContentCallback = callback as (Uri?) -> Unit
                if (input is String) {
                    getContent.launch(input)
                }
            }
            is ActivityResultContracts.GetMultipleContents -> {
                // ActivityResultContract<String, List<Uri>>
                getMultipleContentCallback = callback as (List<Uri>) -> Unit
                if (input is String) {
                    getMultipleContent.launch(input)
                }
            }
            is ActivityResultContracts.OpenDocument -> {
                // ActivityResultContract<Array<String>, Uri?>
                openDocumentCallback = callback as (Uri?) -> Unit
                if (input is Array<*>) {
                    openDocument.launch(input as Array<String>)
                }
            }
            is ActivityResultContracts.OpenMultipleDocuments -> {
                // ActivityResultContract<Array<String>, List<Uri>>
                openMultipleDocumentsCallback = callback as (List<Uri>) -> Unit
                if (input is Array<*>) {
                    openMultipleDocuments.launch(input as Array<String>)
                }
            }
            is ActivityResultContracts.OpenDocumentTree -> {
                // ActivityResultContract<Uri?, Uri?>
                openDocumentTreeCallback = callback as (Uri?) -> Unit
                if (input is Uri?) {
                    openDocumentTree.launch(input)
                }
            }
        }
    }
}
