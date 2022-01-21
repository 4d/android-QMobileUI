/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.text.InputType
import android.view.View
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeInt
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionParameterEnum

class NumberViewHolder(
    itemView: View,
    private val format: String,
    hideKeyboardCallback: () -> Unit
) : TextViewHolder(itemView, format, hideKeyboardCallback) {

    override fun getInputType(format: String): Int {
        return when (format) {
            ActionParameterEnum.NUMBER_INTEGER.format,
            ActionParameterEnum.NUMBER_SPELL_OUT.format -> {
                InputType.TYPE_CLASS_NUMBER
            }
            else -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
    }

    @Suppress("ReturnCount")
    override fun validate(displayError: Boolean): Boolean {

        val stringValue = when (format) {
            ActionParameterEnum.NUMBER_PERCENTAGE.format -> input.text.toString().removeSuffix("%")
            ActionParameterEnum.NUMBER_SPELL_OUT.format -> numberValueForSpellOut
            else -> input.text.toString()
        }

        if (isMandatory() && stringValue.trim().isEmpty()) {
            if (displayError)
                showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }

        if (stringValue.isNotEmpty() && stringValue.toDoubleOrNull() == null) {
            if (displayError)
                showError("Field value must be a number")
            return false
        }

        stringValue.toDoubleOrNull()?.let { doubleValue ->
            return isInRange(doubleValue, displayError)
        }
        return true
    }

    private fun getMin(): Int {
        itemJsonObject.getSafeArray("rules")?.let { jsonArray ->
            for (i in 0 until jsonArray.length()) {
                jsonArray.getSafeObject(i)?.getSafeInt("min")?.let { return it }
            }
        }
        return Int.MIN_VALUE
    }

    private fun getMax(): Int {
        itemJsonObject.getSafeArray("rules")?.let { jsonArray ->
            for (i in 0 until jsonArray.length()) {
                jsonArray.getSafeObject(i)?.getSafeInt("max")?.let { return it }
            }
        }
        return Int.MAX_VALUE
    }

    @Suppress("ReturnCount")
    private fun isInRange(doubleValue: Double, displayError: Boolean): Boolean {
        if (doubleValue < getMin()) {
            if (displayError)
                showError(
                    itemView.resources.getString(
                        R.string.action_parameter_min_value_error,
                        getMin().toString()
                    )
                )
            return false
        }

        if (doubleValue > getMax()) {
            if (displayError)
                showError(
                    itemView.resources.getString(
                        R.string.action_parameter_max_value_error,
                        getMax().toString()
                    )
                )
            return false
        }
        return true
    }
}
