/*
 * Created by qmarciset on 30/11/2021.
 * 4D SAS
 * Copyright (c) 2021 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.webview

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class MyWebViewClient : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return false
        /*if (Uri.parse(url).host == "www.google.com") {
            // This is my web site, so do not override; let my WebView load the page
            Toast.makeText(view!!.context, Uri.parse(url).host, Toast.LENGTH_SHORT).show()
            return false
        }
        // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            startActivity(view!!.context, this, null)
            Toast.makeText(view!!.context, "start activity", Toast.LENGTH_SHORT).show()
        }
        return true*/
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
    }
}
