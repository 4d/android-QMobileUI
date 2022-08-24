/*
 * Created by qmarciset on 24/8/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.utils

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileui.R

class DurationPicker : DialogFragment() {

    private lateinit var timePickerLayout: View
    private lateinit var hourPicker: NumberPicker
    private lateinit var minPicker: NumberPicker
    private lateinit var secPicker: NumberPicker

    var title: CharSequence = ""
    private var onPositiveCallback: (hour: Int, minute: Int, second: Int) -> Unit = { _, _, _ -> }

    private var onNegativeCallback: () -> Unit = {}

    private var defaultHour: Int = 0
    private var defaultMinute: Int = 0
    private var defaultSecond: Int = 0

    companion object {
        private const val MIN_VALUE_HOUR = 0
        private const val MIN_VALUE_MINUTE = 0
        private const val MIN_VALUE_SECOND = 0
        private const val MAX_VALUE_HOUR = 99
        private const val MAX_VALUE_MINUTE = 59
        private const val MAX_VALUE_SECOND = 59
    }

    /**
     * Default value is true.
     * If set to false the hour picker is not
     * visible in the Dialog
     */
    private var includeHours: Boolean = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            timePickerLayout =
                requireActivity().layoutInflater.inflate(R.layout.duration_picker, null)

            setupTimePickerLayout()

            val builder = MaterialAlertDialogBuilder(it)
                .setView(timePickerLayout)
                .setTitle(title)
                .setPositiveButton(it.getString(R.string.action_time_dialog_positive)) { _, _ ->
                    var hour = hourPicker.value
                    if (!includeHours) {
                        hour = 0
                    }
                    onPositiveCallback(hour, minPicker.value, secPicker.value)
                }
                .setNegativeButton(it.getString(R.string.action_time_dialog_negative)) { _, _ ->
                    onNegativeCallback
                }
                .create()
            builder
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    fun addOnPositiveButtonClickListener(onTimeSet: (hour: Int, minute: Int, second: Int) -> Unit) {
        this.onPositiveCallback = onTimeSet
    }

    fun addOnNegativeButtonClickListener(onCancelOption: () -> Unit) {
        this.onNegativeCallback = onCancelOption
    }

    fun setupDefault(hour: Int, minute: Int, second: Int) {
        defaultHour = hour
        defaultMinute = minute
        defaultSecond = second
    }

    private fun setupTimePickerLayout() {
        bindViews()

        setupMaxValues()
        setupMinValues()
        setupInitialValues()

        if (!includeHours) {
            timePickerLayout.findViewById<LinearLayout>(R.id.hours_container).visibility = View.GONE
        }
    }

    private fun bindViews() {
        hourPicker = timePickerLayout.findViewById(R.id.hours)
        minPicker = timePickerLayout.findViewById(R.id.minutes)
        secPicker = timePickerLayout.findViewById(R.id.seconds)
    }

    private fun setupMaxValues() {
        hourPicker.maxValue = MAX_VALUE_HOUR
        minPicker.maxValue = MAX_VALUE_MINUTE
        secPicker.maxValue = MAX_VALUE_SECOND
    }

    private fun setupMinValues() {
        hourPicker.minValue = MIN_VALUE_HOUR
        minPicker.minValue = MIN_VALUE_MINUTE
        secPicker.minValue = MIN_VALUE_SECOND
    }

    private fun setupInitialValues() {
        hourPicker.value = defaultHour
        minPicker.value = defaultMinute
        secPicker.value = defaultSecond
    }
}
