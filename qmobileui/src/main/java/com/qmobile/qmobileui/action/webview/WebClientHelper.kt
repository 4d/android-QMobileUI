/*
 * Created by htemanni on 2/9/2022.
 * 4D SAS
 * Copyright (c) 2022 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui.action.webview

import android.util.Base64
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.qmobile.qmobileapi.network.ApiClient.REQUEST_TIMEOUT
import com.qmobile.qmobiledatasync.app.BaseApp
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
object WebClientHelper {
    fun injectScriptFile(view: WebView, actionName: String, actionLabel: String, actionShortLabel: String) {
        val input: InputStream
        try {
            input = getScriptForAction(actionName, actionLabel, actionShortLabel).byteInputStream()
            val buffer = ByteArray(input.available())
            input.read(buffer)
            input.close()
            val encoded = Base64.encodeToString(buffer, Base64.NO_WRAP)
            view.loadUrl(
                "javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var script = document.createElement('script');" +
                    "script.type = 'text/javascript';" +
                    "script.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(script)" +
                    "})()"
            )
        } catch (e: IOException) {
            Timber.e(e.message.orEmpty())
        }
    }

    private fun getScriptForAction(actionName: String, actionLabel: String, actionShortLabel: String): String {
        return """
         'use strict';
           var ${'$'}4d = {mobile: { 'dismiss': function () {
                                                                console.log('click');
                                                                Android.onDismiss()
                                                            },
                                                         
                                                            'status': function (message) {
                                                                Android.status(JSON.stringify(message))
                                                            },
                                                            
                                                             action: {
                                                                    name: '$actionName',
                                                                    label: '$actionLabel',
                                                                    shortLabel: '$actionShortLabel' 
                                                                    }, 
                                                                       logger: {
                                                                                log: function (level, message) {
                                                                                Android.log(level, message);
                                                                                },
                                                                                info: function (message) {
                                                                                    this.log('info', message);
                                                                                },
                                                                                debug: function (message) {
                                                                                    this.log('debug', message);
                                                                                },
                                                                                warning: function (message) {
                                                                                    this.log('warning', message);
                                                                                },
                                                                                error: function (message) {
                                                                                    this.log('error', message);
                                                                                }
                                                                    }     
       }
      }  """
    }

    fun getResponseWithHeader(url: String, headerName: String, headerValue: String, onError: () -> Unit): WebResourceResponse? {
        try {
            val request: Request = Request.Builder()
                .url(url.trim { it <= ' ' })
                // Add cookie as header
                .addHeader("Cookie", BaseApp.sharedPreferencesHolder.cookies)
                .addHeader(headerName, headerValue)
                .build()
            val response: Response = HttpClient.instance.newCall(request).execute()
            return WebResourceResponse(
                null,
                response.header("content-encoding", "utf-8"),
                response.body.byteStream())
        } catch (exception : SocketTimeoutException){
            Timber.e("WebClientHelper: ${exception.localizedMessage}")
            onError()
            return null
        }

    }

    // Singleton http client
    object HttpClient {
        private const val timeOut = REQUEST_TIMEOUT.toLong()
        val instance =
            OkHttpClient().newBuilder()
                .connectTimeout(timeOut, TimeUnit.SECONDS) // connect timeout
                .writeTimeout(timeOut, TimeUnit.SECONDS) // write timeout
                .readTimeout(timeOut, TimeUnit.SECONDS) // read timeout
                .build()
    }
}
