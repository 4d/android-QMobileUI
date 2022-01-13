/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.text.InputType
import android.view.View
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionParameterEnum

class NumberViewHolder(itemView: View, format: String, hideKeyboardCallback: () -> Unit): TextViewHolder(itemView, format, hideKeyboardCallback) {

    override fun getInputType(format: String): Int {
        return when (format) {
            ActionParameterEnum.NUMBER_DEFAULT1.format,
            ActionParameterEnum.NUMBER_DEFAULT1.format
            ->
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            ActionParameterEnum.NUMBER_INTEGER.format -> {
                InputType.TYPE_CLASS_NUMBER
            }
            else -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
    }

    override fun validate(displayError: Boolean): Boolean {
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
}
