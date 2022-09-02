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
import org.json.JSONObject

const val TAG = "WebView info"

class AndroidJavaScriptHandler(var activity: FragmentActivity) {
    @JavascriptInterface
    fun onDismiss() {
        activity.onBackPressed()
    }

    @JavascriptInterface
    fun log(level: String, message: String) {
        when (level) {
            "info" -> Log.i(TAG, message)
            "debug" -> Log.d(TAG, message)
            "warning" -> Log.w(TAG, message)
            "error" -> Log.e(TAG, message)
        }
    }

    @JavascriptInterface
    fun status(message: String) {
        val jsonObject = JSONObject(message)
        // case of message is {message: String, succes: Boolean}
        jsonObject.getSafeString("message")?.let { text ->
            val success = jsonObject.getSafeBoolean("success")
            if (success != null) {
                val type = when (success) {
                    true -> ToastMessage.Type.SUCCESS
                    false -> ToastMessage.Type.ERROR
                }
                SnackbarHelper.show(activity, text, type)
            } else {
                // case of message is {message: String}
                SnackbarHelper.show(activity, text, ToastMessage.Type.NEUTRAL)
            }
            return
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
