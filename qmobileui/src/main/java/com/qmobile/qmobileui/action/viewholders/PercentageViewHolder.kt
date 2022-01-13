/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.text.Spannable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.addSuffix
import com.qmobile.qmobileui.ui.ViewUtils
import org.json.JSONObject

class PercentageViewHolder(itemView: View, private val format: String, private val hideKeyboardCallback: () -> Unit): BaseViewHolder(itemView, hideKeyboardCallback) {

    companion object {
        const val PERCENT_KEY = "%"
        const val PERCENT_MULTIPLIER = 0.01F
    }

    val input: TextInputEditText = itemView.findViewById(R.id.input)
    val container: TextInputLayout = itemView.findViewById(R.id.container)

    val shakeAnimation = ViewUtils.getShakeAnimation(itemView.context)

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

        input.addSuffix(PERCENT_KEY)
        input.setText(PERCENT_KEY)
        Selection.setSelection(input.text as Spannable?, input.text.toString().length - 1)

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Nothing to do
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nothing to do
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s.toString().replace(PERCENT_KEY, "").let {
                    if (it.isNotEmpty()) {
                        val percentValue =
                            "%.2f".format(it.toFloatOrNull()?.times(PERCENT_MULTIPLIER))
                        if (percentValue != null) {
                            onValueChanged(
                                parameterName,
                                percentValue,
                                null,
                                validate(false)
                            )
                        }
                    }
                }
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
        return InputType.TYPE_CLASS_NUMBER
    }

    override fun validate(displayError: Boolean): Boolean {
//        val textWithoutPercent = input.text.toString().replace(PERCENT_KEY, "").trim()
//        if (isMandatory() && textWithoutPercent.isEmpty()) {
//            if (displayError)
//                showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
//            return false
//        }
//
//        textWithoutPercent.toFloatOrNull()?.let { value ->
//            getMin()?.let { mix ->
//                if (value < mix) {
//                    if (displayError)
//                        showError(
//                            itemView.resources.getString(
//                                R.string.action_parameter_min_value_error,
//                                mix.toString()
//                            )
//                        )
//                    return false
//                }
//            }
//
//            getMax()?.let { max ->
//                if (value > max) {
//                    if (displayError)
//                        showError(
//                            itemView.resources.getString(
//                                R.string.action_parameter_max_value_error,
//                                max.toString()
//                            )
//                        )
//                    return false
//                }
//            }
//        }
//
//        return true
        if (isMandatory() && input.text.toString().trim().isEmpty()) {
            if (displayError)
                showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }

        input.text.toString().toFloatOrNull()?.let { value ->

            getMin()?.let { mix ->
                if (value < mix) {
                    if (displayError)
                        showError(
                            itemView.resources.getString(
                                R.string.action_parameter_min_value_error,
                                mix.toString()
                            )
                        )
                    return false
                }
            }

            getMax()?.let { max ->
                if (value > max) {
                    if (displayError)
                        showError(
                            itemView.resources.getString(
                                R.string.action_parameter_max_value_error,
                                max.toString()
                            )
                        )
                    return false
                }
            }
        }
        return true
    }

    fun showError(text: String) {
        container.error = text
        input.startAnimation(shakeAnimation)
    }
}