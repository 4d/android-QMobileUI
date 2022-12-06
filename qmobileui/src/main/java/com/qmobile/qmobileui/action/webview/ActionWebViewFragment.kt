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
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.webview.JavaScriptUtils.injectScriptFile
import com.qmobile.qmobileui.activity.mainactivity.MainActivity
import com.qmobile.qmobileui.databinding.FragmentActionWebviewBinding
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.ui.setSharedAxisZEnterTransition
import com.qmobile.qmobileui.webview.WebViewHelper

class ActionWebViewFragment : BaseFragment() {

    private var _binding: FragmentActionWebviewBinding? = null
    private val binding get() = _binding!!
    private var path: String = ""
    private var actionName = ""
    private var actionLabel = ""
    private var actionShortLabel = ""
    private var base64EncodedContext = ""

    companion object {
        private const val HEADER_CONTEXT_KEY = "X-QMobile-Context"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("path")?.let { path = it }
        arguments?.getString("actionName")?.let { actionName = it }
        arguments?.getString("actionLabel")?.let { actionLabel = it }
        arguments?.getString("actionShortLabel")?.let {
            actionShortLabel = it
        }
        arguments?.getString("base64EncodedContext")?.let { base64EncodedContext = it }

        setSharedAxisZEnterTransition()

        activity?.onBackPressedDispatcher?.addCallback {
            delegate.setFullScreenMode(false)
            (activity as? MainActivity?)?.navController?.navigateUp()
            this.isEnabled = false
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
        delegate.setFullScreenMode(true)
        setupWebView()
    }

    override fun onDetach() {
        super.onDetach()
        delegate.setFullScreenMode(false)
    }

    private fun setupWebView() {
        binding.progressCircular.visibility = View.VISIBLE
        delegate.checkNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                binding.webView.settings.javaScriptEnabled = true
                binding.webView.settings.domStorageEnabled = true

                val javaScriptHandler = AndroidJavaScriptHandler(activity)
                binding.webView.addJavascriptInterface(javaScriptHandler, "Android")
                binding.webView.settings.builtInZoomControls = true

                binding.webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                        binding.progressCircular.visibility = View.INVISIBLE
                        view.injectScriptFile(actionName, actionLabel, actionShortLabel)
                    }
                }

                val url = BaseApp.sharedPreferencesHolder.remoteUrl + path
                val extraHeaders = mapOf(HEADER_CONTEXT_KEY to base64EncodedContext)
                WebViewHelper.loadUrl(binding.webView, url, extraHeaders)
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
        binding.webView.stopLoading()
        // Checking context as we may be gone from fragment
        context?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(requireActivity().getString(R.string.server_not_reachable))
                .setCancelable(false)
                .setPositiveButton(
                    requireActivity().getString(R.string.open_url_retry_dialog)
                ) { _, _ ->
                    setupWebView()
                }
                .setNegativeButton(
                    requireActivity().getString(R.string.open_url_dialog_cancel)
                ) { _, _ ->
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
                .show()
        }
    }
}
