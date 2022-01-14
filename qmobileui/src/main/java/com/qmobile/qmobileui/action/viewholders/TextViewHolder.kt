/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.qmobile.qmobileapi.auth.isEmailValid
import com.qmobile.qmobileapi.auth.isUrlValid
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionParameterEnum
import com.qmobile.qmobileui.action.addSuffix
import com.qmobile.qmobileui.list.SpellOutHelper
import com.qmobile.qmobileui.ui.ViewUtils

open class TextViewHolder(
    itemView: View,
    private val format: String,
    private val hideKeyboardCallback: () -> Unit
) :
    BaseViewHolder(itemView, hideKeyboardCallback) {

    companion object {
        private const val maxLines = 15
        private const val minLines = 3
    }

    val input: TextInputEditText = itemView.findViewById(R.id.input)
    private val container: TextInputLayout = itemView.findViewById(R.id.container)
    private val shakeAnimation = ViewUtils.getShakeAnimation(itemView.context)
    var numberValueForSpellOut: String = ""

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntityJsonObject, onValueChanged)

        if (format == ActionParameterEnum.TEXT_AREA.format) {
            input.maxLines = maxLines
            input.minLines = minLines
        }

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

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Nothing to do
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nothing to do
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (format == ActionParameterEnum.NUMBER_SPELL_OUT.format)
                    s?.toString()?.toIntOrNull()?.toString().takeIf { it != "null" }
                        ?.let { numberValueForSpellOut = it }
                s?.let { onValueChanged(parameterName, s.toString(), null, validate(false)) }
            }
        })

        input.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                container.error = null
                unformatValue()
            } else {
                formatValue()
                if (validate(true))
                    container.error = null
                hideKeyboardCallback()
            }
        }
        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged) {
            input.setText(it.toString())
        }

        val valueForCallback = if (format == ActionParameterEnum.NUMBER_SPELL_OUT.format)
            numberValueForSpellOut
        else
            input.text.toString()
        onValueChanged(parameterName, valueForCallback, null, validate(false))

        addSuffix()
    }

    private fun addSuffix() {
        when (format) {
            ActionParameterEnum.NUMBER_PERCENTAGE.format -> {
                input.addSuffix("%")
            }
        }
    }

    override fun getInputType(format: String): Int {
        return when (format) {
            ActionParameterEnum.TEXT_DEFAULT.format ->
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            ActionParameterEnum.TEXT_ZIP.format -> InputType.TYPE_CLASS_NUMBER
            ActionParameterEnum.TEXT_PHONE.format -> InputType.TYPE_CLASS_PHONE
            ActionParameterEnum.TEXT_EMAIL.format ->
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            ActionParameterEnum.TEXT_URL.format ->
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            ActionParameterEnum.TEXT_PASSWORD.format ->
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            ActionParameterEnum.TEXT_ACCOUNT.format ->
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            ActionParameterEnum.TEXT_AREA.format ->
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            else -> InputType.TYPE_CLASS_TEXT
        }
    }

    @Suppress("ReturnCount")
    override fun validate(displayError: Boolean): Boolean {

        if (isMandatory() && input.text.toString().trim().isEmpty()) {
            if (displayError)
                showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }

        if (format == ActionParameterEnum.TEXT_EMAIL.format && !input.text.toString()
            .isEmailValid()
        ) {
            if (displayError)
                showError(itemView.context.resources.getString(R.string.action_parameter_invalid_email_error))
            return false
        }

        if (format == ActionParameterEnum.TEXT_URL.format && !input.text.toString().isUrlValid()) {
            if (displayError)
                showError(itemView.context.resources.getString(R.string.action_parameter_invalid_url_error))
            return false
        }
        return true
    }

    private fun formatValue() {
        when (format) {
            ActionParameterEnum.NUMBER_SCIENTIFIC.format -> {
                input.text.toString().toDoubleOrNull()?.let {
                    input.setText(it.toString())
                }
            }
            ActionParameterEnum.NUMBER_SPELL_OUT.format -> {
                input.text.toString().toLongOrNull()?.let {
                    input.setText(SpellOutHelper.convert(it))
                }
            }
        }
    }

    private fun unformatValue() {
        when (format) {
            ActionParameterEnum.NUMBER_SPELL_OUT.format -> {
                if (numberValueForSpellOut.isNotEmpty())
                    input.setText(numberValueForSpellOut)
            }
        }
    }

    fun showError(text: String) {
        container.error = text
        input.startAnimation(shakeAnimation)
    }
}
