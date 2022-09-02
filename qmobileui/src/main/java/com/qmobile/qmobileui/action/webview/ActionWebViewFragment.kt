/*
 * Created by qmarciset on 10/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.webview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.camera.core.ExperimentalGetImage
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.databinding.FragmentActionWebviewBinding

@ExperimentalGetImage
class ActionWebViewFragment : BaseFragment() {

    private var _binding: FragmentActionWebviewBinding? = null
    val binding get() = _binding!!
    var path: String = ""
    var actionName = ""
    var actionLabel = ""
    var actionShortLabel = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("path")?.let { path = it }
        arguments?.getString("actionName")?.let { actionName = it }
        arguments?.getString("actionLabel")?.let { actionLabel = it }
        arguments?.getString("actionShortLabel")?.let {
            actionShortLabel = it
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
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.addJavascriptInterface(AndroidJavaScriptHandler(requireActivity()), "Android")
        binding.webView.settings.javaScriptEnabled = true;

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                WebClientHelper.injectScriptFile(view, actionName, actionLabel, actionShortLabel)
            }
        }

        val url = BaseApp.sharedPreferencesHolder.remoteUrl + path
        binding.webView.loadUrl(url)
        this@ActionWebViewFragment
    }
}


