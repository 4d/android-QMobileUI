/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters

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
import androidx.core.view.MenuProvider
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.gcacace.signaturepad.views.SignaturePad
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.UploadHelper
import com.qmobile.qmobileapi.utils.UploadHelper.getBodies
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatastore.dao.ActionInfo
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobiledatasync.viewmodel.EntityViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityViewModel
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionProvider
import com.qmobile.qmobileui.action.actionparameters.viewholders.BaseViewHolder
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.utils.ActionHelper
import com.qmobile.qmobileui.action.utils.UriHelper.uriToString
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.binding.writeBitmap
import com.qmobile.qmobileui.databinding.FragmentActionParametersBinding
import com.qmobile.qmobileui.ui.CenterLayoutManager
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import com.qmobile.qmobileui.ui.setupToolbarTitle
import com.qmobile.qmobileui.utils.hideKeyboard
import okhttp3.RequestBody
import org.json.JSONArray
import timber.log.Timber
import java.io.File
import java.util.Date
import kotlin.collections.HashMap

class ActionParametersFragment : BaseFragment(), ActionProvider, MenuProvider {

    // views
    private lateinit var adapter: ActionsParametersListAdapter
    private var _binding: FragmentActionParametersBinding? = null
    private val binding get() = _binding!!
    private lateinit var signatureDialog: View

    // fragment parameters
    override var tableName = ""
    private var itemId = ""
    private var actionUUID = ""
    private var parentItemId = ""
    private var relation: Relation? = null

    private var shouldInitObservers = true

    override lateinit var actionActivity: ActionActivity

    internal var errorsByParameter = HashMap<String, String>()
    internal var paramsToSubmit = HashMap<String, Any>()
    private val metaDataToSubmit = HashMap<String, String>()
    internal var validationMap = linkedMapOf<String, Boolean>()
    internal var imagesToUpload = HashMap<String, Uri>()

    private var scrollPos = 0
    private lateinit var currentPhotoPath: String
    internal lateinit var action: Action
    internal var selectedEntity: RoomEntity? = null
    private var actionPosition = -1

    internal var currentTask: ActionTask? = null
    internal var taskId: String? = null
    private var fromPendingTasks = false
    internal var allParameters = JSONArray()

    internal var entityViewModel: EntityViewModel<EntityModel>? = null

