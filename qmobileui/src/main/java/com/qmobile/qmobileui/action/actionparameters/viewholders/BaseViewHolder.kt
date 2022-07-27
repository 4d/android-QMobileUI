/*
 * Created by qmarciset on 11/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.getStringList
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.actionparameters.ActionParametersUtil
import com.qmobile.qmobileui.utils.ReflectionUtils
import org.json.JSONObject

abstract class BaseViewHolder(itemView: View, format: String) : RecyclerView.ViewHolder(itemView) {

    lateinit var parameterName: String
    lateinit var itemJsonObject: JSONObject
    private var serverError: String? = null
    val apu = ActionParametersUtil(format)

    open fun bind(
        item: Any,
        currentEntity: RoomEntity?,
        isLastParameter: Boolean,
        alreadyFilledValue: Any?,
        serverError: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        itemJsonObject = item as JSONObject
        parameterName = itemJsonObject.getSafeString("name") ?: ""
        this.serverError = serverError

        if (isMandatory()) {
            onValueChanged(parameterName, "", null, validate(false))
        }

        itemView.findViewById<TextInputEditText>(R.id.input)?.imeOptions =
            if (isLastParameter) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
    }

    abstract fun validate(displayError: Boolean): Boolean

    abstract fun getInputType(format: String): Int

    open fun getDefaultFieldValue(
        currentEntity: RoomEntity?,
        itemJsonObject: JSONObject,
        valueCallback: (Any) -> Unit
    ) {
        currentEntity?.let { entity ->
            itemJsonObject.getSafeString("defaultField")?.let { defaultField ->
                ReflectionUtils.getInstanceProperty(entity, defaultField)?.let { value ->
                    valueCallback(value)
                }
            }
        }
    }

    fun isMandatory() = itemJsonObject.getSafeArray("rules")?.getStringList()?.contains("mandatory") ?: false

    fun showServerError() {
        serverError?.let { error ->
            if (error.isNotEmpty()) {
                showError(error)
                serverError = null
            }
        }
    }

    abstract fun showError(text: String)

    abstract fun fill(value: Any)
}
