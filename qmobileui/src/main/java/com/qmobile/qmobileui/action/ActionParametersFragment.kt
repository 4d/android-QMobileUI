/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.action

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
import com.qmobile.qmobileui.action.viewholders.BaseViewHolder
import com.qmobile.qmobileui.databinding.FragmentActionParametersBinding
import com.qmobile.qmobileui.network.NetworkChecker
import com.qmobile.qmobileui.ui.CenterLayoutManager
import com.qmobile.qmobileui.utils.createTempImageFile
import com.qmobile.qmobileui.utils.hideKeyboard
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.IOException

const val IMAGE_QUALITY = 90

open class ActionParametersFragment : Fragment(), BaseFragment {

    private var _binding: FragmentActionParametersBinding? = null
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
    private val validationMap = mutableMapOf<String, Boolean>()
    private lateinit var adapter: ActionsParametersListAdapter
    private var fromRelation = false

    private var scrollPos = 0

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                triggerError(scrollPos)
                recyclerView.removeOnScrollListener(this)
            }
        }
    }
    private val imagesToUpload = HashMap<String, Uri>()

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
        _binding = FragmentActionParametersBinding.inflate(
            inflater,
            container,
            false
        ).apply {
            lifecycleOwner = viewLifecycleOwner
            adapter = ActionsParametersListAdapter(
                requireContext(),
                delegate.getSelectAction().parameters,
                delegate.getSelectedEntity(),
                activity?.supportFragmentManager,
                { onHideKeyboardCallback() }
            ) { name: String, value: Any?, metaData: String?, isValid: Boolean ->
                validationMap[name] = isValid
                paramsToSubmit[name] = value ?: ""
                metaData?.let {
                    metaDataToSubmit[name] = metaData
                }
            }
            val smoothScroller = CenterLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            recyclerView.layoutManager = smoothScroller
            recyclerView.adapter = adapter
            // Important line : prevent recycled views to get their content reset
            recyclerView.setItemViewCacheSize(delegate.getSelectAction().parameters.length())
        }
        return binding.root
    }

    private fun onHideKeyboardCallback() {
        activity?.let {
            hideKeyboard(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_actions_parameters, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.validate) {
            var selectedActionId: String? = null
            delegate.getSelectAction().preset?.let { preset ->
                if (preset == "edit") {
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
    }

    @Suppress("ReturnCount")
    private fun isFormValid(): Boolean {

        // first: check if visible items are valid
        val firstNotValidItemPosition = validationMap.values.indexOfFirst { !it }
        if (firstNotValidItemPosition > -1) {
            scrollTo(firstNotValidItemPosition)
            triggerError(firstNotValidItemPosition)
            return false
        }

        // second: check if there are not yet visible items
        val nbItems = adapter.itemCount
        val viewedItems = validationMap.size
        if (viewedItems < nbItems) {
            val pos =
                (binding.recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            if (pos < nbItems - 1) {
                scrollTo(pos + 1)
            }
            return false
        }

        // third: check any not valid item
        var formIsValid = true
        validationMap.values.forEachIndexed { index, isValid ->
            if (!isValid) {
                if (formIsValid) { // scroll to first not valid
                    scrollTo(index)
                }
                formIsValid = false
                triggerError(index)
            }
        }
        if (!formIsValid) {
            return false
        }

        return true
    }

    private fun scrollTo(position: Int) {
        onHideKeyboardCallback()
        scrollPos = position
        binding.recyclerView.removeOnScrollListener(onScrollListener)
        binding.recyclerView.addOnScrollListener(onScrollListener)
        binding.recyclerView.smoothScrollToPosition(position)
    }

    private fun triggerError(position: Int) {
        (binding.recyclerView.findViewHolderForLayoutPosition(position) as BaseViewHolder?)?.validate(
            true
        )
    }

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
                            }
                            activity?.onBackPressed()
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

    fun handleResult(requestCode: Int, data: Intent) {
        // the request code is te equivalent of position of item in adapter

        // case of image picked from gallery
        val uri = data.data
        if (uri != null) {
            adapter.getUpdatedImageParameterName(requestCode)?.let {
                imagesToUpload[it] = uri
            }
            adapter.updateImageForPosition(requestCode, uri)
        } else {
            // case of image token from camera
            val thumbnail = data.extras?.get("data") as Bitmap?
            val bytes = ByteArrayOutputStream()
            thumbnail?.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, bytes)
            val destination = createTempImageFile(requireContext())
            val fileOutputStream: FileOutputStream
            try {
                fileOutputStream = FileOutputStream(destination)
                fileOutputStream.write(bytes.toByteArray())
                fileOutputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            adapter.getUpdatedImageParameterName(requestCode)?.let { parameterName ->
                imagesToUpload[parameterName] = Uri.fromFile(destination)
            }
            data.extras?.get("data")?.let { adapter.updateImageForPosition(requestCode, it) }
        }
    }
}
