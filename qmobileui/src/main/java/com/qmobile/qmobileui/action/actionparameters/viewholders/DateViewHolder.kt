/*
 * Created by qmarciset on 11/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.formatters.DateFormat
import com.qmobile.qmobileui.formatters.FormatterUtils
import org.json.JSONObject
import java.util.*

class DateViewHolder(
    itemView: View,
    format: String,
    private val fragmentManager: FragmentManager?
) : BaseInputLessViewHolder(itemView, format) {

    private var dateFormat: String = when {
        apu.isDateDefault1() || apu.isDateDefault2() -> "mediumDate"
        apu.isLongDate() -> "longDate"
        apu.isShortDate() -> "shortDate"
        apu.isFullDate() -> "fullDate"
        else -> "shortDate"
    }

    private var initialPickerDate = -1L

    override fun bind(
        item: JSONObject,
        currentEntity: RoomEntity?,
        isLastParameter: Boolean,
        alreadyFilledValue: Any?,
        serverError: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, isLastParameter, alreadyFilledValue, serverError, onValueChanged)

        container.endIconDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.calendar_month)

        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setSelection(if (initialPickerDate == -1L) Calendar.getInstance().timeInMillis else initialPickerDate)
                .setTitleText(container.hint)
                .setInputMode(MaterialDatePicker.INPUT_MODE_TEXT)
                .build()

        datePicker.addOnPositiveButtonClickListener {
            initialPickerDate = it

            val dateToSubmit = getDateToSubmit()
            input.setText(formatToDisplay(dateToSubmit))
            onValueChanged(parameterName, dateToSubmit, "simpleDate", validate(false))
        }

        onValueChanged(
            parameterName,
            getDateToSubmit(),
            null,
            validate(false)
        )

        setOnSingleClickListener {
            fragmentManager?.let {
                val dialog = datePicker.dialog
                if (dialog == null || !dialog.isShowing) {
                    datePicker.show(it, datePicker.toString())
                }
            }
        }
    }

    private fun getDateToSubmit(): String {
        if (initialPickerDate == -1L) {
            return DateFormat.nullDate
        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = initialPickerDate
        return calendar.get(Calendar.DAY_OF_MONTH).toString() + "!" + (
            calendar.get(
                Calendar.MONTH
            ) + 1
            ) + "!" + calendar.get(Calendar.YEAR)
    }

    private fun updatePickerDate(newDate: String) {
        DateFormat.getDateFromString(newDate)?.let { cal ->
            val clearedTZ = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH))
                set(Calendar.MONTH, cal.get(Calendar.MONTH))
                set(Calendar.YEAR, cal.get(Calendar.YEAR))
            }
            initialPickerDate = clearedTZ.timeInMillis
        } ?: run {
            initialPickerDate = -1L
        }
    }

    override fun formatToDisplay(input: String): String =
        FormatterUtils.applyFormat(dateFormat, input)

    override fun fill(value: Any) {
        if (value == JSONObject.NULL) {
            val string = DateFormat.nullDate
            updatePickerDate(string)
            super.fill(string)
        }
        else {
            updatePickerDate(value.toString())
            super.fill(value)
        }
    }
}
