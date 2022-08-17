/*
 * Created by qmarciset on 11/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_KEYBOARD
import com.google.android.material.timepicker.TimeFormat
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.formatters.TimeFormat.convertToSeconds
import com.qmobile.qmobileui.formatters.TimeFormat.getShortAMPMTimeFromSeconds
import com.qmobile.qmobileui.formatters.TimeFormat.secondsToVerboseDuration

@Suppress("MagicNumber")
class TimeViewHolder(
    itemView: View,
    format: String,
    private val fragmentManager: FragmentManager?
) : BaseInputLessViewHolder(itemView, format) {

    private val isDuration = apu.isDuration()

    override fun bind(
        item: Any,
        currentEntity: RoomEntity?,
        isLastParameter: Boolean,
        alreadyFilledValue: Any?,
        serverError: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, isLastParameter, alreadyFilledValue, serverError, onValueChanged)

        container.endIconDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.schedule)

        val clockFormat = if (isDuration) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

        var defaultHour = 12
        var defaultMinute = 30
        input.text.toString().split(":").forEachIndexed { index, s ->
            when (index) {
                0 -> s.toIntOrNull()?.takeIf { it in 0..23 }?.let { defaultHour = it }
                1 -> s.substringBefore(" ").toIntOrNull()?.takeIf { it in 0..59 }?.let { defaultMinute = it }
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
            val totalSeconds = convertToSeconds(picker.hour, picker.minute).toLong()
            input.setText(getFormattedString(totalSeconds, isDuration))
            onValueChanged(parameterName, totalSeconds, null, validate(false))
        }

        onValueChanged(
            parameterName,
            convertToSeconds(defaultHour, defaultMinute),
            null,
            validate(false)
        )

        onEndIconClick {
            fragmentManager?.let {
                val dialog = picker.dialog
                if (dialog == null || !dialog.isShowing) {
                    picker.show(it, picker.toString())
                }
            }
        }
    }

    override fun formatToDisplay(input: String): String {
        return input.toLongOrNull()?.let {
            getFormattedString(it, isDuration)
        } ?: ""
    }

    private fun getFormattedString(totalSeconds: Long, isDuration: Boolean): String {
        return if (isDuration) {
            secondsToVerboseDuration(totalSeconds)
        } else {
            getShortAMPMTimeFromSeconds(totalSeconds)
        }
    }
}