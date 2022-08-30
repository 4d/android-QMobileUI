/*
 * Created by qmarciset on 10/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.webview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import androidx.camera.core.ExperimentalGetImage
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.databinding.FragmentActionWebviewBinding

@ExperimentalGetImage
class ActionWebViewFragment : BaseFragment() {

    private var _binding: FragmentActionWebviewBinding? = null
    val binding get() = _binding!!
    internal var path: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("path")?.let { path = it }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActionWebviewBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // WebViewClient allows you to handle
        // onPageFinished and override Url loading.
        binding.webView.webViewClient = WebViewClient()

        // this will load the url of the website
        val url = BaseApp.sharedPreferencesHolder.remoteUrl + path
        binding.webView.loadUrl(url)

        // this will enable the javascript settings
        binding.webView.settings.javaScriptEnabled = true

        // if you want to enable zoom feature
        binding.webView.settings.setSupportZoom(true)
    }
}