    companion object {
        const val BARCODE_FRAGMENT_REQUEST_KEY = "scan_request"
        const val BARCODE_VALUE_INJECT_KEY = "barcode_inject"
        const val BARCODE_VALUE_KEY = "barcode_value"
        const val IMAGE_URI_INJECT_KEY = "image_uri_inject"
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
        if (success) {
            onImageChosen(Uri.fromFile(File(currentPhotoPath)))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("navbarTitle")?.let { navbarTitle = it }
        arguments?.getString("tableName")?.let { tableName = it }
        arguments?.getString("itemId")?.let { itemId = it }
        arguments?.getString("actionUUID")?.let { actionUUID = it }
        arguments?.getString("taskId")?.let {
            if (it.isNotEmpty()) {
                taskId = it
                fromPendingTasks = true
            }
        }
        arguments?.getString("relationName")?.let { relationName ->
            if (relationName.isNotEmpty()) {
                relation = RelationHelper.getRelation(tableName, relationName)
                tableName = relation?.dest ?: tableName
                arguments?.getString("parentItemId")?.let { parentItemId = it }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initMenuProvider()
        activity?.setupToolbarTitle(navbarTitle)

        setFragmentResultListener(BARCODE_FRAGMENT_REQUEST_KEY) { _, bundle ->
            bundle.getString(BARCODE_VALUE_KEY)?.let {
                adapter.updateBarcodeForPosition(actionPosition, it)
            }
        }

        _binding = FragmentActionParametersBinding.inflate(
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

        action = retrieveAction()

        setupAdapter()
        setupRecyclerView()

        if (!fromPendingTasks) {
            entityViewModel = getEntityViewModel(this, tableName, itemId, delegate.apiService)
            allParameters = action.parameters
        }

        if (shouldInitObservers) {
            ActionParametersFragmentObserver(this).initObservers()
            shouldInitObservers = false
        }
    }

    private fun setupRecyclerView() {
        val smoothScroller = CenterLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.parametersRecyclerView.layoutManager = smoothScroller
        binding.parametersRecyclerView.adapter = adapter
        // Important line : prevent recycled views to get their content reset
        binding.parametersRecyclerView.setItemViewCacheSize(action.parameters.length())
        // Add this empty view to remove keyboard when click is perform below recyclerView items
        binding.emptyView.setOnSingleClickListener {
            onHideKeyboardCallback()
        }
    }

    internal fun setupAdapter() {
        adapter = ActionsParametersListAdapter(
            context = requireContext(),
            list = allParameters,
            paramsToSubmit = paramsToSubmit,
            imagesToUpload = imagesToUpload,
            paramsError = errorsByParameter,
            currentEntity = selectedEntity,
            fragmentManager = activity?.supportFragmentManager,
            hideKeyboardCallback = { onHideKeyboardCallback() },
            focusNextCallback = { position, onlyScroll -> onFocusNextCallback(position, onlyScroll) },
            actionTypesCallback = { actionType, position ->
                actionTypesCallback(actionType, position)
            },
            onValueChanged = { name: String, value: Any?, metaData: String?, isValid: Boolean ->
                onValueChanged(name, value, metaData, isValid)
            }
        )
        binding.parametersRecyclerView.adapter = adapter
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
            binding.root.clearFocus()
            hideKeyboard(it)
        }
    }

    private fun onFocusNextCallback(position: Int, onlyScroll: Boolean) {
        scrollTo(position + 1, false)
        if (!onlyScroll) {
            binding.parametersRecyclerView.findViewHolderForLayoutPosition(position + 1)
                ?.itemView?.findViewById<TextInputEditText>(R.id.input)
                ?.requestFocus()
                ?: kotlin.run {
                    Timber.d("Can't find input to focus at position ${position + 1}, scrolling now")
                }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_actions_parameters, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.validate) {
            onValidateClick()
        }
        return false
    }

    override fun onPrepareMenu(menu: Menu) {
        val item = menu.findItem(R.id.validate)
        if (fromPendingTasks) {
            currentTask?.isErrorServer()?.let { isErrorServer ->
                item.title = if (isErrorServer) { // error server tasks
                    getString(R.string.retry_action)
                } else { // pending tasks
                    getString(R.string.validate_action)
                }
            }
        } else {
            item.title = getString(R.string.validate_action)
        }
        super.onPrepareMenu(menu)
    }

    private fun onValidateClick() {
        if (fromPendingTasks && currentTask?.status == ActionTask.Status.PENDING) {
            // When coming from pending task
            validatePendingTask()
        } else {
            // When coming from actions or click on error server failed task (in history section)
            if (currentTask?.status == ActionTask.Status.ERROR_SERVER) {
                // should delete current failed task to re-store it as new task after sending
                currentTask?.let {
                    actionActivity.getTaskViewModel().deleteOne(it.id)
                }
            }
            validateAction()
        }
    }

    private fun getActionInfo(): ActionInfo = ActionInfo(
        paramsToSubmit = paramsToSubmit,
        metaDataToSubmit = metaDataToSubmit,
        imagesToUpload = imagesToUpload.uriToString(),
        validationMap = validationMap,
        allParameters = action.parameters.toString(),
        actionName = action.name,
        tableName = tableName,
        actionUUID = action.uuid,
        isOfflineCompatible = action.isOfflineCompatible(),
        preferredShortName = action.getPreferredShortName()
    )

    private fun validatePendingTask() {
        if (isFormValid()) {
            currentTask?.let { task ->
                val actionTask = ActionTask(
                    status = ActionTask.Status.PENDING,
                    date = task.date,
                    relatedItemId = task.relatedItemId,
                    label = task.label,
                    actionInfo = getActionInfo()
                )
                actionTask.id = task.id

                actionActivity.getTaskViewModel().insert(actionTask)
            }
            activity?.onBackPressed()
        }
    }

    private fun validateAction() {
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ActionActivity) {
            actionActivity = context
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
                (binding.parametersRecyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            if (pos < nbItems - 1) {
                SnackbarHelper.show(activity, activity?.getString(R.string.scroll_to_not_seen))
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
        binding.parametersRecyclerView.removeOnScrollListener(onScrollListener)
        binding.parametersRecyclerView.addOnScrollListener(onScrollListener)
        binding.parametersRecyclerView.smoothScrollToPosition(position)
    }

    private fun triggerError(position: Int) {
        (binding.parametersRecyclerView.findViewHolderForLayoutPosition(position) as BaseViewHolder?)?.validate(true)
    }

    private fun retrieveAction(): Action {
        ActionHelper.getActionObjectList(BaseApp.runtimeDataHolder.tableActions, tableName)
            .plus(ActionHelper.getActionObjectList(BaseApp.runtimeDataHolder.currentRecordActions, tableName))
            .forEach { action ->
                // create id with pattern: $actionName$tableName
                val actionId = action.getSafeString("name") + tableName
                if (actionUUID == actionId) {
                    return ActionHelper.createActionFromJsonObject(action)
                }
            }
        throw Action.ActionException("Couldn't find action from table [$tableName], with uuid [$actionUUID]")
    }

    private fun createPendingTask(): ActionTask {
        return ActionTask(
            status = ActionTask.Status.PENDING,
            date = Date(),
            relatedItemId = (selectedEntity?.__entity as EntityModel?)?.__KEY,
            label = action.getPreferredName(),
            actionInfo = getActionInfo()
        )
    }

    private fun sendAction() {
        val pendingTask = createPendingTask()
        actionActivity.sendAction(
            actionContent = getActionContent(pendingTask.id, (selectedEntity?.__entity as EntityModel?)?.__KEY),
            actionTask = pendingTask,
            tableName = tableName
        ) {
            activity?.onBackPressed()
        }
    }

    private fun uploadImages(proceed: () -> Unit) {
        val bodies: Map<String, RequestBody?> = imagesToUpload.getBodies(activity)

        actionActivity.uploadImage(
            bodies = bodies,
            tableName = tableName,
            isFromAction = true,
            taskToSendIfOffline = if (action.isOfflineCompatible()) createPendingTask() else null,
            onImageUploaded = { parameterName, receivedId ->
                paramsToSubmit[parameterName] = receivedId
                metaDataToSubmit[parameterName] = UploadHelper.UPLOADED_METADATA_STRING
            }
        ) {
            proceed()
        }
    }

    override fun getActionContent(actionUUID: String, itemId: String?): MutableMap<String, Any> {
        return ActionHelper.getActionContent(
            tableName = tableName,
            actionUUID = actionUUID,
            itemId = itemId ?: this.itemId,
            parameters = paramsToSubmit,
            metaData = metaDataToSubmit,
            parentItemId = parentItemId,
            relation = relation
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
        activity?.apply {
            signatureDialog = LayoutInflater.from(this)
                .inflate(R.layout.action_parameter_signature_dialog, findViewById(android.R.id.content), false)

            MaterialAlertDialogBuilder(this)
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
        activity?.let {
            ImageHelper.getTempImageFile(it, Bitmap.CompressFormat.PNG) { _, signatureFilePath ->
                File(signatureFilePath).apply {
                    val bitmap = try {
                        signatureDialog.findViewById<SignaturePad>(R.id.signature_pad)?.transparentSignatureBitmap
                    } catch (e: IllegalArgumentException) {
                        Timber.d("Could not get the signature bitmap (${e.message})")
                        null
                    }
                    bitmap?.let {
                        this.writeBitmap(bitmap)
                        onImageChosen(Uri.fromFile(this))
                    }
                }
            }
        }
    }

    private fun pickPhotoFromGallery() {
        getImageFromGallery.launch("image/*")
    }

    private fun takePhotoFromCamera() {
        activity?.let {
            ImageHelper.getTempImageFile(it, Bitmap.CompressFormat.JPEG) { uri, photoFilePath ->
                currentPhotoPath = photoFilePath
                getCameraImage.launch(uri)
            }
        }
    }

    private fun scan() {
        BaseApp.genericNavigationResolver.navigateToActionScanner(binding, actionPosition)
    }
}
