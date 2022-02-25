/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.action

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.gcacace.signaturepad.views.SignaturePad
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.APP_OCTET
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.viewholders.BaseViewHolder
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.binding.writeBitmap
import com.qmobile.qmobileui.databinding.FragmentActionParametersBinding
import com.qmobile.qmobileui.ui.CenterLayoutManager
import com.qmobile.qmobileui.utils.hideKeyboard
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import java.lang.IllegalArgumentException

class ActionParametersFragment : BaseFragment(), ActionProvider {

    // views
    private lateinit var adapter: ActionsParametersListAdapter
    private var _binding: FragmentActionParametersBinding? = null
    private val binding get() = _binding!!
    private lateinit var signatureDialog: View

    // fragment parameters
    override var tableName = ""
    private var itemId = ""
    private var inverseName = ""
    private var parentItemId = ""
    private var parentRelationName = ""
    private var parentTableName = ""
    private var fromRelation = false

    override lateinit var actionActivity: ActionActivity

    private val paramsToSubmit = HashMap<String, Any>()
    private val metaDataToSubmit = HashMap<String, String>()
    private val validationMap = mutableMapOf<String, Boolean>()
    private val imagesToUpload = HashMap<String, Uri>()
    private var scrollPos = 0
    private lateinit var currentPhotoPath: String
    private lateinit var action: Action
    private var selectedEntity: EntityModel? = null
    private var actionPosition = -1

    companion object {
        const val BARCODE_FRAGMENT_REQUEST_KEY = "scan_request"
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                triggerError(scrollPos)
                recyclerView.removeOnScrollListener(this)
            }
        }
    }

    private val getImageFromGallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            onImageChosen(uri)
        }
    }

    private val getCameraImage = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success)
            onImageChosen(Uri.fromFile(File(currentPhotoPath)))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        setHasOptionsMenu(true)
        arguments?.getString("tableName")?.let { tableName = it }
        arguments?.getString("itemId")?.let { itemId = it }
        arguments?.getString("destinationTable")?.let {
            if (it.isNotEmpty()) {
                tableName = it
                fromRelation = true
            }
        }
        arguments?.getString("parentItemId")?.let { parentItemId = it }
        arguments?.getString("inverseName")?.let { inverseName = it }

        if (fromRelation) {
            RelationHelper.getRelation(tableName, inverseName)?.dest?.let { parentTableName = it }
            RelationHelper.getRelation(tableName, inverseName)?.dest?.let { parentRelationName = it }
        }

        setFragmentResultListener(BARCODE_FRAGMENT_REQUEST_KEY) { _, bundle ->
            bundle.getString("barcode_value")?.let {
                adapter.updateBarcodeForPosition(actionPosition, it)
            }
        }

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
                focusNextCallback = { position -> onFocusNextCallback(position) },
                actionTypesCallback = { actionType, position ->
                    actionTypesCallback(actionType, position)
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
            // Add this empty view to remove keyboard when click is perform below recyclerView items
            emptyView.setOnClickListener {
                onHideKeyboardCallback()
            }
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

    private fun onFocusNextCallback(position: Int) {
        binding.recyclerView.findViewHolderForLayoutPosition(position + 1)
            ?.itemView?.findViewById<TextInputEditText>(R.id.input)
            ?.requestFocus()
            ?: kotlin.run {
                Timber.d("Can't find input to focus at position ${position + 1}, scrolling now")
                scrollTo(position + 1, false)
            }
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
    }

    @Suppress("ReturnCount")
    private fun isFormValid(): Boolean {

        // first: check if visible items are valid
        val firstNotValidItemPosition = validationMap.values.indexOfFirst { !it }
        if (firstNotValidItemPosition > -1) {
            scrollTo(firstNotValidItemPosition, true)
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
                scrollTo(pos + 1, true)
            }
            return false
        }

        // third: check any not valid item
        var formIsValid = true
        validationMap.values.forEachIndexed { index, isValid ->
            if (!isValid) {
                if (formIsValid) { // scroll to first not valid
                    scrollTo(index, true)
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

    private fun scrollTo(position: Int, shouldHideKeyboard: Boolean) {
        if (shouldHideKeyboard) onHideKeyboardCallback()
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

        actionActivity.sendAction(
            actionName = action.name,
            actionContent = getActionContent(selectedEntity?.__KEY),
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

    override fun getActionContent(itemId: String?): MutableMap<String, Any> {
        return ActionHelper.getActionContent(
            tableName = tableName,
            itemId = itemId ?: "",
            parameters = paramsToSubmit,
            metaData = metaDataToSubmit,
            relationName = inverseName,
            parentItemId = parentItemId,
            parentTableName = parentTableName,
            parentRelationName = parentRelationName
        )
    }

    private fun onImageChosen(uri: Uri) {
        action.parameters.getSafeObject(actionPosition)?.getSafeString("name")?.let { parameterName ->
            imagesToUpload[parameterName] = uri
        }
        adapter.updateImageForPosition(actionPosition, uri)
    }

    private fun actionTypesCallback(actionType: Action.Type, position: Int) {
        actionPosition = position
        when (actionType) {
            Action.Type.PICK_PHOTO_GALLERY -> pickPhotoFromGallery()
            Action.Type.TAKE_PICTURE_CAMERA -> takePhotoFromCamera()
            Action.Type.SCAN -> scan()
            Action.Type.SIGN -> showSignDialog()
        }
    }

    private fun showSignDialog() {
        requireActivity().apply {
            signatureDialog = LayoutInflater.from(this)
                .inflate(R.layout.action_parameter_signature_dialog, findViewById(android.R.id.content), false)

            MaterialAlertDialogBuilder(this, R.style.TitleThemeOverlay_MaterialComponents_MaterialAlertDialog)
                .setView(signatureDialog)
                .setTitle(getString(R.string.signature_dialog_title))
                .setPositiveButton(getString(R.string.signature_dialog_positive)) { _, _ ->
                    onSigned()
                }
                .setNegativeButton(getString(R.string.signature_dialog_cancel), null)
                .show()
        }
    }

    private fun onSigned() {
        ImageHelper.getTempImageFile(requireContext(), Bitmap.CompressFormat.PNG) { _, signatureFilePath ->
            File(signatureFilePath).apply {
                val bitmap = try {
                    signatureDialog.findViewById<SignaturePad>(R.id.signature_pad)?.transparentSignatureBitmap
                } catch (e: IllegalArgumentException) {
                    Timber.d("Could not get the signature bitmap (${e.message})")
                    null
                }
                bitmap?.let {
                    this.writeBitmap(it)
                    onImageChosen(Uri.fromFile(this))
                }
            }
        }
    }

    private fun pickPhotoFromGallery() {
        getImageFromGallery.launch("image/*")
    }

    private fun takePhotoFromCamera() {
        ImageHelper.getTempImageFile(requireContext(), Bitmap.CompressFormat.JPEG) { uri, photoFilePath ->
            currentPhotoPath = photoFilePath
            getCameraImage.launch(uri)
        }
    }

    private fun scan() {
        delegate.setFullScreenMode(true)
        BaseApp.genericNavigationResolver.navigateToActionScanner(binding)
    }
}
