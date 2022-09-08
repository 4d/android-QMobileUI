/*
 * Created by qmarciset on 11/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.utils.DurationPicker
import com.qmobile.qmobileui.formatters.TimeFormat.convertToMillis
import com.qmobile.qmobileui.formatters.TimeFormat.getShortAMPMTime
import com.qmobile.qmobileui.formatters.TimeFormat.toVerboseDuration

@Suppress("MagicNumber")
class TimeViewHolder(
    itemView: View,
    format: String,
    private val fragmentManager: FragmentManager?
) : BaseInputLessViewHolder(itemView, format) {

    private val isDuration = apu.isDuration()
    private lateinit var durationPicker: DurationPicker
    private lateinit var timePicker: MaterialTimePicker

    private var defaultHour = if (isDuration) 0 else 12
    private var defaultMinute = if (isDuration) 0 else 30
    private var defaultSecond = 0

    override fun bind(
        item: Any,
        currentEntity: RoomEntity?,
        isLastParameter: Boolean,
        alreadyFilledValue: Any?,
        serverError: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(
            item,
            currentEntity,
            isLastParameter,
            alreadyFilledValue,
            serverError,
            onValueChanged
        )

        container.endIconDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.schedule)

        val clockFormat = if (isDuration) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

        if (isDuration) {
            extractDurationFromString()
        } else {
            extractTimeFromString()
        }

        if (isDuration) {
            durationPicker = DurationPicker()
            durationPicker.title = container.hint ?: ""
            durationPicker.addOnPositiveButtonClickListener { hour, minute, second ->
                onTimeSet(hour, minute, second, onValueChanged)
            }
        } else {
            timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setTitleText(container.hint)
                .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
                .setPositiveButtonText(itemView.context.getString(R.string.action_time_dialog_positive))
                .setNegativeButtonText(itemView.context.getString(R.string.action_time_dialog_negative))
                .build()

            timePicker.addOnPositiveButtonClickListener {
                onTimeSet(timePicker.hour, timePicker.minute, 0, onValueChanged)
            }
        }
        setInitialValues(defaultHour, defaultMinute, defaultSecond)

        onValueChanged(
            parameterName,
            convertToMillis(defaultHour, defaultMinute),
            null,
            validate(false)
        )

        setOnSingleClickListener {
            if (isDuration && ::durationPicker.isInitialized) {
                showPicker(durationPicker)
            }
            if (!isDuration && ::timePicker.isInitialized) {
                showPicker(timePicker)
            }
        }
    }

    private fun showPicker(picker: DialogFragment) {
        fragmentManager?.let {
            val dialog = picker.dialog
            if (dialog == null || !dialog.isShowing) {
                picker.show(it, picker.toString())
            }
        }
    }

    private fun setInitialValues(hour: Int, minute: Int, second: Int) {
        if (isDuration && ::durationPicker.isInitialized) {
            durationPicker.setupDefault(hour, minute, second)
        }
        if (!isDuration && ::timePicker.isInitialized) {
            timePicker.hour = hour
            timePicker.minute = minute
        }
    }

    private fun onTimeSet(
        hour: Int,
        minute: Int,
        second: Int,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        setInitialValues(hour, minute, second)
        val millis = convertToMillis(hour, minute, second).toLong()
        input.setText(getFormattedString(millis, isDuration))
        onValueChanged(parameterName, millis, null, validate(false))
    }

    override fun formatToDisplay(input: String): String {
        return input.toLongOrNull()?.let { millis ->
            getFormattedString(millis, isDuration)
        } ?: ""
    }

    private fun getFormattedString(millis: Long, isDuration: Boolean): String {
        return if (isDuration) {
            toVerboseDuration(millis)
        } else {
            getShortAMPMTime(millis)
        }
    }

    private fun extractTimeFromString() {
        input.text.toString().split(":").forEachIndexed { index, s ->
            when (index) {
                0 -> s.toIntOrNull()?.takeIf { it in 0..99 }?.let { defaultHour = it }
                1 -> s.substringBefore(" ").toIntOrNull()?.takeIf { it in 0..59 }
                    ?.let { defaultMinute = it }
                2 -> s.toIntOrNull()?.takeIf { it in 0..59 }?.let { defaultSecond = it }
            }
        }
    }

    @Suppress("ComplexMethod")
    private fun extractDurationFromString() {
        var text = input.text.toString()
        var days = 0
        var hours = defaultHour
        var minutes = defaultMinute
        var seconds = defaultSecond
        if (text.contains("days")) {
            days = text.substringBefore("days").trim().toIntOrNull() ?: 0
            text = text.substringAfter("days")
        }
        if (text.contains("day")) {
            days = text.substringBefore("day").trim().toIntOrNull() ?: 0
            text = text.substringAfter("day")
        }
        if (text.contains("hours")) {
            hours = text.substringBefore("hours").trim().toIntOrNull() ?: 0
            text = text.substringAfter("hours")
        }
        if (text.contains("hour")) {
            hours = text.substringBefore("hour").trim().toIntOrNull() ?: 0
            text = text.substringAfter("hour")
        }
        if (text.contains("minutes")) {
            minutes = text.substringBefore("minutes").trim().toIntOrNull() ?: 0
            text = text.substringAfter("minutes")
        }
        if (text.contains("minute")) {
            minutes = text.substringBefore("minute").trim().toIntOrNull() ?: 0
            text = text.substringAfter("minute")
        }
        if (text.contains("seconds")) {
            seconds = text.substringBefore("seconds").trim().toIntOrNull() ?: 0
        }
        if (text.contains("second")) {
            seconds = text.substringBefore("second").trim().toIntOrNull() ?: 0
        }

        defaultHour = hours + days * 24
        defaultMinute = minutes
        defaultSecond = seconds
    }
}
