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
import com.qmobile.qmobileui.formatters.FormatterUtils
import java.util.Calendar

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

    private val calendar = Calendar.getInstance()

    override fun bind(
        item: Any,
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
                .setTitleText(container.hint)
                .setInputMode(MaterialDatePicker.INPUT_MODE_TEXT)
                .build()

        datePicker.addOnPositiveButtonClickListener {
            calendar.timeInMillis = it

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

        onEndIconClick {
            fragmentManager?.let {
                val dialog = datePicker.dialog
                if (dialog == null || !dialog.isShowing) {
                    datePicker.show(it, datePicker.toString())
                }
            }
        }
    }

    private fun getDateToSubmit(): String =
        calendar.get(Calendar.DAY_OF_MONTH).toString() + "!" + (
            calendar.get(
                Calendar.MONTH
            ) + 1
            ) + "!" + calendar.get(Calendar.YEAR)

    override fun formatToDisplay(input: String): String =
        FormatterUtils.applyFormat(dateFormat, input)
}
