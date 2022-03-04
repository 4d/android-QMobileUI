/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.action

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.databinding.FragmentActionParametersBinding
import com.qmobile.qmobileui.network.NetworkChecker
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ActionParametersFragment : Fragment(), BaseFragment {

    private var _binding: ViewDataBinding? = null
    internal lateinit var entityListViewModel: EntityListViewModel<EntityModel>
    val binding get() = _binding!!
    lateinit var tableName: String
    private var inverseName: String = ""
    private var parentItemId: String = "0"
    private var parentRelationName: String = ""
    private var parentTableName: String? = null
    override lateinit var delegate: FragmentCommunication
    private val paramsToSubmit = HashMap<String, Any>()
    private val metaDataToSubmit = HashMap<String, String>()
    private val imagesToUpload = HashMap<String, Any>()
    private val validationMap = HashMap<String, Boolean>()
    lateinit var adapter: ActionsParametersListAdapter
    private var fromRelation = false

    // Is set to true if all recyclerView items are seen at lean once
    private var areAllItemsSeen = false
    private var goToCamera: (() -> Unit)? = null
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    var currentDestinationPath: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        arguments?.getString("tableName")?.let { tableName = it }
        arguments?.getString("currentItemId")?.let { parentItemId = it }
        arguments?.getBoolean("fromRelation")?.let { fromRelation = it }
        arguments?.getString("inverseName")?.let { inverseName = it }

        if (fromRelation) {
            parentTableName =
                BaseApp.genericRelationHelper.getRelatedTableName(tableName, inverseName)
            parentRelationName =
                BaseApp.genericRelationHelper.getInverseRelationName(tableName, inverseName)
        }

        entityListViewModel = getEntityListViewModel(activity, tableName, delegate.apiService)
        if (_binding == null) {
            _binding = FragmentActionParametersBinding.inflate(
                inflater,
                container,
                false
            ).apply {
                lifecycleOwner = viewLifecycleOwner
                setupRecycleView(recyclerView)
            }
        }
        return binding.root
    }

    private fun setupRecycleView(recyclerView: RecyclerView) {
        adapter = ActionsParametersListAdapter(
            requireContext(),
            delegate.getSelectAction().parameters,
            delegate.getSelectedEntity(),
            { name: String, value: Any?, metaData: String?, isValid: Boolean ->
                validationMap[name] = isValid
                paramsToSubmit[name] = value ?: ""
                metaData?.let {
                    metaDataToSubmit[name] = metaData
                }
            },
            {
                BaseApp.genericNavigationResolver.navigateToBarCodeScanner(binding, it)
            }, { intent: Intent, position: Int, destinationPath: String ->
            goToCamera = {
                currentDestinationPath = destinationPath
                (context as Activity).startActivityForResult(
                    intent,
                    // Send position as request code, so we can update image preview only for the selected item
                    position,

                )
            }
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }, { parameterName: String, uri: Uri? ->
            if (uri != null) {
                imagesToUpload[parameterName] = uri
            } else {
                // When user signed and then cleared signature pad we should remove last signature from imagesToUpload
                imagesToUpload.remove(parameterName)
            }
        }
        )
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            layoutManager.orientation
        )
        recyclerView.addItemDecoration(dividerItemDecoration)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (layoutManager.findLastVisibleItemPosition() == layoutManager.itemCount - 1) {
                    areAllItemsSeen = true
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        })
        if (layoutManager.findLastVisibleItemPosition() == layoutManager.itemCount - 1) {
            areAllItemsSeen = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener("scan_request") { _: String, bundle: Bundle ->
            val value = bundle.getString("scanned")
            val position = bundle.getInt("position")
            value?.let { adapter.updateBarcodeForPosition(position, it) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_actions_parameters, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.validate) {
            var selectedActionId: String? = null
            delegate.getSelectAction().preset?.let {
                if (it == "edit") {
                    selectedActionId = delegate.getSelectedEntity()?.__KEY
                }
            }

            if (imagesToUpload.isNotEmpty()) {
                uploadImages(delegate.getSelectAction().name, selectedActionId)
            } else {
                sendAction(delegate.getSelectAction().name, selectedActionId)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }

        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    goToCamera?.let { it() }
                } else {
                    Toast.makeText(requireActivity(), "Permission Denied", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun isFormValid(): Boolean =
        if (!areAllItemsSeen)
            false
        else
            validationMap.values.firstOrNull { !it } == null

    private fun sendAction(actionName: String, selectedActionId: String?) {
        if (isFormValid()) {
            delegate.checkNetwork(object : NetworkChecker {
                override fun onServerAccessible() {
                    entityListViewModel.sendAction(
                        actionName,
                        ActionHelper.getActionContent(
                            tableName,
                            selectedActionId,
                            paramsToSubmit,
                            metaDataToSubmit,
                            relationName = inverseName,
                            parentPrimaryKey = parentItemId,
                            parentTableName = parentTableName,
                            parentRelationName = parentRelationName
                        )
                    ) { actionResponse ->
                        actionResponse?.let {
                            actionResponse.dataSynchro?.let { dataSynchro ->
                                if (dataSynchro) {
                                    delegate.requestDataSync(tableName)
                                }
                                activity?.onBackPressed()
                            }
                        }
                    }
                }

                override fun onServerInaccessible() {
                    entityListViewModel.toastMessage.showMessage(
                        context?.getString(R.string.action_send_server_not_accessible),
                        tableName,
                        MessageType.ERROR
                    )
                }

                override fun onNoInternet() {
                    entityListViewModel.toastMessage.showMessage(
                        context?.getString(R.string.action_send_no_internet),
                        tableName,
                        MessageType.ERROR
                    )
                }
            })
        }
    }

    private fun uploadImages(actionName: String, selectedActionId: String?) {
        val bodies = imagesToUpload.mapValues {
            val fileUri = it.value as Uri
            val stream = activity?.contentResolver?.openInputStream(fileUri)
            val body = stream?.readBytes()?.let { it1 ->
                it1
                    .toRequestBody(
                        "application/octet".toMediaTypeOrNull(),
                        0, it1.size
                    )
            }
            body
        }

        delegate.checkNetwork(object : NetworkChecker {
            override fun onServerAccessible() {
                entityListViewModel.uploadImage(bodies, { parameterName, receivedId ->
                    paramsToSubmit[parameterName] = receivedId
                    metaDataToSubmit[parameterName] = "uploaded"
                }) {
                    sendAction(actionName, selectedActionId)
                }
            }

            override fun onServerInaccessible() {
                entityListViewModel.toastMessage.showMessage(
                    context?.getString(R.string.action_send_server_not_accessible),
                    tableName,
                    MessageType.ERROR
                )
            }

            override fun onNoInternet() {
                entityListViewModel.toastMessage.showMessage(
                    context?.getString(R.string.action_send_no_internet),
                    tableName,
                    MessageType.ERROR
                )
            }
        })
    }

    fun handleResult(requestCode: Int, data: Intent?) {
        // the request code is te equivalent of position of item in adapter
        // case of image picked from gallery
        val uri = data?.data
        if (uri != null) {
            adapter.getUpdatedImageParameterName(requestCode)?.let {
                imagesToUpload[it] = uri
            }
            adapter.updateImageForPosition(requestCode, uri)
        } else {
            // case of camera capture

            currentDestinationPath?.let {
                val uri = Uri.fromFile(File(it))
                adapter.getUpdatedImageParameterName(requestCode)?.let { parameterName ->
                    imagesToUpload[parameterName] = uri
                }
                adapter.updateImageForPosition(requestCode, uri)
            }
        }
    }
}
