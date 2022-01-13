/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.app.TimePickerDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import com.qmobile.qmobileapi.model.entity.EntityHelper
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R
import org.json.JSONObject
import java.text.DecimalFormat

class TimeViewHolder(itemView: View, format: String, hideKeyboardCallback: () -> Unit): BaseViewHolder(itemView, hideKeyboardCallback) {

    companion object {
        const val AM_KEY = "AM"
        const val PM_KEY = "PM"
        const val SELECTED_HOUR = 12
        const val SELECTED_MINUTE = 30
    }

    private var selectedTime: TextView = itemView.findViewById(R.id.selectedTime)
    private var selectedHour = SELECTED_HOUR
    private val selectedMinute = SELECTED_MINUTE
    private val is24HourFormat = format == "duration"

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntityJsonObject, onValueChanged)

        itemJsonObject.getSafeString("placeholder")?.let {
            selectedTime.hint = it
        }

        itemJsonObject.getSafeString("default")?.let {
            selectedTime.text = it
        }


        super.bind(item, currentEntityJsonObject, onValueChanged)

        val timeSetListener =
            TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                selectedHour = hourOfDay

                val formattedResult: String = if (is24HourFormat) {
                    "$selectedHour hours $minute minutes"
                } else {
                    if (selectedHour >= 12) {
                        "${selectedHour - 12}:$minute $PM_KEY"
                    } else {
                        "$selectedHour:$minute $AM_KEY"
                    }
                }
                selectedTime.text = formattedResult
                val numberOfSeconds = selectedHour * 60 * 60 + minute * 60
                onValueChanged(parameterName, numberOfSeconds, null, validate(false))
            }

        val timePickerDialog = TimePickerDialog(
            itemView.context,
            android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            timeSetListener,
            selectedHour, selectedMinute,
            is24HourFormat
        )

        itemView.setOnClickListener {
            timePickerDialog.show()
        }

        selectedTime.setOnClickListener {
            timePickerDialog.show()
        }

        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged) {
            selectedTime.text = it.toString()
        }
    }

    override fun validate(displayError: Boolean): Boolean {
        if (isMandatory() && selectedTime.text.trim().isEmpty()) {
            if (displayError)
//                showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }

        return true
    }

    override fun getInputType(format: String): Int {
        return -1
    }
}