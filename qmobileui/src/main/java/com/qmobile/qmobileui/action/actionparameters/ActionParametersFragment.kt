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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
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
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.getStringList
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
import com.qmobile.qmobileui.action.inputcontrols.InputControl.Format.saveInputControlFormatHolders
import com.qmobile.qmobileui.action.inputcontrols.InputControlFormatHolder
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.utils.ActionHelper
import com.qmobile.qmobileui.action.utils.UriHelper.uriToString
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.binding.writeBitmap
import com.qmobile.qmobileui.databinding.FragmentActionParametersBinding
import com.qmobile.qmobileui.ui.BounceEdgeEffectFactory
import com.qmobile.qmobileui.ui.CenterLayoutManager
import com.qmobile.qmobileui.ui.SnackbarHelper
import com.qmobile.qmobileui.ui.setFadeThroughEnterTransition
import com.qmobile.qmobileui.ui.setSharedAxisXEnterTransition
import com.qmobile.qmobileui.ui.setSharedAxisXExitTransition
import com.qmobile.qmobileui.ui.setSharedAxisZExitTransition
import com.qmobile.qmobileui.ui.setupToolbarTitle
import com.qmobile.qmobileui.utils.hideKeyboard
import okhttp3.RequestBody
import org.json.JSONArray
import timber.log.Timber
import java.io.File
import java.lang.Integer.max
import java.util.*

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
    internal var inputControlFormatHolders = mutableMapOf<Int, InputControlFormatHolder>()

    private var scrollPos = 0
    private lateinit var currentPhotoPath: String
    private lateinit var action: Action
    internal var selectedEntity: RoomEntity? = null
    private var parameterPosition = -1

    internal var currentTask: ActionTask? = null
    internal var taskId: String? = null
    private var fromPendingTasks = false
    internal var allParameters = JSONArray()

    internal var entityViewModel: EntityViewModel<EntityModel>? = null
    private lateinit var validateMenuItem: MenuItem

    private var allSeen = false
    private var maxSeen = 0

    companion object {
        // barcode
        const val BARCODE_FRAGMENT_REQUEST_KEY = "scan_request"
        const val BARCODE_VALUE_KEY = "barcode_value"

        // push input control
        const val INPUT_CONTROL_PUSH_FRAGMENT_REQUEST_KEY = "input_control_push_request"
        const val INPUT_CONTROL_PUSH_DISPLAY_TEXT_KEY = "input_control_push_display_text"
        const val INPUT_CONTROL_PUSH_FIELD_VALUE_KEY = "input_control_push_field_value"
        const val INPUT_CONTROL_DISPLAY_TEXT_INJECT_KEY = "input_control_display_text_inject"
        const val INPUT_CONTROL_FIELD_VALUE_INJECT_KEY = "input_control_field_value_inject"
    }

    private val lastVisibleItemPosition: Int
        get() = (binding.parametersRecyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

    private val onScrollToValidationListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                if (lastVisibleItemPosition == adapter.itemCount - 1) {
                    allSeen = true
                }
            }
        }
    }

    private val onScrollToErrorListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                for (i in 0 until lastVisibleItemPosition + 1) {
                    triggerError(i)
                }
                recyclerView.removeOnScrollListener(this)
            }
        }
    }

    private val getImageFromGallery: ActivityResultLauncher<String> = registerImageFromGallery()

    private val getCameraImage: ActivityResultLauncher<Uri> = registerCameraImage()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFadeThroughEnterTransition()

        arguments?.getString("navbarTitle")?.let { navbarTitle = it }
        arguments?.getString("tableName")?.let { tableName = it }
        arguments?.getString("itemId")?.let { itemId = it }
        arguments?.getString("actionUUID")?.let { actionUUID = it }
        arguments?.getString("taskId")?.let {
            if (it.isNotEmpty()) {
                taskId = it
                fromPendingTasks = true
                setSharedAxisXEnterTransition()
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
                action.parameters.getSafeObject(parameterPosition)?.getSafeString("name")?.let { parameterName ->
                    paramsToSubmit[parameterName] = it
                    adapter.notifyItemChanged(parameterPosition)
                }
            }
        }

        setFragmentResultListener(INPUT_CONTROL_PUSH_FRAGMENT_REQUEST_KEY) { _, bundle ->
            bundle.getString(INPUT_CONTROL_PUSH_DISPLAY_TEXT_KEY)?.let { displayText ->
                val fieldValue: String? = bundle.getString(INPUT_CONTROL_PUSH_FIELD_VALUE_KEY)
                inputControlFormatHolders[parameterPosition] = InputControlFormatHolder(displayText, fieldValue)
                adapter.notifyItemChanged(parameterPosition)
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
        onHideKeyboardCallback()
    }

    private fun setupRecyclerView() {
        val smoothScroller = CenterLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.parametersRecyclerView.layoutManager = smoothScroller
        binding.parametersRecyclerView.edgeEffectFactory = BounceEdgeEffectFactory()
        binding.parametersRecyclerView.adapter = adapter
        binding.parametersRecyclerView.addOnScrollListener(onScrollToValidationListener)
        // Important line : prevent recycled views to get their content reset
        binding.parametersRecyclerView.setItemViewCacheSize(action.parameters.length())
        // Remove keyboard when click is perform below recyclerView items
        binding.parametersRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent): Boolean {
                return when {
                    motionEvent.action != MotionEvent.ACTION_UP || recyclerView.findChildViewUnder(
                        motionEvent.x,
                        motionEvent.y
                    ) != null -> false
                    else -> {
                        onHideKeyboardCallback()
                        true
                    }
                }
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            override fun onTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent) {}
        })
    }

    internal fun setupAdapter() {
        adapter = ActionsParametersListAdapter(
            context = requireContext(),
            allParameters = allParameters,
            paramsToSubmit = paramsToSubmit,
            imagesToUpload = imagesToUpload,
            paramsError = errorsByParameter,
            inputControlFormatHolders = inputControlFormatHolders,
            currentEntity = selectedEntity,
            fragmentManager = activity?.supportFragmentManager,
            hideKeyboardCallback = { onHideKeyboardCallback() },
            focusNextCallback = { position, onlyScroll -> onFocusNextCallback(position, onlyScroll) },
            actionTypesCallback = { actionType, position ->
                actionTypesCallback(actionType, position)
            },
            goToPushFragment = { position ->
                goToPushFragment(position)
            },
            formatHolderCallback = { holder, position ->
                formatHolderCallback(holder, position)
            },
            onDataLoadedCallback = {
                onDataLoadedCallback()
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
        scrollTo(position = position + 1, shouldHideKeyboard = false, triggerError = false)
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
        validateMenuItem = menu.findItem(R.id.validate)
        if (fromPendingTasks) {
            currentTask?.isErrorServer()?.let { isErrorServer ->
                validateMenuItem.title = if (isErrorServer) { // error server tasks
                    getString(R.string.retry_action)
                } else { // pending tasks
                    getString(R.string.validate_action)
                }
            }
        } else {
            validateMenuItem.title = getString(R.string.validate_action)
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
                actionTask.actionContent = getActionContent(task.id, (selectedEntity?.__entity as? EntityModel)?.__KEY)

                actionTask.saveInputControlFormatHolders(inputControlFormatHolders)

                actionActivity.getTaskViewModel().insert(actionTask)
            }
            activity?.onBackPressed()
        }
    }

    private fun validateAction() {
        if (isFormValid()) {
            validateMenuItem.isEnabled = false
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
        val validList = LinkedList(validationMap.values)
        for (i in 0 until lastVisibleItemPosition + 1) {
            if (!validList[i]) {
                scrollTo(position = i, shouldHideKeyboard = true, triggerError = true)
                return false
            }
        }

        // second: check if there are not yet visible items
        if (!allSeen && lastVisibleItemPosition < adapter.itemCount - 1) { // Second condition is if we never scrolled
            maxSeen = max(maxSeen, lastVisibleItemPosition)
            if (maxSeen < adapter.itemCount - 1) {
                SnackbarHelper.show(activity, activity?.getString(R.string.scroll_to_not_seen))
                scrollTo(position = maxSeen + 2, shouldHideKeyboard = true, triggerError = true)
            }
            return false
        }

        // third: check any not valid item
        var formIsValid = true
        validationMap.values.forEachIndexed { index, isValid ->
            if (!isValid) {
                if (formIsValid) { // scroll to first not valid
                    scrollTo(position = index, shouldHideKeyboard = true, triggerError = true)
                }
                formIsValid = false
            }
        }
        if (!formIsValid) {
            return false
        }

        return true
    }

    private fun scrollTo(position: Int, shouldHideKeyboard: Boolean, triggerError: Boolean) {
        if (shouldHideKeyboard) onHideKeyboardCallback()
        if (position == -1) return
        scrollPos = if (position > adapter.itemCount - 1) {
            adapter.itemCount - 1
        } else {
            position
        }
        if (triggerError) {
            binding.parametersRecyclerView.removeOnScrollListener(onScrollToErrorListener)
            binding.parametersRecyclerView.addOnScrollListener(onScrollToErrorListener)
        }
        binding.parametersRecyclerView.smoothScrollToPosition(scrollPos)
    }

    private fun triggerError(position: Int) {
        (binding.parametersRecyclerView.findViewHolderForLayoutPosition(position) as? BaseViewHolder)?.validate(true)
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
        val actionTask = ActionTask(
            status = ActionTask.Status.PENDING,
            date = Date(),
            relatedItemId = (selectedEntity?.__entity as? EntityModel)?.__KEY,
            label = action.getPreferredName(),
            actionInfo = getActionInfo()
        )
        actionTask.actionContent = getActionContent(actionTask.id, (selectedEntity?.__entity as? EntityModel)?.__KEY)

        actionTask.saveInputControlFormatHolders(inputControlFormatHolders)

        return actionTask
    }

    private fun sendAction() {
        val pendingTask = createPendingTask()
        actionActivity.sendAction(actionTask = pendingTask, tableName = tableName) {
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
        action.parameters.getSafeObject(parameterPosition)?.getSafeString("name")?.let { parameterName ->
            imagesToUpload[parameterName] = uri
            paramsToSubmit[parameterName] = uri
            adapter.notifyItemChanged(parameterPosition)
        }
    }

    private fun actionTypesCallback(actionType: Action.Type, position: Int) {
        parameterPosition = position
        when (actionType) {
            Action.Type.PICK_PHOTO_GALLERY -> pickPhotoFromGallery()
            Action.Type.TAKE_PICTURE_CAMERA -> takePhotoFromCamera()
            Action.Type.SCAN -> scan()
            Action.Type.SIGN -> showSignDialog()
        }
    }

    private fun goToPushFragment(position: Int) {
        parameterPosition = position
        val actionParameter = action.parameters.getSafeObject(parameterPosition)
        actionParameter?.getSafeString("source")?.let { format ->
            setSharedAxisXExitTransition()
            val isMandatory = actionParameter.getSafeArray("rules")?.getStringList()?.contains("mandatory") ?: false
            BaseApp.genericNavigationResolver.navigateToPushInputControl(binding, format.removePrefix("/"), isMandatory)
        }
    }

    private fun formatHolderCallback(inputControlFormatHolder: InputControlFormatHolder, position: Int) {
        if (inputControlFormatHolder.displayText.isEmpty() && inputControlFormatHolder.fieldValue == null) {
            inputControlFormatHolders.remove(position)
        } else {
            inputControlFormatHolders[position] = inputControlFormatHolder
        }
    }

    // Callback triggered with an input control datasource has done loading its data and displayed it
    private fun onDataLoadedCallback() {
        scrollTo(position = parameterPosition, shouldHideKeyboard = false, triggerError = false)
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
        setSharedAxisZExitTransition()
        BaseApp.genericNavigationResolver.navigateToActionScanner(binding, parameterPosition)
    }

    private fun registerImageFromGallery(): ActivityResultLauncher<String> {
        return registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                onImageChosen(uri)
            }
        }
    }

    private fun registerCameraImage(): ActivityResultLauncher<Uri> {
        return registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                onImageChosen(Uri.fromFile(File(currentPhotoPath)))
            }
        }
    }
}
