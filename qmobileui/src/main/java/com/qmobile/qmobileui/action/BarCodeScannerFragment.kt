package com.qmobile.qmobileui.action

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.databinding.FragmentBarCodeScannerBinding
import timber.log.Timber
import java.util.concurrent.Executors

@Suppress( "MagicNumber")
@ExperimentalGetImage
class BarCodeScannerFragment : Fragment(), BaseFragment {
    private var _binding: ViewDataBinding? = null
    val binding get() = _binding!!
    var alreadyScanned = false

    override lateinit var delegate: FragmentCommunication

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentBarCodeScannerBinding.inflate(
            inflater,
            container,
            false
        ).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        delegate.setFullScreenMode(true)

        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    bindCameraUseCases()
                } else {
                    Toast.makeText(requireActivity(), "Permission Denied", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        (binding as FragmentBarCodeScannerBinding).closeButton.setOnClickListener {
            activity?.supportFragmentManager?.findFragmentById(R.id.nav_host_container)
                ?.findNavController()?.navigateUp()
        }
    }

    override fun onDetach() {
        super.onDetach()
        delegate.setFullScreenMode(false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
    }

    private fun bindCameraUseCases() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider((binding as FragmentBarCodeScannerBinding).cameraView.surfaceProvider)
                }
            /* passing in our desired barcode formats - MLKit supports additional formats outside of the
            ones listed here, and you may not need to offer support for all of these. You should only
            specify the ones you need */
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(
                com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS
            ).build()

            val scanner = BarcodeScanning.getClient(options)
            val analysisUseCase = ImageAnalysis.Builder()
                .build()
            analysisUseCase.setAnalyzer(
                Executors.newSingleThreadExecutor(),
                { imageProxy ->
                    processImageProxy(scanner, imageProxy)
                }
            )

            // configure to use the back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    previewUseCase,
                    analysisUseCase
                )
            } catch (illegalStateException: IllegalStateException) {
                Timber.e(illegalStateException.message.orEmpty())
            } catch (illegalArgumentException: IllegalArgumentException) {
                Timber.e(illegalArgumentException.message.orEmpty())
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {

        imageProxy.image?.let { image ->
            val inputImage =
                InputImage.fromMediaImage(
                    image,
                    imageProxy.imageInfo.rotationDegrees
                )

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodeList ->

                    if (!alreadyScanned) {
                        val barcode = barcodeList.getOrNull(0)

                        // `rawValue` is the decoded value of the barcode
                        barcode?.rawValue?.let { value ->
                            alreadyScanned = true

                            (binding as FragmentBarCodeScannerBinding).progress.visibility =
                                View.VISIBLE

                            Handler(Looper.getMainLooper()).postDelayed({
                                (binding as FragmentBarCodeScannerBinding).progress.visibility =
                                    View.GONE

                                val result = Bundle().apply {
                                    putString("scanned", value)
                                    arguments?.getInt("position")
                                        ?.let { putInt("position", it) }
                                }
                                fragmentManager?.setFragmentResult(
                                    "scan_request",
                                    result
                                )
                                activity?.supportFragmentManager?.findFragmentById(R.id.nav_host_container)
                                    ?.findNavController()?.navigateUp()
                            }, 1000)
                        }
                    }
                }
                .addOnFailureListener {
                    Timber.e(it.message.orEmpty())
                }.addOnCompleteListener { task ->

                    if (task.isSuccessful) {
                        task.result?.let { result ->
                            inputImage.mediaImage?.let {
                                (binding as FragmentBarCodeScannerBinding).barcodeOverlay.update(
                                    it,
                                    result
                                )
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
