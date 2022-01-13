/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import com.qmobile.qmobileapi.model.entity.EntityHelper
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R
import org.json.JSONObject

class BooleanViewHolder(itemView: View, hideKeyboardCallback: () -> Unit): BaseViewHolder(itemView, hideKeyboardCallback) {

    private var compoundButton: CompoundButton = itemView.findViewById(R.id.compoundButton)

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntityJsonObject, onValueChanged)

        compoundButton.setOnCheckedChangeListener { _, b ->
            onValueChanged(parameterName, b, null, true)
        }
        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged) {
            if (it is Boolean)
                compoundButton.isChecked = it
        }
    }

    override fun validate(displayError: Boolean): Boolean {
        return true
    }

    override fun getInputType(format: String): Int {
        return -1
    }

}