/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.qmobile.qmobileapi.model.entity.EntityHelper
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileapi.utils.getStringList
import com.qmobile.qmobileui.R
import org.json.JSONObject

abstract class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    lateinit var parameterName: String
    lateinit var itemJsonObject: JSONObject
    private var isEditPreset = false

    open fun bind(
        item: Any,
        currentEntity: EntityModel?,
        preset: String?,
        isLastParameter: Boolean,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        itemJsonObject = item as JSONObject
        parameterName = itemJsonObject.getSafeString("name") ?: ""
        isEditPreset = preset == "edit"

        if (isMandatory()) {
            onValueChanged(parameterName, "", null, validate(false))
        }

        itemView.findViewById<TextInputEditText>(R.id.input)?.imeOptions =
            if (isLastParameter) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
    }

    abstract fun validate(displayError: Boolean): Boolean

    abstract fun getInputType(format: String): Int

    open fun getDefaultFieldValue(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        valueCallback: (Any) -> Unit
    ) {
        currentEntity?.let { entity ->
            val fieldName = if (isEditPreset)
                itemJsonObject.getSafeString("name")
            else
                itemJsonObject.getSafeString("defaultField")
            fieldName?.let {
                EntityHelper.readInstanceProperty(entity, it)?.also { value ->
                    valueCallback(value)
                }
            }
        }
    }

    fun isMandatory(): Boolean {
        return itemJsonObject.getSafeArray("rules")?.getStringList()?.contains("mandatory") ?: false
    }
}
