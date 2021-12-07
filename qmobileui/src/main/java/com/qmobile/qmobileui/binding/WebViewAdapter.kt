/*
 * Created by qmarciset on 3/12/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.binding

import android.webkit.WebView

object WebViewAdapter {

    fun loadUrl(view: WebView, url: String?) {
        if (url.isNullOrEmpty()) return
        view.loadUrl(url)
    }
}
