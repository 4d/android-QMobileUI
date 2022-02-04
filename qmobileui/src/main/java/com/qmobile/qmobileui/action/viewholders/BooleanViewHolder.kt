/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R

class BooleanViewHolder(itemView: View, hideKeyboardCallback: () -> Unit) :
    BaseViewHolder(itemView, hideKeyboardCallback) {

    private var compoundButton: CompoundButton = itemView.findViewById(R.id.compoundButton)
    private val label: TextView = itemView.findViewById(R.id.label)

    override fun bind(
        item: Any,
        currentEntity: EntityModel?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, onValueChanged)

        itemJsonObject.getSafeString("label")?.let { parameterLabel ->
            label.text = if (isMandatory()) {
                "$parameterLabel *"
            } else {
                parameterLabel
            }
        }

        compoundButton.setOnCheckedChangeListener { _, b ->
            onValueChanged(parameterName, b, null, true)
        }
        setDefaultFieldIfNeeded(currentEntity, itemJsonObject, onValueChanged) {
            if (it is Boolean)
                compoundButton.isChecked = it
        }

        onValueChanged(parameterName, compoundButton.isChecked, null, validate(false))
    }

    override fun validate(displayError: Boolean): Boolean {
        return true
    }

    override fun getInputType(format: String): Int {
        return -1
    }
}
