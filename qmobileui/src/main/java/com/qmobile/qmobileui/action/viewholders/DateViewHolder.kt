/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.view.View
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileui.action.ActionParameterEnum
import com.qmobile.qmobileui.formatters.FormatterUtils
import java.util.Calendar

class DateViewHolder(
    itemView: View,
    format: String,
    private val fragmentManager: FragmentManager?,
    hideKeyboardCallback: () -> Unit
) : BaseDateTimeViewHolder(itemView, hideKeyboardCallback) {

    private var dateFormat: String = when (format) {
        ActionParameterEnum.DATE_DEFAULT2.format,
        ActionParameterEnum.DATE_DEFAULT1.format -> "mediumDate"
        ActionParameterEnum.DATE_LONG.format -> "longDate"
        ActionParameterEnum.DATE_SHORT.format -> "shortDate"
        ActionParameterEnum.DATE_FULL.format -> "fullDate"
        else -> "shortDate"
    }

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntityJsonObject, onValueChanged)

        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(container.hint)
                .setInputMode(MaterialDatePicker.INPUT_MODE_TEXT)
                .build()

        val calendar = Calendar.getInstance()

        datePicker.addOnPositiveButtonClickListener {
            calendar.timeInMillis = it

            val dateToSubmit = calendar.get(Calendar.DAY_OF_MONTH).toString() + "!" + (
                calendar.get(
                    Calendar.MONTH
                ) + 1
                ) + "!" + calendar.get(Calendar.YEAR)
            val formattedDate = FormatterUtils.applyFormat(
                dateFormat,
                dateToSubmit
            )
            input.setText(formattedDate)
            onValueChanged(parameterName, dateToSubmit, "simpleDate", validate(false))
        }

        configureInputLayout(fragmentManager, datePicker)
    }
}
