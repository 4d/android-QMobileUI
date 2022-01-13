/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.qmobile.qmobileapi.auth.isEmailValid
import com.qmobile.qmobileapi.auth.isUrlValid
import com.qmobile.qmobileapi.model.entity.EntityHelper
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionParameterEnum
import com.qmobile.qmobileui.ui.ViewUtils
import org.json.JSONObject

open class TextViewHolder(itemView: View, private val format: String, private val hideKeyboardCallback: () -> Unit) :
    BaseViewHolder(itemView, hideKeyboardCallback) {

    val input: TextInputEditText = itemView.findViewById(R.id.input)
    val container: TextInputLayout = itemView.findViewById(R.id.container)

    private val shakeAnimation = ViewUtils.getShakeAnimation(itemView.context)

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntityJsonObject, onValueChanged)

        input.inputType = getInputType(format)

        val placeholder = itemJsonObject.getSafeString("placeholder") ?: ""

        input.setText(itemJsonObject.getSafeString("default") ?: "")
        container.hint = placeholder

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Nothing to do
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nothing to do
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { onValueChanged(parameterName, s.toString(), null, validate(false)) }
            }
        })

        input.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                container.error = null
            } else {
                if (validate(true))
                    container.error = null
                hideKeyboardCallback()
            }
        }
        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged) {
            input.setText(it.toString())
        }
        onValueChanged(parameterName, input.text.toString(), null, validate(false))
    }

    override fun getInputType(format: String): Int {
        return when (format) {
            ActionParameterEnum.TEXT_DEFAULT.format -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            ActionParameterEnum.TEXT_ZIP.format -> InputType.TYPE_CLASS_NUMBER
            ActionParameterEnum.TEXT_PHONE.format -> InputType.TYPE_CLASS_PHONE
            ActionParameterEnum.TEXT_EMAIL.format -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            ActionParameterEnum.TEXT_URL.format -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            ActionParameterEnum.TEXT_PASSWORD.format -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            ActionParameterEnum.TEXT_ACCOUNT.format -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            ActionParameterEnum.TEXT_AREA.format -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            else -> InputType.TYPE_CLASS_TEXT
        }
    }

    override fun validate(displayError: Boolean): Boolean {

        if (isMandatory() && input.text.toString().trim().isEmpty()) {
            if (displayError)
                showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }

        if (isMandatory() && (format == ActionParameterEnum.TEXT_EMAIL.format)) {
            if (!input.text.toString().isEmailValid()) {
                if (displayError)
                    showError(itemView.context.resources.getString(R.string.action_parameter_invalid_email_error))
                return false
            }
        }

        if (isMandatory() && (format == ActionParameterEnum.TEXT_URL.format)) {
            if (!input.text.toString().isUrlValid()) {
                if (displayError)
                    showError(itemView.context.resources.getString(R.string.action_parameter_invalid_url_error))
                return false
            }
        }
        return true
    }

    fun showError(text: String) {
        container.error = text
        input.startAnimation(shakeAnimation)
    }
}
