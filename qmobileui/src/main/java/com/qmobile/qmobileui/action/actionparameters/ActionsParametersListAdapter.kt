/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobileui.action.actionparameters.viewholder.ActionParameterViewHolder
import com.qmobile.qmobileui.action.actionparameters.viewholder.ActionParameterViewHolderFactory
import com.qmobile.qmobileui.action.actionparameters.viewholder.ImageViewHolder
import com.qmobile.qmobileui.action.actionparameters.viewholder.SignatureViewHolder
import org.json.JSONArray
import org.json.JSONObject

class ActionsParametersListAdapter(
    context: Context,
    val list: JSONArray,
    // contains data of the failed action, this data will be used for pre-fill form to edit pending task
    private val paramsToSubmit: HashMap<String, Any>,
    private val imagesToUpload: HashMap<String, Uri>,
    private val currentEntity: RoomEntity?,
    val onValueChanged: (String, Any?, String?, Boolean) -> Unit,
    val goToScanner: (Int) -> Unit,
    val goToCamera: (Intent, Int, String) -> Unit,
    val queueForUpload: (String, Uri?) -> Unit,
    val paramsError: HashMap<String, String>
) :
    RecyclerView.Adapter<ActionParameterViewHolder>() {

    private val context: Context = context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionParameterViewHolder {
        return ActionParameterViewHolderFactory.createViewHolderFromViewType(
            viewType,
            parent,
            context
        )
    }

    override fun getItemCount(): Int {
        return list.length()
    }

    override fun onBindViewHolder(holder: ActionParameterViewHolder, position: Int) {
        val item = list[position]
        val paramName = (item as JSONObject).getSafeString("name")

        // Value used to pre-fill offline action edit form
        val alreadyFilledValue = when (holder) {
            is ImageViewHolder, is SignatureViewHolder ->
                // if image or signature we take the uri to pre-fill image/signature preview
                imagesToUpload[paramName]
            else ->
                // for other field we take the value to prefill editText
                paramsToSubmit[paramName]
        }

        // Error returned from server for this specific param
        val errorText = paramsError[paramName]

        holder.bind(
            item,
            currentEntity,
            alreadyFilledValue,
            { name: String, value: Any?, metaData: String?, isValid: Boolean ->
                onValueChanged(name, value, metaData, isValid)
            },
            {
                goToScanner(it)
            },
            { intent: Intent, pos: Int, destinationPath ->
                goToCamera(intent, pos, destinationPath)
            },
            { parameterName: String, uri: Uri? ->
                queueForUpload(parameterName, uri)
            },
            errorText
        )
    }

    override fun getItemViewType(position: Int): Int {
        val itemJsonObject = list[position] as JSONObject
        val type = itemJsonObject.getSafeString("type")
        val format = itemJsonObject.getSafeString("format") ?: "default"
        return ActionParameterEnum.values().find { it.type == type && it.format == format }?.ordinal
            ?: 0
    }

    fun updateImageForPosition(position: Int, data: Any) {
        if (data is Uri) {
            (list[position] as JSONObject).put("uri", data)
        }
        notifyItemChanged(position)
    }

    fun getUpdatedImageParameterName(position: Int): String? {
        return (list[position] as JSONObject).getSafeString("name")
    }
}
