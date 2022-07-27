/*
 * Created by qmarciset on 11/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.view.View
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.ui.getShakeAnimation

abstract class BaseInputLessViewHolder(itemView: View, format: String) : BaseViewHolder(itemView, format) {

    internal val input: TextInputEditText = itemView.findViewById(R.id.input)
    internal val container: TextInputLayout = itemView.findViewById(R.id.container)

    private val shakeAnimation = getShakeAnimation(itemView.context)

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
            container.hint = if (isMandatory()) {
                "$parameterLabel *"
            } else {
                parameterLabel
            }
        }

        when {
            itemJsonObject.getSafeString("default") != null ->
                input.setText(itemJsonObject.getSafeString("default"))
            itemJsonObject.getSafeString("placeholder") != null ->
                input.setText(itemJsonObject.getSafeString("placeholder"))
        }

        container.endIconMode = TextInputLayout.END_ICON_CUSTOM
        input.isFocusable = false
        input.isLongClickable = false

        alreadyFilledValue?.let {
            fill(it)
        } ?: kotlin.run {
            getDefaultFieldValue(currentEntity, itemJsonObject) {
                fill(it)
            }
        }

        showServerError()
    }

    override fun validate(displayError: Boolean): Boolean {
        if (isMandatory() && input.text.toString().trim().isEmpty()) {
            if (displayError) {
                showError(itemView.context.getString(R.string.action_parameter_mandatory_error))
            }
            return false
        }
        return true
    }

    override fun showError(text: String) {
        container.error = text
        input.startAnimation(shakeAnimation)
    }

    override fun fill(value: Any) {
        input.setText(formatToDisplay(value.toString()))
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
