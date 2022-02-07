/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.action

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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.APP_OCTET
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.viewholders.BaseViewHolder
import com.qmobile.qmobileui.databinding.FragmentActionParametersBinding
import com.qmobile.qmobileui.ui.CenterLayoutManager
import com.qmobile.qmobileui.utils.hideKeyboard
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import kotlin.collections.HashMap

open class ActionParametersFragment : Fragment(), BaseFragment, ActionProvider {

    // views
    private lateinit var adapter: ActionsParametersListAdapter
    private var _binding: FragmentActionParametersBinding? = null
    val binding get() = _binding!!

    // fragment parameters
    override var tableName: String = ""
    private var inverseName: String = ""
    private var parentItemId: String = "0"
    private var parentRelationName: String = ""
    private var parentTableName: String? = null
    private var fromRelation = false

    internal lateinit var entityListViewModel: EntityListViewModel<EntityModel>
    override lateinit var actionActivity: ActionActivity
    override lateinit var delegate: FragmentCommunication

    private val paramsToSubmit = HashMap<String, Any>()
    private val metaDataToSubmit = HashMap<String, String>()
    private val validationMap = mutableMapOf<String, Boolean>()
    private val imagesToUpload = HashMap<String, Uri>()
    private var scrollPos = 0
    private lateinit var currentPhotoPath: String
    private lateinit var action: Action
    private var selectedEntity: EntityModel? = null

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                triggerError(scrollPos)
                recyclerView.removeOnScrollListener(this)
            }
        }
    }

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
                context = requireContext(),
                action = action,
                currentEntity = selectedEntity,
                fragmentManager = activity?.supportFragmentManager,
                hideKeyboardCallback = { onHideKeyboardCallback() },
                startActivityCallback = { intent, position, photoFilePath ->
                    startActivityCallback(intent, position, photoFilePath)
                },
                onValueChanged = { name: String, value: Any?, metaData: String?, isValid: Boolean ->
                    onValueChanged(name, value, metaData, isValid)
                }
            )
            val smoothScroller = CenterLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            recyclerView.layoutManager = smoothScroller
            recyclerView.adapter = adapter
            // Important line : prevent recycled views to get their content reset
            recyclerView.setItemViewCacheSize(action.parameters.length())
        }
        return binding.root
    }

    private fun onValueChanged(name: String, value: Any?, metaData: String?, isValid: Boolean) {
        validationMap[name] = isValid
        paramsToSubmit[name] = value ?: ""
        metaData?.let {
            metaDataToSubmit[name] = metaData
        }
        if (value == null) {
            imagesToUpload.remove(name)
        }
    }

    private fun onHideKeyboardCallback() {
        activity?.let {
            hideKeyboard(it)
        }
    }

    private fun startActivityCallback(intent: Intent, position: Int, photoFilePath: String?) {
        photoFilePath?.let { currentPhotoPath = it }
        activity?.startActivityForResult(intent, position)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_actions_parameters, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.validate) {
            if (isFormValid()) {
                if (imagesToUpload.isEmpty()) {
                    sendAction()
                } else {
                    uploadImages {
                        sendAction()
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ActionActivity) {
            actionActivity = context
            action = actionActivity.getSelectedAction()
            selectedEntity = actionActivity.getSelectedEntity()
        }
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

    private fun sendAction() {

        val actionId = if (action.preset == "edit") {
            selectedEntity?.__KEY
        } else {
            null
        }

        actionActivity.sendAction(
            actionName = action.name,
            actionContent = getActionContent(actionId),
            tableName = tableName
        ) {
            activity?.onBackPressed()
        }
    }

    private fun uploadImages(proceed: () -> Unit) {
        val bodies: Map<String, RequestBody?> = imagesToUpload.mapValues {
            val stream = activity?.contentResolver?.openInputStream(it.value)
            stream?.readBytes()?.toRequestBody(APP_OCTET.toMediaTypeOrNull())
        }

        actionActivity.uploadImage(
            bodies = bodies,
            tableName = tableName,
            onImageUploaded = { parameterName, receivedId ->
                paramsToSubmit[parameterName] = receivedId
                metaDataToSubmit[parameterName] = "uploaded"
            }
        ) {
            proceed()
        }
    }

    fun handleResult(requestCode: Int, data: Intent) {
        // The request code is the position of item in adapter.
        // If image is from gallery, it's stored in data.data,
        // Otherwise recover currentPhotoPath from created image from Camera app
        val uri: Uri = data.data ?: Uri.fromFile(File(currentPhotoPath))

        adapter.getUpdatedImageParameterName(requestCode)?.let { parameterName ->
            imagesToUpload[parameterName] = uri
        }
        adapter.updateImageForPosition(requestCode, uri)
    }

    override fun getActionContent(actionId: String?): MutableMap<String, Any> {
        return ActionHelper.getActionContent(
            tableName = tableName,
            selectedActionId = actionId,
            parameters = paramsToSubmit,
            metaData = metaDataToSubmit,
            relationName = inverseName,
            parentPrimaryKey = parentItemId,
            parentTableName = parentTableName,
            parentRelationName = parentRelationName
        )
    }
}
