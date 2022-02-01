package com.qmobile.qmobileui.action

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.databinding.FragmentBarCodeScannerBinding
import java.io.IOException

const val PREVIEW_SIZE_WIDTH = 1080
const val PREVIEW_SIZE_HEIGHT = 1920

class BarCodeScannerFragment : Fragment(), BaseFragment {
    private var _binding: ViewDataBinding? = null
    val binding get() = _binding!!
    private val cameraSurfaceView: SurfaceView
        get() = (binding as FragmentBarCodeScannerBinding).cameraSurfaceView

    // flag used to avoid this problem https://stackoverflow.com/questions/47121097/barcode-scanner-result-twice
    var alreadyScanned = false
    private lateinit var cameraSource: CameraSource
    private lateinit var barcodeDetector: BarcodeDetector
    private var scannedValue = ""
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
                    startScan()
                    cameraSurfaceView.visibility = View.VISIBLE
                } else {
                    Toast.makeText(requireActivity(), "Permission Denied", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)

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

    private fun startScan() {
        barcodeDetector =
            BarcodeDetector.Builder(requireActivity()).setBarcodeFormats(Barcode.ALL_FORMATS)
                .build()

        cameraSource = CameraSource.Builder(requireActivity(), barcodeDetector)
            .setRequestedPreviewSize(PREVIEW_SIZE_HEIGHT, PREVIEW_SIZE_WIDTH)
            .setAutoFocusEnabled(true) // you should add this feature
            .build()

        cameraSurfaceView.holder
            .addCallback(object : SurfaceHolder.Callback {
                @SuppressLint("MissingPermission") // Permission already handled before calling this methode
                override fun surfaceCreated(holder: SurfaceHolder) {
                    try {
                        cameraSource.start(holder)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }

                @SuppressLint("MissingPermission") // Permission already handled before calling this methode
                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                    try {
                        cameraSource.start(holder)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    cameraSource.stop()
                }
            })

        cameraSurfaceView.visibility = View.VISIBLE

        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {
            }

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                if (!alreadyScanned) {
                    val barcodes = detections.detectedItems
                    if (barcodes.size() == 1) {
                        alreadyScanned = true
                        scannedValue = barcodes.valueAt(0).rawValue

                        requireActivity().runOnUiThread {
                            cameraSource.stop()
                            barcodeDetector.release()
                            val result = Bundle().apply {
                                putString("scanned", scannedValue)
                                arguments?.getInt("position")?.let { putInt("position", it) }
                            }
                            parentFragmentManager?.setFragmentResult(
                                "scan_request",
                                result
                            )
                            activity?.supportFragmentManager?.findFragmentById(R.id.nav_host_container)
                                ?.findNavController()?.navigateUp()
                        }
                    }
                }
            }
        })
    }
}
