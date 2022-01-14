/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.view.View
import androidx.fragment.app.FragmentManager
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_KEYBOARD
import com.google.android.material.timepicker.TimeFormat
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileui.action.ActionParameterEnum

class TimeViewHolder(
    itemView: View,
    format: String,
    private val fragmentManager: FragmentManager?,
    hideKeyboardCallback: () -> Unit
) : BaseDateTimeViewHolder(itemView, hideKeyboardCallback) {

    private val isDuration = format == ActionParameterEnum.TIME_DURATION.format

    @Suppress("MagicNumber")
    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntityJsonObject, onValueChanged)

        val clockFormat = if (isDuration) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

        var defaultHour = 12
        var defaultMinute = 30
        input.text.toString().split(":").forEachIndexed { index, s ->
            when (index) {
                0 -> s.toIntOrNull()?.takeIf { it in 0..23 }?.let { defaultHour = it }
                1 -> s.toIntOrNull()?.takeIf { it in 0..59 }?.let { defaultMinute = it }
            }
        }

        val picker =
            MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setHour(defaultHour)
                .setMinute(defaultMinute)
                .setTitleText(container.hint)
                .setInputMode(INPUT_MODE_KEYBOARD)
                .build()

        picker.addOnPositiveButtonClickListener {
            val hour = picker.hour
            val minute = picker.minute
            val formattedResult: String = when {
                isDuration -> "$hour hours $minute minutes"
                hour >= 12 -> "${hour - 12}:$minute PM"
                else -> "$hour:$minute AM"
            }
            input.setText(formattedResult)
            val numberOfSeconds = picker.hour * 60 * 60 + picker.minute * 60
            onValueChanged(parameterName, numberOfSeconds, null, validate(false))
        }

        configureInputLayout(fragmentManager, picker)
    }
}
