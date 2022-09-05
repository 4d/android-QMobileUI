/*
 * Created by htemanni on 2/9/2022.
 * 4D SAS
 * Copyright (c) 2022 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui.action.webview

import android.util.Log
import android.webkit.JavascriptInterface
import androidx.fragment.app.FragmentActivity
import com.qmobile.qmobileapi.utils.getSafeBoolean
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatasync.toast.ToastMessage
import com.qmobile.qmobileui.ui.SnackbarHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

const val TAG = "WebView info"

class AndroidJavaScriptHandler(var activity: FragmentActivity) {
    @JavascriptInterface
    fun onDismiss() {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            activity.onBackPressed()
        }
    }

    @JavascriptInterface
    fun log(level: String, message: String) {
        when (level) {
            "info" -> Timber.i(message)
            "debug" -> Timber.d(message)
            "warning" -> Timber.w(message)
            "error" -> Timber.e(message)
        }
    }

    @JavascriptInterface
    fun status(message: String) {
        val jsonObject = JSONObject(message)
        // case of message is {message: String, success: Boolean}
        jsonObject.getSafeString("message")?.let { text ->
            return when (jsonObject.getSafeBoolean("success")) {
                null -> {
                    // case of message is {message: String}
                    SnackbarHelper.show(activity, text, ToastMessage.Type.NEUTRAL)
                }
                true -> {
                    SnackbarHelper.show(activity, text, ToastMessage.Type.SUCCESS)

                }
                false -> {
                    SnackbarHelper.show(activity, text, ToastMessage.Type.ERROR)
                }
            }

        }

        jsonObject.getSafeString("statusText")?.let { text ->
            val level = jsonObject.getSafeString("level")
            level?.let {
                // case of message is {statusText: String, level: string}
                val type = when (it) {
                    "debug" -> ToastMessage.Type.NEUTRAL
                    "info" -> ToastMessage.Type.NEUTRAL
                    "warning" -> ToastMessage.Type.WARNING
                    "error" -> ToastMessage.Type.ERROR
                    else -> ToastMessage.Type.NEUTRAL
                }
                SnackbarHelper.show(activity, text, type)
                return
            }
        }
    }
}
