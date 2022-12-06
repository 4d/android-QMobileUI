/*
 * Created by qmarciset on 10/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.barcode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.camera.core.ExperimentalGetImage
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.action.actionparameters.ActionParametersFragment.Companion.BARCODE_FRAGMENT_REQUEST_KEY
import com.qmobile.qmobileui.action.actionparameters.ActionParametersFragment.Companion.BARCODE_VALUE_KEY
import com.qmobile.qmobileui.activity.mainactivity.MainActivity
import com.qmobile.qmobileui.barcode.Scanner
import com.qmobile.qmobileui.databinding.FragmentBarcodeBinding
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import com.qmobile.qmobileui.ui.setSharedAxisZEnterTransition

@ExperimentalGetImage
class BarcodeScannerFragment : BaseFragment() {

    private var _binding: FragmentBarcodeBinding? = null
    private val binding get() = _binding!!

    private val scanner = Scanner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSharedAxisZEnterTransition()
        activity?.actionBar?.hide()

        activity?.onBackPressedDispatcher?.addCallback {
            delegate.setFullScreenMode(false)
            (activity as? MainActivity?)?.navController?.navigateUp()
            this.isEnabled = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBarcodeBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topActionBarInLiveCamera.closeButton.setOnSingleClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        delegate.setFullScreenMode(true)
        scanner.start(
            context = requireContext(),
            lifecycleOwner = this,
            previewView = binding.cameraView,
            barcodeOverlay = binding.barcodeOverlay,
            progressIndicator = binding.progress,
            onScanned = { value -> onScanned(value) }
        )
    }

    override fun onDestroy() {
        scanner.shutdown()
        super.onDestroy()
    }

    private fun onScanned(value: String) {
        if (isAdded) {
            val result = Bundle().apply {
                putString(BARCODE_VALUE_KEY, value)
            }
            parentFragmentManager.setFragmentResult(BARCODE_FRAGMENT_REQUEST_KEY, result)
        }
        activity?.onBackPressedDispatcher?.onBackPressed()
    }
}
