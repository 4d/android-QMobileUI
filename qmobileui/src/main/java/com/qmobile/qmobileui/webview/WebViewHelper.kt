package com.qmobile.qmobileui.webview

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.view.children
import com.qmobile.qmobiledatasync.app.BaseApp

object WebViewHelper {

    private const val DEFAULT_INITIAL_SCALE = 110

    fun loadUrl(view: WebView, url: String?, extraHeaders: Map<String, String>? = null) {
        if (url.isNullOrEmpty()) return
        BaseApp.sharedPreferencesHolder.injectCookies(url)
        if (extraHeaders != null) {
            view.loadUrl(url, extraHeaders)
        } else {
            view.loadUrl(url)
        }
    }

    fun WebView.adjustSize() = apply {
        setInitialScale(DEFAULT_INITIAL_SCALE)
        settings.useWideViewPort = false
        settings.loadWithOverviewMode = true
    }

    fun View.checkIfChildIsWebView(): WebView? = when (this) {
        is WebView -> this
        is ViewGroup -> this.checkIfContainsWebView()
        else -> null
    }

    @Suppress("ReturnCount")
    fun ViewGroup.checkIfContainsWebView(): WebView? {
        var childContainsWebView: WebView? = null
        this.children.forEach { child ->
            if (child is WebView) return child
            if (child is ViewGroup) {
                childContainsWebView = child.checkIfContainsWebView()
                if (childContainsWebView != null) {
                    return childContainsWebView
                }
            }
        }
        return childContainsWebView
    }
}
