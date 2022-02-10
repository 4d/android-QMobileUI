/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_KEYBOARD
import com.google.android.material.timepicker.TimeFormat
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionParameterEnum

@Suppress("MagicNumber")
class TimeViewHolder(
    itemView: View,
    format: String,
    private val fragmentManager: FragmentManager?,
    hideKeyboardCallback: () -> Unit
) : BaseInputLessViewHolder(itemView, hideKeyboardCallback) {

    companion object {
        private const val dayFactor = 60 * 60 * 24
        private const val hourFactor = 60 * 60
        private const val secondFactor = 60
    }

    private val isDuration = format == ActionParameterEnum.TIME_DURATION.format

    override fun bind(
        item: Any,
        currentEntity: EntityModel?,
        preset: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, preset, onValueChanged)

        container.endIconDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.calendar_clock)

        val clockFormat = if (isDuration) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

        var defaultHour = 12
        var defaultMinute = 30
        input.text.toString().split(":").forEachIndexed { index, s ->
            when (index) {
                0 -> s.toIntOrNull()?.takeIf { it in 0..23 }?.let { defaultHour = it }
                1 -> s.substringBefore(" ").toIntOrNull()?.takeIf { it in 0..59 }
                    ?.let { defaultMinute = it }
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
            input.setText(getFormattedString(Triple(0, hour, minute)))
            onValueChanged(
                parameterName,
                convertToSeconds(picker.hour, picker.minute),
                null,
                validate(false)
            )
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

    private fun convertToSeconds(hour: Int, minute: Int): Int =
        hour * hourFactor + minute * secondFactor

    override fun formatToDisplay(input: String): String {
        convertToDaysHoursAndMinutes(input.toIntOrNull())?.let {
            return getFormattedString(it)
        }
        return ""
    }

    // Returns a triple of days, hours and minutes
    private fun convertToDaysHoursAndMinutes(seconds: Int?): Triple<Int, Int, Int>? {
        if (seconds == null) return null
        val days = seconds / dayFactor
        val daysSecond = days * dayFactor
        val hour = (seconds - daysSecond) / hourFactor
        val hoursSecond = hour * hourFactor
        val minute = (seconds - daysSecond - hoursSecond) / secondFactor
        return Triple(days, hour, minute)
    }

    private fun getFormattedString(daysHoursAndMinutes: Triple<Int, Int, Int>): String {
        val days = daysHoursAndMinutes.first
        val hours = daysHoursAndMinutes.second
        val minutes = daysHoursAndMinutes.third
        return if (isDuration)
            getDurationString(days, hours, minutes)
        else
            getTimeString(hours, minutes)
    }

    private fun getTimeString(hours: Int, minutes: Int): String = when {
        hours >= 12 -> "${hours - 12}:${getMinutes(minutes)} PM"
        else -> "$hours:${getMinutes(minutes)} AM"
    }

    private fun getDurationString(days: Int, hours: Int, minutes: Int): String = when {
        days > 0 -> "$days ${getDayWord(days)} $hours ${getHourWord(hours)} ${getMinutes(minutes)} ${
        getMinuteWord(
            minutes
        )
        }"
        hours > 0 -> "$hours ${getHourWord(hours)} ${getMinutes(minutes)} ${getMinuteWord(minutes)}"
        else -> "${getMinutes(minutes)} ${getMinuteWord(minutes)}"
    }

    private fun getDayWord(days: Int): String = if (days <= 1) "day" else "days"
    private fun getHourWord(hours: Int): String = if (hours <= 1) "hour" else "hours"
    private fun getMinuteWord(minutes: Int): String = if (minutes <= 1) "minute" else "minutes"
    private fun getMinutes(minutes: Int): String = if (minutes < 10) "0$minutes" else "$minutes"
}
