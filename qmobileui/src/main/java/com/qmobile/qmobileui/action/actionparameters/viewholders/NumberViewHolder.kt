/*
 * Created by qmarciset on 11/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.text.InputType
import android.view.View
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeInt
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.utils.addSuffix
import com.qmobile.qmobileui.formatters.SpellOutFormat
import org.json.JSONObject

class NumberViewHolder(itemView: View, format: String) : BaseTextViewHolder(itemView, format) {

    companion object {
        private const val PERCENT_STRING_TO_DOUBLE = 0.01
        private const val PERCENT_DOUBLE_TO_STRING = 100
    }

    private var numberValueForSpellOut: String = ""

    override fun bind(
        item: JSONObject,
        currentEntity: RoomEntity?,
        isLastParameter: Boolean,
        alreadyFilledValue: Any?,
        serverError: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, isLastParameter, alreadyFilledValue, serverError, onValueChanged)

        if (apu.isPercentage()) {
            input.addSuffix("%")
        }
    }

    override fun onTextChanged(s: CharSequence) {
        when {
            apu.isSpellOut() -> {
                sendIntValueForSpellOut(s.toString())
            }
            else -> onValueChanged(parameterName, getValueToSend(), null, validate(false))
        }
    }

    private fun sendIntValueForSpellOut(input: String) {
        val intValue = input.toIntOrNull()
        if (intValue != null) {
            numberValueForSpellOut = intValue.toString()
            onValueChanged(parameterName, numberValueForSpellOut, null, validate(false))
        }
    }

    override fun getInputType(format: String): Int {
        return when {
            apu.isInteger() || apu.isSpellOut() -> InputType.TYPE_CLASS_NUMBER
            else -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
    }

    @Suppress("ReturnCount")
    override fun validate(displayError: Boolean): Boolean {
        val stringValue = when {
            apu.isPercentage() -> input.text.toString().removeSuffix(" %")
            apu.isSpellOut() -> numberValueForSpellOut
            else -> input.text.toString()
        }

        if (isMandatory() && stringValue.trim().isEmpty()) {
            if (displayError) {
                showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            }
            return false
        }

        if (input.text.toString().trim().isEmpty()) {
            return true
        }

        if (stringValue.isNotEmpty() && stringValue.toDoubleOrNull() == null) {
            if (displayError) {
                showError(itemView.context.resources.getString(R.string.action_parameter_number_error))
            }
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
            if (displayError) {
                showError(
                    itemView.context.resources.getString(
                        R.string.action_parameter_min_value_error,
                        getMin().toString()
                    )
                )
            }
            return false
        }

        if (doubleValue > getMax()) {
            if (displayError) {
                showError(
                    itemView.context.resources.getString(
                        R.string.action_parameter_max_value_error,
                        getMax().toString()
                    )
                )
            }
            return false
        }
        return true
    }

    override fun formattedValue(input: String): String {
        return when {
            apu.isScientific() -> input.toDoubleOrNull()?.toString() ?: input
            apu.isSpellOut() -> {
                sendIntValueForSpellOut(input)
                SpellOutFormat.convertNumberToWord(input)
            }
            apu.isPercentage() -> getPercentageToDisplay(input)
            else -> input
        }
    }

    override fun unformattedValue(input: String): String {
        return when {
            apu.isSpellOut() -> numberValueForSpellOut.ifEmpty { "" }
            else -> input
        }
    }

    override fun getValueToSend(): String {
        return when {
            apu.isSpellOut() -> numberValueForSpellOut
            apu.isPercentage() -> getPercentageToSend(input.text.toString())
            apu.isScientific() -> input.text.toString().toDoubleOrNull()?.toString() ?: ""
            else -> input.text.toString()
        }
    }

    private fun getPercentageToDisplay(input: String): String {
        return if (input.contains("%")) {
            input
        } else {
            input.toDoubleOrNull()?.let {
                (it * PERCENT_DOUBLE_TO_STRING).toInt().toString()
            } ?: ""
        }
    }

    private fun getPercentageToSend(input: String): String {
        return if (input.contains("%")) {
            input.removeSuffix(" %").toDoubleOrNull()?.let {
                (it * PERCENT_STRING_TO_DOUBLE).toString()
            } ?: ""
        } else {
            input
        }
    }
}
