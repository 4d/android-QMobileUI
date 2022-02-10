/*
 * Created by qmarciset on 10/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.barcode

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.action.ActionParametersFragment.Companion.BARCODE_FRAGMENT_REQUEST_KEY
import com.qmobile.qmobileui.databinding.FragmentBarcodeBinding
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@ExperimentalGetImage
class BarcodeScannerFragment : Fragment(), BaseFragment {

    private var _binding: FragmentBarcodeBinding? = null
    val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    override lateinit var delegate: FragmentCommunication
    private val alreadyScanned = AtomicBoolean(false)

    companion object {
        const val PROGRESS_DELAY: Long = 5000
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        cameraExecutor = Executors.newSingleThreadExecutor()

        _binding = FragmentBarcodeBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

        binding.topActionBarInLiveCamera.closeButton.setOnClickListener {
            delegate.setFullScreenMode(false)
            activity?.onBackPressed()
        }
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        delegate.setFullScreenMode(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindCameraUseCases()
    }

    private fun bindCameraUseCases() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraView.surfaceProvider)
                }

            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
            val scanner = BarcodeScanning.getClient(options)

            val analysisUseCase = ImageAnalysis.Builder().build()
            analysisUseCase.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageProxy(scanner, imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.bindToLifecycle(this, cameraSelector, previewUseCase, analysisUseCase)
            } catch (illegalStateException: IllegalStateException) {
                Timber.e(illegalStateException.localizedMessage)
            } catch (illegalArgumentException: IllegalArgumentException) {
                Timber.e(illegalArgumentException.localizedMessage)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processImageProxy(barcodeScanner: BarcodeScanner, imageProxy: ImageProxy) {
        imageProxy.image?.let { image ->
            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodeList ->

                    if (!alreadyScanned.getAndSet(true)) {
                        barcodeList.getOrNull(0)?.rawValue?.let { value ->
                            binding.progress.visibility = View.VISIBLE

                            Handler(Looper.getMainLooper()).postDelayed({
                                binding.progress.visibility = View.GONE

                                val result = Bundle().apply {
                                    putString("barcode_value", value)
                                }
                                if (isAdded)
                                    parentFragmentManager.setFragmentResult(BARCODE_FRAGMENT_REQUEST_KEY, result)
                                activity?.onBackPressed()
                            }, PROGRESS_DELAY)
                        } ?: run {
                            alreadyScanned.set(false)
                        }
                    }
                }
                .addOnFailureListener {
                    Timber.e(it.localizedMessage)
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result?.let { result ->
                            inputImage.mediaImage?.let {
                                binding.barcodeOverlay.update(it, result)
                            }
                        }
                    } else {
                        Timber.e("failed to scan image: ${task.exception?.message}")
                    }
                    imageProxy.image?.close()
                    imageProxy.close()
                }
        }
    }
}
