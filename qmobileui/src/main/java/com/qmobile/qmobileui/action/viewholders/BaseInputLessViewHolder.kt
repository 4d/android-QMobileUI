/*
 * Created by qmarciset on 14/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.view.View
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.ui.getShakeAnimation

abstract class BaseInputLessViewHolder(itemView: View) : BaseViewHolder(itemView) {

    internal val input: TextInputEditText = itemView.findViewById(R.id.input)
    internal val container: TextInputLayout = itemView.findViewById(R.id.container)

    private val shakeAnimation = getShakeAnimation(itemView.context)

    override fun bind(
        item: Any,
        currentEntity: EntityModel?,
        preset: String?,
        isLastParameter: Boolean,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, preset, isLastParameter, onValueChanged)

        itemJsonObject.getSafeString("label")?.let { parameterLabel ->
            container.hint = if (isMandatory()) {
                "$parameterLabel *"
            } else {
                parameterLabel
            }
        }

        if (itemJsonObject.getSafeString("default") != null)
            input.setText(itemJsonObject.getSafeString("default"))
        else if (itemJsonObject.getSafeString("placeholder") != null)
            input.setText(itemJsonObject.getSafeString("placeholder"))

        container.endIconMode = TextInputLayout.END_ICON_CUSTOM
        input.isFocusable = false
        input.isLongClickable = false

        getDefaultFieldValue(currentEntity, itemJsonObject) {
            input.setText(formatToDisplay(it.toString()))
        }
    }

    override fun validate(displayError: Boolean): Boolean {
        if (isMandatory() && input.text.toString().trim().isEmpty()) {
            if (displayError)
                showError(itemView.context.getString(R.string.action_parameter_mandatory_error))
            return false
        }
        return true
    }

    private fun showError(text: String) {
        container.error = text
        input.startAnimation(shakeAnimation)
    }

    override fun getInputType(format: String): Int {
        return -1
    }

    internal fun onEndIconClick(endIconClick: () -> Unit) {
        container.setEndIconOnClickListener {
            endIconClick()
        }
    }

    abstract fun formatToDisplay(input: String): String
}
