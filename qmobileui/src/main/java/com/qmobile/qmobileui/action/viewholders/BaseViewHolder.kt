/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileapi.model.entity.EntityHelper
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeInt
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R
import org.json.JSONObject

abstract class BaseViewHolder(itemView: View, private val hideKeyboardCallback: () -> Unit) :
    RecyclerView.ViewHolder(itemView) {

    lateinit var parameterName: String
    lateinit var itemJsonObject: JSONObject
    val label: TextView = itemView.findViewById(R.id.label)

    open fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        itemJsonObject = item as JSONObject
        parameterName = itemJsonObject.getSafeString("name") ?: ""

        itemJsonObject.getSafeString("label")?.let { parameterLabel ->
            label.text = if (isMandatory()) {
                "$parameterLabel *"
            } else {
                parameterLabel
            }
        }

        if (isMandatory()) {
            onValueChanged(parameterName, "", null, validate(false))
        }

        itemView.setOnClickListener {
            hideKeyboardCallback()
        }
    }

    abstract fun validate(displayError: Boolean): Boolean

    abstract fun getInputType(format: String): Int

    open fun setDefaultFieldIfNeeded(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        onValueChanged: (String, Any, String?, isValid: Boolean) -> Unit,
        valueCallback: (Any) -> Unit
    ) {
        currentEntity?.let {
            itemJsonObject.getSafeString("defaultField")?.let { defaultField ->  // "defaultField" ? semble pas exister
                EntityHelper.readInstanceProperty(it, defaultField).also { value ->
                    valueCallback(value)
                }
            }
        }
    }

    fun isMandatory(): Boolean {
        return itemJsonObject.getSafeArray("rules")?.toString()?.contains("mandatory") ?: false
    }

    fun getMin(): Int? {
        itemJsonObject.getSafeArray("rules")?.let { jsonArray ->
            for (i in 0 until jsonArray.length()) {

                val rule = jsonArray.getSafeObject(i)
                rule?.getSafeInt("min")?.let {
                    return it
                }
            }
        }
        return null
    }

    fun getMax(): Int? {
        itemJsonObject.getSafeArray("rules")?.let { jsonArray ->
            for (i in 0 until jsonArray.length()) {

                val rule = jsonArray.getSafeObject(i)
                rule?.getSafeInt("max")?.let {
                    return it
                }
            }
        }
        return null
    }
}
