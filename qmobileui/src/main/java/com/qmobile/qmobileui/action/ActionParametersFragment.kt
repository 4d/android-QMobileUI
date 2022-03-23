/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.action

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
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.dao.ActionInfo
import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobiledatastore.dao.ActionTaskDao
import com.qmobile.qmobiledatastore.dao.STATUS
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.toast.MessageType
import com.qmobile.qmobiledatasync.viewmodel.EntityListViewModel
import com.qmobile.qmobiledatasync.viewmodel.factory.getEntityListViewModel
import com.qmobile.qmobileui.BaseFragment
import com.qmobile.qmobileui.FragmentCommunication
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.databinding.FragmentActionParametersBinding
import com.qmobile.qmobileui.network.NetworkChecker
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Date
import kotlin.collections.HashMap

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
    private var paramsToSubmit = HashMap<String, Any>()
    private val metaDataToSubmit = HashMap<String, String>()
    private var imagesToUpload = HashMap<String, Uri>()
    private var validationMap = HashMap<String, Boolean>()
    lateinit var adapter: ActionsParametersListAdapter
    private var fromRelation = false
    private lateinit var actionTaskDao: ActionTaskDao
    private lateinit var allParameters: JSONArray

    // Is set to true if all recyclerView items are seen at lean once
    private var areAllItemsSeen = false
    private var goToCamera: (() -> Unit)? = null
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    var currentDestinationPath: String? = null
    var currentPendingTaskDate: Date? = null

    private var currentTask: ActionTask? = null
    private var taskId: Long? = null

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
        arguments?.getLong("taskId")?.let {
            taskId = it
        }

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
                if (taskId != 0L) {
                    actionTaskDao.getAll().observeOnce(requireParentFragment(), {
                        it.find { actionTask -> actionTask.id == taskId }?.let { task ->
                            task.actionInfo.validationMap?.let { map -> validationMap = map }
                            task.actionInfo.paramsToSubmit?.let { params ->
                                paramsToSubmit = params
                            }
                            imagesToUpload =
                                task.actionInfo.imagesToUpload?.mapValues { entry ->
                                    Uri.parse(entry.value)
                                } as HashMap<String, Uri>
                            allParameters = JSONArray(task.actionInfo.allParameters)
                            currentPendingTaskDate = task.date
                            currentTask = task
                        }
                        setupRecycleView(recyclerView)
                    })
                } else {
                    allParameters = delegate.getSelectAction().parameters
                    setupRecycleView(recyclerView)
                }
            }
        }
        return binding.root
    }

    private fun setupRecycleView(recyclerView: RecyclerView) {
        adapter = ActionsParametersListAdapter(
            requireContext(),
            allParameters,
            paramsToSubmit,
            imagesToUpload,
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
                        position
                    )
                }
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }, { parameterName: String, uri: Uri? ->
                if (uri != null) {
                    imagesToUpload[parameterName] = uri
                } else {
                    // When user signed and then cleared signature
                    // pad we should remove last signature from imagesToUpload

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
        actionTaskDao = BaseApp.daoProvider.getActionTaskDao()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_actions_parameters, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.validate) {
            // When coming from pending task
            if (taskId != 0L) {
                lifecycleScope.launch {
                    taskId?.let { id ->
                        currentPendingTaskDate?.let { date ->
                            currentTask?.let { task ->
                                actionTaskDao.insert(
                                    ActionTask(
                                        id = id,
                                        status = STATUS.PENDING,
                                        date = date,
                                        relatedItemId = currentTask?.relatedItemId,
                                        label = task.label,
                                        actionInfo = task.actionInfo
                                    )
                                )
                            }
                        }
                        activity?.onBackPressed()
                    }
                }
            } else {
                // When coming from actions
                var relatedEntityId: String? = null
                delegate.getSelectAction().preset?.let {
                    if (it == "edit") {
                        relatedEntityId = delegate.getSelectedEntity()?.__KEY
                    }
                }
                if (imagesToUpload.isNotEmpty()) {
                    uploadImages(
                        relatedEntityId,
                        delegate.getSelectAction().id
                    )
                } else {
                    sendAction(
                        relatedEntityId,
                        delegate.getSelectAction().id
                    )
                }
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

    private fun sendAction(relatedEntityId: String?, actionUUID: String) {
        if (isFormValid()) {
            delegate.checkNetwork(object : NetworkChecker {
                val task = ActionTask(
                    status = STATUS.PENDING,
                    date = Date(),
                    relatedItemId = relatedEntityId,
                    label = delegate.getSelectAction().getPreferredName(),
                    actionInfo = ActionInfo(
                        paramsToSubmit = paramsToSubmit,
                        metaDataToSubmit = metaDataToSubmit,
                        imagesToUpload = imagesToUpload.mapValues { entry ->
                            entry.value.path
                        } as HashMap<String, String>,
                        validationMap = validationMap,
                        allParameters = delegate.getSelectAction().parameters.toString(),
                        actionName = delegate.getSelectAction().name,
                        tableName = tableName,
                        currentRecordId = delegate.getSelectedEntity()?.__KEY,
                        actionUUID = delegate.getSelectAction().id
                    )
                )

                override fun onServerAccessible() {
                    if (delegate.getSelectAction().isOfflineCompatible()) {
                        lifecycleScope.launch {
                            task.id = actionTaskDao.insert(task)
                        }
                    }

                    entityListViewModel.sendAction(
                        delegate.getSelectAction().name,
                        ActionHelper.getActionContent(
                            tableName,
                            relatedEntityId,
                            paramsToSubmit,
                            metaDataToSubmit,
                            relationName = inverseName,
                            parentPrimaryKey = parentItemId,
                            parentTableName = parentTableName,
                            parentRelationName = parentRelationName,
                            actionUUID = actionUUID
                        )
                    ) { actionResponse ->
                        actionResponse?.let {

                            lifecycleScope.launch {
                                val status = if (actionResponse.success) {
                                    STATUS.SUCCESS
                                } else {
                                    STATUS.ERROR_SERVER
                                }
                                task.status = status
                                task.message = actionResponse.statusText
                                actionTaskDao.insert(
                                    task
                                )
                            }

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
                    lifecycleScope.launch {
                        val jsonObject = JSONObject()
                        jsonObject.put("params", delegate.getSelectAction().parameters)

                        if (delegate.getSelectAction().isOfflineCompatible()) {
                            if (shouldShowActionError()) {
                                entityListViewModel.toastMessage.showMessage(
                                    context?.getString(R.string.action_send_server_not_accessible),
                                    tableName,
                                    MessageType.NEUTRAL
                                )
                            }
                            actionTaskDao.insert(task)
                        }
                        activity?.onBackPressed()
                    }
                }

                override fun onNoInternet() {
                    lifecycleScope.launch {
                        if (delegate.getSelectAction().isOfflineCompatible()) {
                            actionTaskDao.insert(
                                task
                            )
                            if (shouldShowActionError()) {
                                entityListViewModel.toastMessage.showMessage(
                                    context?.getString(R.string.action_send_no_internet),
                                    tableName,
                                    MessageType.NEUTRAL
                                )
                            }
                        }
                        activity?.onBackPressed()
                    }
                }
            })
        }
    }

    private fun uploadImages(relatedEntityId: String?, actionUUID: String) {
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
            val task = ActionTask(
                status = STATUS.PENDING,
                date = Date(),
                relatedItemId = relatedEntityId,
                label = delegate.getSelectAction().getPreferredName(),
                actionInfo = ActionInfo(
                    paramsToSubmit = paramsToSubmit,
                    metaDataToSubmit = metaDataToSubmit,
                    imagesToUpload = imagesToUpload.mapValues { entry ->
                        entry.value.toString()
                    } as HashMap<String, String>,
                    validationMap = validationMap,
                    allParameters = delegate.getSelectAction().parameters.toString(),
                    actionName = delegate.getSelectAction().name,
                    tableName = tableName,
                    currentRecordId = delegate.getSelectedEntity()?.__KEY,
                    actionUUID = delegate.getSelectAction().id
                )
            )

            override fun onServerAccessible() {
                entityListViewModel.uploadImage(bodies, { parameterName, receivedId ->
                    paramsToSubmit[parameterName] = receivedId
                    metaDataToSubmit[parameterName] = "uploaded"
                }) {
                    sendAction(relatedEntityId, actionUUID)
                }
            }

            override fun onServerInaccessible() {

                lifecycleScope.launch {
                    actionTaskDao.insert(
                        task
                    )
                }
                entityListViewModel.toastMessage.showMessage(
                    context?.getString(R.string.action_send_server_not_accessible),
                    tableName,
                    MessageType.NEUTRAL
                )
                activity?.onBackPressed()
            }

            override fun onNoInternet() {
                lifecycleScope.launch {
                    actionTaskDao.insert(
                        task
                    )
                }
                entityListViewModel.toastMessage.showMessage(
                    context?.getString(R.string.action_send_no_internet),
                    tableName,
                    MessageType.NEUTRAL
                )
                activity?.onBackPressed()
            }
        })
    }

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
