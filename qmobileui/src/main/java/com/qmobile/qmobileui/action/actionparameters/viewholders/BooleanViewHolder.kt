/*
 * Created by qmarciset on 11/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobileui.R

open class BooleanViewHolder(itemView: View, format: String) : BaseViewHolder(itemView, format) {

    protected var compoundButton: CompoundButton = itemView.findViewById(R.id.compoundButton)
    private val label: TextView = itemView.findViewById(R.id.label)
    protected val error: TextView = itemView.findViewById(R.id.error)

    override fun bind(
        item: Any,
        currentEntity: RoomEntity?,
        isLastParameter: Boolean,
        alreadyFilledValue: Any?,
        serverError: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, isLastParameter, alreadyFilledValue, serverError, onValueChanged)

        itemJsonObject.getSafeString("label")?.let { parameterLabel ->
            label.text = if (isMandatory()) {
                "$parameterLabel *"
            } else {
                parameterLabel
            }
        }

        compoundButton.setOnCheckedChangeListener { _, b ->
            error.visibility = View.INVISIBLE
            onValueChanged(parameterName, b, null, true)
        }

        alreadyFilledValue?.let {
            fill(it)
        } ?: kotlin.run {
            getDefaultFieldValue(currentEntity, itemJsonObject) {
                fill(it)
            }
        }

        onValueChanged(parameterName, compoundButton.isChecked, null, validate(false))

        showServerError()
    }

    override fun fill(value: Any) {
        if (value is Boolean) {
            compoundButton.isChecked = value
        }
    }

    override fun showError(text: String) {
        error.text = text
        error.visibility = View.VISIBLE
    }

    override fun validate(displayError: Boolean): Boolean {
        return true
    }

    override fun getInputType(format: String): Int {
        return -1
    }
}
