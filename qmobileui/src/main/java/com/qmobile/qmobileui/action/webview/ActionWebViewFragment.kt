/*
 * Created by qmarciset on 10/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.webview

import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.databinding.FragmentActionWebviewBinding
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.webview.WebViewHelper

const val HEADER_CONTEXT_KEY = "X-QMobile-Context"

class ActionWebViewFragment : BaseFragment() {

    private var _binding: FragmentActionWebviewBinding? = null
    val binding get() = _binding!!
    var path: String = ""
    var actionName = ""
    var actionLabel = ""
    var actionShortLabel = ""
    var base64EncodedContext = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("path")?.let { path = it }
        arguments?.getString("actionName")?.let { actionName = it }
        arguments?.getString("actionLabel")?.let { actionLabel = it }
        arguments?.getString("actionShortLabel")?.let {
            actionShortLabel = it
        }
        arguments?.getString("base64EncodedContext")?.let {
            base64EncodedContext = it
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActionWebviewBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWebView()
    }

    private fun setupWebView() {
        delegate.checkNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                binding.webView.settings.javaScriptEnabled = true
                binding.webView.addJavascriptInterface(AndroidJavaScriptHandler(requireActivity()), "Android")
                binding.webView.settings.javaScriptEnabled = true
                val url = BaseApp.sharedPreferencesHolder.remoteUrl + path

                binding.webView.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                    }

                    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
                        return WebClientHelper.getResponseWithHeader(
                            url.toString(),
                            HEADER_CONTEXT_KEY,
                            base64EncodedContext
                        )
                    }
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return false
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        binding.progressCircular.visibility = View.INVISIBLE
                        WebClientHelper.injectScriptFile(view, actionName, actionLabel, actionShortLabel)
                    }
                }

                WebViewHelper.loadUrl(binding.webView, url)
            }

            override fun onServerInaccessible() {
                showErrorServer()
            }

            override fun onNoInternet() {
                showErrorServer()
            }
        })
    }

    fun showErrorServer() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage(R.string.server_not_accessible)
            .setPositiveButton(
                R.string.retry_action
            ) { _, _ ->
                setupWebView()
            }
            .setNegativeButton(
                R.string.remote_url_dialog_cancel
            ) { _, _ ->
                requireActivity().onBackPressed()
            }
        builder.create()
        builder.show()
    }
}