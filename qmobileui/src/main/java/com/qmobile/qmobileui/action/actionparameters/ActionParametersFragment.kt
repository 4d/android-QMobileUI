/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.APP_OCTET
import com.qmobile.qmobiledatastore.dao.ActionInfo
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.relation.Relation
import com.qmobile.qmobiledatasync.relation.RelationHelper
import com.qmobile.qmobileui.ActionActivity
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionProvider
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.action.utils.ActionHelper
import com.qmobile.qmobileui.action.utils.createImageFile
import com.qmobile.qmobileui.action.viewholder.BITMAP_QUALITY
import com.qmobile.qmobileui.action.viewholder.ORIGIN_POSITION
import com.qmobile.qmobileui.databinding.FragmentActionParametersBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Date
import kotlin.collections.HashMap

class ActionParametersFragment : BaseFragment(), ActionProvider {

    // views
    private lateinit var adapter: ActionsParametersListAdapter
    private var _binding: FragmentActionParametersBinding? = null
    private val binding get() = _binding!!
//    private lateinit var signatureDialog: View

    // fragment parameters
    override var tableName = ""
    private var itemId = ""
    private var parentItemId = ""
    private var relation: Relation? = null

    override lateinit var actionActivity: ActionActivity

    internal var paramsToSubmit = HashMap<String, Any>()
    private val metaDataToSubmit = HashMap<String, String>()
    internal var validationMap = hashMapOf<String, Boolean>()
    internal var imagesToUpload = HashMap<String, Uri>()

    //    private var scrollPos = 0
//    private lateinit var currentPhotoPath: String
    private lateinit var action: Action
    private var selectedEntity: RoomEntity? = null
//    private var actionPosition = -1

    // Is set to true if all recyclerView items are seen at lean once
    private var areAllItemsSeen = false
    private var goToCamera: (() -> Unit)? = null
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    var currentDestinationPath: String? = null

    internal var currentTask: ActionTask? = null
    internal var taskId: String? = null
    private var fromPendingTasks = false
    internal lateinit var allParameters: JSONArray

    companion object {
        const val BARCODE_FRAGMENT_REQUEST_KEY = "scan_request"
    }

//    private val onScrollListener = object : RecyclerView.OnScrollListener() {
//        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                triggerError(scrollPos)
//                recyclerView.removeOnScrollListener(this)
//            }
//        }
//    }

//    private val getImageFromGallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//        uri?.let {
//            onImageChosen(uri)
//        }
//    }

//    private val getCameraImage = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
//        if (success)
//            onImageChosen(Uri.fromFile(File(currentPhotoPath)))
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        setHasOptionsMenu(true)
        arguments?.getString("tableName")?.let { tableName = it }
        arguments?.getString("itemId")?.let { itemId = it }
        arguments?.getString("taskId")?.let {
            if (it.isNotEmpty()) {
                taskId = it
                fromPendingTasks = true
            }
        }
        arguments?.getString("relationName")?.let { relationName ->
            if (relationName.isNotEmpty())
                relation = RelationHelper.getRelation(tableName, relationName)
            arguments?.getString("parentItemId")?.let { parentItemId = it }
        }

//        setFragmentResultListener(BARCODE_FRAGMENT_REQUEST_KEY) { _, bundle ->
//            bundle.getString("barcode_value")?.let {
//                adapter.updateBarcodeForPosition(actionPosition, it)
//            }
//        }

        allParameters = if (!fromPendingTasks)
            action.parameters
        else
            JSONArray()

