package com.qmobile.qmobileui.action

import android.app.TimePickerDialog
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.qmobile.qmobileui.R
import org.json.JSONObject
import com.qmobile.qmobileapi.utils.getSafeString
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.text.Spannable
import android.text.TextWatcher
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView

import com.qmobile.qmobileui.formatters.FormatterUtils
import com.qmobile.qmobileui.list.SpellOutHelper

abstract class ActionParameterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit)
}

/**
 * TEXT VIEW HOLDERS
 */
@Suppress("ComplexMethod", "LongMethod", "MagicNumber")
class TextViewHolder(itemView: View, val format: String) :
    ActionParameterViewHolder(itemView) {
    var label: TextView = itemView.findViewById(R.id.label)
    var editText: TextView = itemView.findViewById(R.id.editText)

    @Suppress("MaxLineLength")
    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        val itemJsonObject = item as JSONObject
        val parameterName = itemJsonObject.getString("name")
        label.text = parameterName
        editText.inputType = when (format) {
            ActionParameterEnum.TEXT_DEFAULT.format -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            ActionParameterEnum.TEXT_ZIP.format,
            ActionParameterEnum.TEXT_PHONE.format -> InputType.TYPE_CLASS_NUMBER
            ActionParameterEnum.TEXT_EMAIL.format -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            ActionParameterEnum.TEXT_URL.format -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            ActionParameterEnum.TEXT_PASSWORD.format -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            ActionParameterEnum.TEXT_ACCOUNT.format -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            ActionParameterEnum.TEXT_AREA.format -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            else ->
                return
        }

        editText.hint = itemJsonObject.getSafeString("placeholder")

        itemJsonObject.getSafeString("default")?.let {
            editText.text = it
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Nothing to do
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nothing to do
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { onValueChanged(parameterName, s.toString(), null) }
            }
        })
    }

}

@Suppress("ComplexMethod", "LongMethod", "MagicNumber")
class TextAreaViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var label: TextView = itemView.findViewById(R.id.label)
    var editText: TextView = itemView.findViewById(R.id.editText)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        val itemJsonObject = item as JSONObject
        val parameterName = itemJsonObject.getString("name")

        label.text = parameterName
        editText.hint = itemJsonObject.getString("placeholder")
        itemJsonObject.getSafeString("default")?.let {
            editText.text = it
        }
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Nothing to do
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nothing to do
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { onValueChanged(parameterName, s.toString(), null) }
            }
        })
    }
}


/**
 * Number VIEW HOLDERS
 */

@Suppress("ComplexMethod", "LongMethod", "MagicNumber")
class NumberViewHolder(itemView: View, val format: String) :
    ActionParameterViewHolder(itemView) {
    var label: TextView = itemView.findViewById(R.id.label)
    private var editText: TextView = itemView.findViewById(R.id.editText)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        val itemJsonObject = item as JSONObject
        val parameterName = itemJsonObject.getString("name")
        label.text = parameterName
        editText.hint = itemJsonObject.getString("placeholder")

        itemJsonObject.getSafeString("default")?.let {
            editText.text = it
        }
        editText.inputType = when (format) {
            ActionParameterEnum.NUMBER_DEFAULT1.format,
            ActionParameterEnum.NUMBER_DEFAULT1.format,
            ActionParameterEnum.NUMBER_SCIENTIFIC.format ->
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            ActionParameterEnum.NUMBER_INTEGER.format -> {
                InputType.TYPE_CLASS_NUMBER
            }
            else -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Nothing to do
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nothing to do
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    it.toString().toFloatOrNull()?.let { it1 ->
                        onValueChanged(
                            parameterName,
                            it1,
                            null
                        )
                    }
                }
            }
        })

    }
}

@Suppress("ComplexMethod", "LongMethod", "MagicNumber")
class SpellOutViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var label: TextView = itemView.findViewById(R.id.label)
    var editText: TextView = itemView.findViewById(R.id.editText)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        val itemJsonObject = item as JSONObject
        val parameterName = itemJsonObject.getString("name")
        var numericValue: Long? = null
        label.text = parameterName
        itemJsonObject.getSafeString("default")?.let {
            editText.text = it
        }
        editText.hint = itemJsonObject.getSafeString("placeholder")
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Nothing to do
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nothing to do
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    it.toString().toLongOrNull()?.let { it1 ->
                        numericValue = it1
                    }
                }
            }
        })
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.setOnFocusChangeListener { view, hasFocus ->
            if (editText.text.isEmpty())
                return@setOnFocusChangeListener

            if (hasFocus && (numericValue != null)) {
                editText.text = numericValue.toString()

            } else {

                numericValue?.let {
                    onValueChanged(
                        parameterName,
                        it,
                        null
                    )

                    SpellOutHelper.convert(it).apply {
                        editText.text = this
                    }

                }
            }
        }
    }
}

const val PERCENT_KEY = "%"

