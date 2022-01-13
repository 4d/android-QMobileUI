/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.app.DatePickerDialog
import android.view.View
import android.widget.TextView
import com.qmobile.qmobileapi.model.entity.EntityHelper
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionParameterEnum
import com.qmobile.qmobileui.formatters.FormatterUtils
import org.json.JSONObject

const val SELECTED_YEAR = 2000
const val SELECTED_MONTH = 5
const val SELECTED_DAY = 10

class DateViewHolder(itemView: View, format: String, hideKeyboardCallback: () -> Unit): BaseViewHolder(itemView, hideKeyboardCallback) {
    private val selectedDate: TextView = itemView.findViewById(R.id.selectedDate)

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
        itemJsonObject.getSafeString("placeholder")?.let {
            selectedDate.hint = it
        }
        itemJsonObject.getSafeString("default")?.let {
            selectedDate.text = it
        }
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                val dateToSubmit = dayOfMonth.toString() + "!" + (monthOfYear + 1) + "!" + year
                val formattedDate = FormatterUtils.applyFormat(
                    dateFormat,
                    dateToSubmit
                )
                selectedDate.text = formattedDate
                onValueChanged(parameterName, dateToSubmit, "simpleDate", validate(false))
            }

        val datePickerDialog = DatePickerDialog(
            itemView.context,
            android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
            dateSetListener, SELECTED_YEAR, SELECTED_MONTH, SELECTED_DAY
        )
        itemView.setOnClickListener {
            datePickerDialog.show()
        }

        selectedDate.setOnClickListener {
            datePickerDialog.show()
        }
        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged) {
            selectedDate.text = it.toString()
        }

    }

    override fun validate(displayError: Boolean): Boolean {
        if (isMandatory() && selectedDate.text.trim().isEmpty()) {
//            showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }
        return true
    }

    override fun getInputType(format: String): Int {
        return -1
    }
}