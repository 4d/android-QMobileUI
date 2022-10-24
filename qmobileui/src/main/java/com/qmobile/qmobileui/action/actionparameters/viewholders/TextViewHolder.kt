/*
 * Created by qmarciset on 11/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.text.InputType
import android.view.View
import com.qmobile.qmobileapi.auth.isEmailValid
import com.qmobile.qmobileapi.auth.isUrlValid
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobileui.R
import org.json.JSONObject

open class TextViewHolder(itemView: View, format: String) :
    BaseTextViewHolder(itemView, format) {

    companion object {
        private const val maxLines = 15
        private const val minLines = 3
    }

    override fun bind(
        item: JSONObject,
        currentEntity: RoomEntity?,
        isLastParameter: Boolean,
        alreadyFilledValue: Any?,
        serverError: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, isLastParameter, alreadyFilledValue, serverError, onValueChanged)

        if (apu.isTextArea()) {
            input.maxLines = maxLines
            input.minLines = minLines
        }
    }

    override fun onTextChanged(s: CharSequence) {
        onValueChanged(parameterName, s.toString(), null, validate(false))
    }

    override fun getInputType(format: String): Int {
        return when {
            apu.isTextDefault() -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            apu.isZip() || apu.isPhone() -> InputType.TYPE_CLASS_NUMBER
            apu.isEmail() -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            apu.isUrl() -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            apu.isPassword() -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            apu.isAccount() -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            apu.isTextArea() -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            else -> InputType.TYPE_CLASS_TEXT
        }
    }

    @Suppress("ReturnCount")
    override fun validate(displayError: Boolean): Boolean {
        if (isMandatory() && input.text.toString().trim().isEmpty()) {
            if (displayError) {
                showError(itemView.context.getString(R.string.action_parameter_mandatory_error))
            }
            return false
        }

        if (input.text.toString().trim().isEmpty()) {
            return true
        }

        if (apu.isEmail() && !input.text.toString().isEmailValid()) {
            if (displayError) {
                showError(itemView.context.getString(R.string.action_parameter_invalid_email_error))
            }
            return false
        }

        if (apu.isUrl() && !input.text.toString().isUrlValid()) {
            if (displayError) {
                showError(itemView.context.getString(R.string.action_parameter_invalid_url_error))
            }
            return false
        }
        return true
    }

    override fun getValueToSend(): String {
        return input.text.toString()
    }

    override fun formattedValue(input: String): String = input

    override fun unformattedValue(input: String): String = input
}
