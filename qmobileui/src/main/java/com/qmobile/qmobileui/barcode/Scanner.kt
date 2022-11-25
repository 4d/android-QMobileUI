/*
 * Created by qmarciset on 24/11/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.barcode

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@ExperimentalGetImage
class Scanner {

    private lateinit var cameraExecutor: ExecutorService
    private val alreadyScanned = AtomicBoolean(false)

    companion object {
        private const val PROGRESS_DELAY: Long = 1000
    }

    fun start(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        barcodeOverlay: BarcodeOverlay?,
        progressIndicator: LinearProgressIndicator?,
        progressDelay: Long = PROGRESS_DELAY,
        onScanned: (value: String) -> Unit
    ) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        bindCameraUseCases(
            context,
            lifecycleOwner,
            previewView,
            barcodeOverlay,
            progressIndicator,
            progressDelay,
            onScanned
        )
    }

    private fun bindCameraUseCases(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        barcodeOverlay: BarcodeOverlay?,
        progressIndicator: LinearProgressIndicator?,
        progressDelay: Long = PROGRESS_DELAY,
        onScanned: (value: String) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
            val scanner = BarcodeScanning.getClient(options)

            val analysisUseCase = ImageAnalysis.Builder().build()
            analysisUseCase.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageProxy(scanner, imageProxy, barcodeOverlay, progressIndicator, progressDelay, onScanned)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, previewUseCase, analysisUseCase)
            } catch (illegalStateException: IllegalStateException) {
                Timber.e(illegalStateException.message.orEmpty())
            } catch (illegalArgumentException: IllegalArgumentException) {
                Timber.e(illegalArgumentException.message.orEmpty())
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy,
        barcodeOverlay: BarcodeOverlay?,
        progressIndicator: LinearProgressIndicator?,
        progressDelay: Long = PROGRESS_DELAY,
        onScanned: (value: String) -> Unit
    ) {
        imageProxy.image?.let { image ->
            val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodeList ->

                    if (!alreadyScanned.getAndSet(true)) {
                        barcodeList.getOrNull(0)?.rawValue?.let { value ->
                            progressIndicator?.visibility = View.VISIBLE

                            Handler(Looper.getMainLooper()).postDelayed({
                                progressIndicator?.visibility = View.GONE

                                onScanned(value)
                            }, progressDelay)
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
                                barcodeOverlay?.update(it, result)
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

    fun reset() {
        alreadyScanned.set(false)
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}
