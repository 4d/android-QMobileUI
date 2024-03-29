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
import com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols.BaseInputControlViewHolder
import com.qmobile.qmobileui.ui.getShakeAnimation
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import org.json.JSONObject

abstract class BaseInputLessViewHolder(itemView: View, format: String) : BaseViewHolder(itemView, format) {

    internal val input: TextInputEditText = itemView.findViewById(R.id.input)
    internal val container: TextInputLayout = itemView.findViewById(R.id.container)

    private val shakeAnimation = getShakeAnimation(itemView.context)

    override fun bind(
        item: JSONObject,
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
            itemJsonObject.getSafeString("placeholder") != null -> {
                input.setText(itemJsonObject.getSafeString("placeholder"))
                itemJsonObject.getSafeString("placeholder")?.let {
                    if (this is BaseInputControlViewHolder) {
                        (this as BaseInputControlViewHolder).placeHolder = it
                    }
                }
            }
        }

        container.endIconMode = TextInputLayout.END_ICON_CUSTOM
        input.isFocusable = false
        input.isLongClickable = false
        input.isCursorVisible = false

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
                showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
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
        val string = if (value == JSONObject.NULL) "" else value.toString()
        input.setText(formatToDisplay(string))
    }

    override fun getInputType(format: String): Int {
        return -1
    }

    internal fun setOnSingleClickListener(onClick: () -> Unit) {
        input.setOnSingleClickListener {
            onClick()
        }
    }

    abstract fun formatToDisplay(input: String): String
}
