/*
 * Created by htemanni on 2/9/2022.
 * 4D SAS
 * Copyright (c) 2022 htemanni. All rights reserved.
 */

package com.qmobile.qmobileui.action.webview

import android.util.Base64
import android.webkit.WebView
import timber.log.Timber
import java.io.IOException
import java.io.InputStream

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

    fun getScriptForAction(actionName: String, actionLabel: String, actionShortLabel: String): String {
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
}