@Suppress("ComplexMethod", "LongMethod", "MagicNumber")
class PercentageViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var label: TextView = itemView.findViewById(R.id.label)
    var editText: TextView = itemView.findViewById(R.id.editText)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        val itemJsonObject = item as JSONObject
        val parameterName = itemJsonObject.getString("name")
        label.text = parameterName
        editText.hint = itemJsonObject.getSafeString("placeholder")
        itemJsonObject.getSafeString("default")?.let {
            editText.text = it
        }
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.text = PERCENT_KEY
        Selection.setSelection(editText.text as Spannable?, 0)

        editText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                onValueChanged(parameterName, s.toString().replace(PERCENT_KEY, "").toInt(), null)
            }

            override fun beforeTextChanged(
                s: CharSequence, start: Int, count: Int,
                after: Int
            ) {
                // Nothing to do
            }

            override fun afterTextChanged(s: Editable) {
                // Nothing to do
            }
        })
    }
}

/**
 * BOOLEAN VIEW HOLDERS
 */

@Suppress("ComplexMethod", "LongMethod", "MagicNumber")
class BooleanSwitchViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var label: TextView = itemView.findViewById(R.id.label)
    var switch: Switch = itemView.findViewById(R.id.switchButton)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        val itemJsonObject = item as JSONObject
        val parameterName = itemJsonObject.getString("name")
        label.text = parameterName
        switch.setOnCheckedChangeListener { _, checked ->
            onValueChanged(parameterName, checked, null)
        }
    }
}

@Suppress("ComplexMethod", "LongMethod", "MagicNumber")

class BooleanCheckMarkViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var label: TextView = itemView.findViewById(R.id.label)
    var checkBox: CheckBox = itemView.findViewById(R.id.checkbox)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        val itemJsonObject = item as JSONObject
        val parameterName = itemJsonObject.getString("name")
        label.text = parameterName
        checkBox.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, b ->
            onValueChanged(parameterName, b, null)
        })
    }
}


/**
 * IMAGE VIEW HOLDERS
 */
class ImageViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var label: TextView = itemView.findViewById(R.id.label)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        val itemJsonObject = item as JSONObject
        val parameterName = itemJsonObject.getString("name")
        label.text = parameterName
    }

}


/**
 * VIEW TIME HOLDERS
 */
const val AM_KEY = "AM"
const val PM_KEY = "PM"
const val SELECTED_HOUR = 12
const val SELECTED_MINUTE = 30

@Suppress("ComplexMethod", "LongMethod", "MagicNumber")
class TimeViewHolder(itemView: View, val format: String) :
    ActionParameterViewHolder(itemView) {
    var label: TextView = itemView.findViewById(R.id.label)
    private var selectedTime: TextView = itemView.findViewById(R.id.selectedTime)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        var selectedHour = SELECTED_HOUR
        var selectedMinute = SELECTED_MINUTE
        var is24HourFormat = format == "duration"

        val itemJsonObject = item as JSONObject
        val parameterName = itemJsonObject.getString("name")
        itemJsonObject.getSafeString("default")?.let {
            selectedTime.text = it
        }
        label.text = parameterName

        val timeSetListener =
            TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                selectedHour = hourOfDay

                var formattedResult: String
                if (is24HourFormat) {
                    formattedResult = "$selectedHour hours $minute minutes"
                } else {
                    formattedResult = if (selectedHour >= 12) {
                        "${selectedHour - 12}:$minute $PM_KEY"
                    } else {
                        "$selectedHour:$minute $AM_KEY"
                    }
                }
                selectedTime.text = formattedResult
                var numberOfSeconds = selectedHour * 60 * 60 + minute * 60
                onValueChanged(parameterName, numberOfSeconds, null)
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
    }
}

/**
 * DATE VIEW HOLDERS
 */

const val SELECTED_YEAR = 2000
const val SELECTED_MONTH = 5
const val SELECTED_DAY = 10

@Suppress("ComplexMethod", "LongMethod", "MagicNumber")
class DateViewHolder(itemView: View, val format: String) :
    ActionParameterViewHolder(itemView) {
    val label: TextView = itemView.findViewById(R.id.label)
    val selectedDate: TextView = itemView.findViewById<TextView>(R.id.selectedDate)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        val itemJsonObject = item as JSONObject
        val parameterName = itemJsonObject.getString("name")
        itemJsonObject.getSafeString("default")?.let {
            selectedDate.text = it
        }
        label.text = parameterName
        val dateSetListener =
            OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                val format = when (format) {
                    ActionParameterEnum.DATE_DEFAULT2.format,
                    ActionParameterEnum.DATE_DEFAULT1.format -> "mediumDate"
                    ActionParameterEnum.DATE_LONG.format -> "longDate"
                    ActionParameterEnum.DATE_SHORT.format -> "shortDate"
                    ActionParameterEnum.DATE_FULL.format -> "fullDate"
                    else -> "shortDate"
                }

                val dateToSubmit = dayOfMonth.toString() + "!" + (monthOfYear + 1) + "!" + year
                val formattedDate = FormatterUtils.applyFormat(
                    format,
                    dateToSubmit
                )
                selectedDate.text = formattedDate
                onValueChanged(parameterName, dateToSubmit, "simpleDate")
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
    }
}


