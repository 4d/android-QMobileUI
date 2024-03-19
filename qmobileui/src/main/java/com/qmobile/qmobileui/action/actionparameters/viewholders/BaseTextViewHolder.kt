/*
 * Created by qmarciset on 12/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.ui.getShakeAnimation
import org.json.JSONObject

abstract class BaseTextViewHolder(itemView: View, private val format: String) : BaseViewHolder(itemView, format) {

    val input: TextInputEditText = itemView.findViewById(R.id.input)
    protected val container: TextInputLayout = itemView.findViewById(R.id.container)
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

        input.inputType = getInputType(format)

        itemJsonObject.getSafeString("placeholder")?.let {
            container.helperText = it
        }

        itemJsonObject.getSafeString("label")?.let { parameterLabel ->
            container.hint = if (isMandatory()) {
                "$parameterLabel *"
            } else {
                parameterLabel
            }
        }

        input.setText(itemJsonObject.getSafeString("default") ?: "")
        container.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Nothing to do
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nothing to do
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { onTextChanged(it) }
            }
        })

        input.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                container.error = null
                input.setText(unformattedValue(input.text.toString()))
            } else {
                input.setText(formattedValue(input.text.toString()))
                if (validate(true)) {
                    container.error = null
                }
            }
        }

        alreadyFilledValue?.let {
            fill(it)
        } ?: kotlin.run {
            getDefaultFieldValue(currentEntity, itemJsonObject) {
                fill(it)
            }
        }

        onValueChanged(parameterName, getValueToSend(), null, validate(false))

        showServerError()
    }

    override fun showError(text: String) {
        container.error = text
        input.startAnimation(shakeAnimation)
    }

    override fun fill(value: Any) {
        val string = if (value == JSONObject.NULL) "" else value.toString()
        input.setText(formattedValue(string))
    }

    abstract fun onTextChanged(s: CharSequence)

    abstract fun getValueToSend(): String

    abstract fun formattedValue(input: String): String
    abstract fun unformattedValue(input: String): String
}