        _binding = FragmentActionParametersBinding.inflate(
            inflater,
            container,
            false
        ).apply {
            lifecycleOwner = viewLifecycleOwner
//            adapter = ActionsParametersListAdapter(
//                context = requireContext(),
//                action = action,
//                currentEntity = selectedEntity,
//                fragmentManager = activity?.supportFragmentManager,
//                hideKeyboardCallback = { onHideKeyboardCallback() },
//                focusNextCallback = { position -> onFocusNextCallback(position) },
//                actionTypesCallback = { actionType, position ->
//                    actionTypesCallback(actionType, position)
//                },
//                onValueChanged = { name: String, value: Any?, metaData: String?, isValid: Boolean ->
//                    onValueChanged(name, value, metaData, isValid)
//                }
//            )
//            val smoothScroller = CenterLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
//            recyclerView.layoutManager = smoothScroller
//            recyclerView.adapter = adapter
//            // Important line : prevent recycled views to get their content reset
//            recyclerView.setItemViewCacheSize(action.parameters.length())
//            // Add this empty view to remove keyboard when click is perform below recyclerView items
//            emptyView.setOnClickListener {
//                onHideKeyboardCallback()
//            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupRecyclerView()
        ActionParametersFragmentObserver(this).initObservers()
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter

        val dividerItemDecoration = DividerItemDecoration(binding.recyclerView.context, layoutManager.orientation)
        binding.recyclerView.addItemDecoration(dividerItemDecoration)
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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

    internal fun setupAdapter() {
        adapter = ActionsParametersListAdapter(
            context = requireContext(),
            list = allParameters,
            paramsToSubmit = paramsToSubmit,
            imagesToUpload = imagesToUpload,
            currentEntity = selectedEntity?.__entity as EntityModel?,
            onValueChanged = { name: String, value: Any?, metaData: String?, isValid: Boolean ->
                validationMap[name] = isValid
                paramsToSubmit[name] = value ?: ""
                metaData?.let {
                    metaDataToSubmit[name] = metaData
                }
            },
            goToScanner = {
                BaseApp.genericNavigationResolver.navigateToActionScanner(binding, it)
            }, goToCamera = { intent: Intent, position: Int, destinationPath: String ->
            goToCamera = {
                currentDestinationPath = destinationPath
                (context as Activity).startActivityForResult(
                    intent,
                    // Send position as request code, so we can update image preview only for the selected item
                    position
                )
            }
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }, queueForUpload = { parameterName: String, uri: Uri? ->
            if (uri != null) {
                imagesToUpload[parameterName] = uri
            } else {
                // When user signed and then cleared signature
                // pad we should remove last signature from imagesToUpload

                imagesToUpload.remove(parameterName)
            }
        }
        )
    }

//    private fun onValueChanged(name: String, value: Any?, metaData: String?, isValid: Boolean) {
//        validationMap[name] = isValid
//        paramsToSubmit[name] = value ?: ""
//        metaData?.let {
//            metaDataToSubmit[name] = metaData
//        }
//        if (value == null) {
//            imagesToUpload.remove(name)
//        }
//    }

//    private fun onHideKeyboardCallback() {
//        activity?.let {
//            hideKeyboard(it)
//        }
//    }

//    private fun onFocusNextCallback(position: Int) {
//        binding.recyclerView.findViewHolderForLayoutPosition(position + 1)
//            ?.itemView?.findViewById<TextInputEditText>(R.id.input)
//            ?.requestFocus()
//            ?: kotlin.run {
//                Timber.d("Can't find input to focus at position ${position + 1}, scrolling now")
//                scrollTo(position + 1, false)
//            }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener("scan_request") { _: String, bundle: Bundle ->
            val value = bundle.getString("barcode_value")
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
            if (fromPendingTasks) {
                // When coming from pending task
                validatePendingTask()
            } else {
                // When coming from actions
                validateAction()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getActionInfo(): ActionInfo = ActionInfo(
        paramsToSubmit = paramsToSubmit,
        metaDataToSubmit = metaDataToSubmit,
        imagesToUpload = imagesToUpload.mapValues { entry ->
            entry.value.path
        } as HashMap<String, String>,
        validationMap = validationMap,
        allParameters = action.parameters.toString(),
        actionName = action.name,
        tableName = tableName,
        actionUUID = action.id,
        isOfflineCompatible = action.isOfflineCompatible(),
        preferredShortName = action.getPreferredShortName()
    )

    private fun validatePendingTask() {
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

    private fun isFormValid(): Boolean =
        if (!areAllItemsSeen)
            false
        else
            validationMap.values.firstOrNull { !it } == null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ActionActivity) {
            actionActivity = context
            action = actionActivity.getSelectedAction()
            selectedEntity = actionActivity.getSelectedEntity()
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

//    @Suppress("ReturnCount")
//    private fun isFormValid(): Boolean {
//
//        // first: check if visible items are valid
//        val firstNotValidItemPosition = validationMap.values.indexOfFirst { !it }
//        if (firstNotValidItemPosition > -1) {
//            scrollTo(firstNotValidItemPosition, true)
//            triggerError(firstNotValidItemPosition)
//            return false
//        }
//
//        // second: check if there are not yet visible items
//        val nbItems = adapter.itemCount
//        val viewedItems = validationMap.size
//        if (viewedItems < nbItems) {
//            val pos =
//                (binding.recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
//            if (pos < nbItems - 1) {
//                scrollTo(pos + 1, true)
//            }
//            return false
//        }
//
//        // third: check any not valid item
//        var formIsValid = true
//        validationMap.values.forEachIndexed { index, isValid ->
//            if (!isValid) {
//                if (formIsValid) { // scroll to first not valid
//                    scrollTo(index, true)
//                }
//                formIsValid = false
//                triggerError(index)
//            }
//        }
//        if (!formIsValid) {
//            return false
//        }
//
//        return true
//    }

//    private fun scrollTo(position: Int, shouldHideKeyboard: Boolean) {
//        if (shouldHideKeyboard) onHideKeyboardCallback()
//        scrollPos = position
//        binding.recyclerView.removeOnScrollListener(onScrollListener)
//        binding.recyclerView.addOnScrollListener(onScrollListener)
//        binding.recyclerView.smoothScrollToPosition(position)
//    }

//    private fun triggerError(position: Int) {
//        (binding.recyclerView.findViewHolderForLayoutPosition(position) as BaseViewHolder?)?.validate(
//            true
//        )
//    }

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
            actionContent = getActionContent(action.id, (selectedEntity?.__entity as EntityModel?)?.__KEY),
            actionTask = pendingTask,
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
            isFromAction = true,
            taskToSendIfOffline = if (action.isOfflineCompatible()) createPendingTask() else null,
            onImageUploaded = { parameterName, receivedId ->
                paramsToSubmit[parameterName] = receivedId
                metaDataToSubmit[parameterName] = "uploaded"
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

//    private fun onImageChosen(uri: Uri) {
//        action.parameters.getSafeObject(actionPosition)?.getSafeString("name")?.let { parameterName ->
//            imagesToUpload[parameterName] = uri
//        }
//        adapter.updateImageForPosition(actionPosition, uri)
//    }

//    private fun actionTypesCallback(actionType: Action.Type, position: Int) {
//        actionPosition = position
//        when (actionType) {
//            Action.Type.PICK_PHOTO_GALLERY -> pickPhotoFromGallery()
//            Action.Type.TAKE_PICTURE_CAMERA -> takePhotoFromCamera()
//            Action.Type.SCAN -> scan()
//            Action.Type.SIGN -> showSignDialog()
//        }
//    }

//    private fun showSignDialog() {
//        requireActivity().apply {
//            signatureDialog = LayoutInflater.from(this)
//                .inflate(R.layout.action_parameter_signature_dialog, findViewById(android.R.id.content), false)
//
//            MaterialAlertDialogBuilder(this, R.style.TitleThemeOverlay_MaterialComponents_MaterialAlertDialog)
//                .setView(signatureDialog)
//                .setTitle(getString(R.string.signature_dialog_title))
//                .setPositiveButton(getString(R.string.signature_dialog_positive)) { _, _ ->
//                    onSigned()
//                }
//                .setNegativeButton(getString(R.string.signature_dialog_cancel), null)
//                .show()
//        }
//    }

//    private fun onSigned() {
//        ImageHelper.getTempImageFile(requireContext(), Bitmap.CompressFormat.PNG) { _, signatureFilePath ->
//            File(signatureFilePath).apply {
//                val bitmap = try {
//                    signatureDialog.findViewById<SignaturePad>(R.id.signature_pad)?.transparentSignatureBitmap
//                } catch (e: IllegalArgumentException) {
//                    Timber.d("Could not get the signature bitmap (${e.message})")
//                    null
//                }
//                bitmap?.let {
//                    this.writeBitmap(it)
//                    onImageChosen(Uri.fromFile(this))
//                }
//            }
//        }
//    }

//    private fun pickPhotoFromGallery() {
//        getImageFromGallery.launch("image/*")
//    }
//
//    private fun takePhotoFromCamera() {
//        ImageHelper.getTempImageFile(requireContext(), Bitmap.CompressFormat.JPEG) { uri, photoFilePath ->
//            currentPhotoPath = photoFilePath
//            getCameraImage.launch(uri)
//        }
//    }
//
//    private fun scan() {
//        delegate.setFullScreenMode(true)
//        BaseApp.genericNavigationResolver.navigateToActionScanner(binding)
//    }

    @Suppress("NestedBlockDepth")
    fun handleResult(requestCode: Int, data: Intent?) {
        // the request code is te equivalent of position of item in adapter
        // case of image picked from gallery
        val uri = data?.data
        if (uri != null) {
            adapter.getUpdatedImageParameterName(requestCode)?.let {
                val bitmap: Bitmap =
                    MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri)
                try {
                    val photo: File? = try {
                        createImageFile(requireContext())
                    } catch (e: IOException) {
                        Timber.e("handleResult: ", e.localizedMessage)
                        null
                    }
                    saveBitmapToJPG(bitmap, photo)
                    imagesToUpload[it] = Uri.fromFile(photo)
                } catch (e: IOException) {
                    Timber.e("handleResult IOException : ", e.localizedMessage)
                }
            }
            adapter.updateImageForPosition(requestCode, uri)
        } else {
            // case of camera capture
            currentDestinationPath?.let {
                val currentDestinationPathUri = Uri.fromFile(File(it))
                adapter.getUpdatedImageParameterName(requestCode)?.let { parameterName ->
                    imagesToUpload[parameterName] = currentDestinationPathUri
                }
                adapter.updateImageForPosition(requestCode, currentDestinationPathUri)
            }
        }
    }

    private fun saveBitmapToJPG(bitmap: Bitmap, photo: File?) {
        val newBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, ORIGIN_POSITION, ORIGIN_POSITION, null)
        val stream: OutputStream = FileOutputStream(photo)
        newBitmap.compress(Bitmap.CompressFormat.JPEG, BITMAP_QUALITY, stream)
        stream.close()
    }
}
