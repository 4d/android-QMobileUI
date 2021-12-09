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
import android.util.Patterns
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeInt
import com.qmobile.qmobileapi.utils.getSafeObject

import com.qmobile.qmobileui.formatters.FormatterUtils
import com.qmobile.qmobileui.list.SpellOutHelper
import java.text.DecimalFormat

abstract class ActionParameterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    lateinit var itemJsonObject: JSONObject
    var label: TextView = itemView.findViewById(R.id.label)
    var errorlabel: TextView = itemView.findViewById(R.id.error_label)
    lateinit var parameterName: String

    open fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        itemJsonObject = item as JSONObject
        parameterName = itemJsonObject.getString("name")
        if (isMandatory()) {
            "$parameterName *".also { label.text = it }
        } else {
            label.text = parameterName
        }
    }

    abstract fun validate(): Boolean
    fun isMandatory(): Boolean {
        return itemJsonObject.getSafeArray("rules")?.toString()?.contains("mandatory") ?: false
    }

    fun getMin(): Int? {
        itemJsonObject.getSafeArray("rules")?.let { jsonArray ->
            for (i in 0 until jsonArray.length()) {

                val rule = jsonArray.getSafeObject(i)
                rule?.getSafeInt("min")?.let {
                    return it
                }
            }
        }
        return null
    }

    fun getMax(): Int? {
        itemJsonObject.getSafeArray("rules")?.let { jsonArray ->
            for (i in 0 until jsonArray.length()) {

                val rule = jsonArray.getSafeObject(i)
                rule?.getSafeInt("max")?.let {
                    return it
                }
            }
        }
        return null
    }

    fun showError(text: String) {
        errorlabel.visibility = View.VISIBLE
        errorlabel.setText(text)
    }

    fun dismissErrorIfNeeded() {
        errorlabel.visibility = View.GONE
    }
}

/**
 * TEXT VIEW HOLDERS
 */
@Suppress("ComplexMethod", "LongMethod", "MagicNumber", "ReturnCount")
class TextViewHolder(itemView: View, val format: String) :
    ActionParameterViewHolder(itemView) {
    var editText: TextView = itemView.findViewById(R.id.editText)

    @Suppress("MaxLineLength")
    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        super.bind(item, onValueChanged)
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

        if (editText.text.toString().isEmpty()) {
            itemJsonObject.getSafeString("default")?.let {
                editText.text = it
            }
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

    override fun validate(): Boolean {
        if (isMandatory() && editText.text.trim().isEmpty()) {
            showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }

        if (isMandatory() && (format == ActionParameterEnum.TEXT_EMAIL.format)) {
            val isValidEmail = Patterns.EMAIL_ADDRESS.matcher(editText.text).matches()
            if (!isValidEmail) {
                showError(itemView.context.resources.getString(R.string.action_parameter_invalid_email_error))
                return false
            }
        }

        if (isMandatory() && (format == ActionParameterEnum.TEXT_URL.format)) {
            val isValidUrl = Patterns.WEB_URL.matcher(editText.text).matches();
            if (!isValidUrl) {
                showError(itemView.context.resources.getString(R.string.action_parameter_invalid_url_error))
                return false
            }
        }

        dismissErrorIfNeeded()
        return true
    }


}

@Suppress("ComplexMethod", "LongMethod", "MagicNumber","ReturnCount")
class TextAreaViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var editText: TextView = itemView.findViewById(R.id.editText)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        super.bind(item, onValueChanged)
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

    override fun validate(): Boolean {
        if (isMandatory() && editText.text.trim().isEmpty()) {
            showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }
        dismissErrorIfNeeded()
        return true
    }
}

/**
 * Number VIEW HOLDERS
 */

@Suppress("ComplexMethod", "LongMethod", "MagicNumber","ReturnCount")
class NumberViewHolder(itemView: View, val format: String) :
    ActionParameterViewHolder(itemView) {
    private var editText: TextView = itemView.findViewById(R.id.editText)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        super.bind(item, onValueChanged)
        editText.hint = itemJsonObject.getSafeString("placeholder")

        itemJsonObject.getSafeString("default")?.let {
            editText.text = it
        }
        editText.inputType = when (format) {
            ActionParameterEnum.NUMBER_DEFAULT1.format,
            ActionParameterEnum.NUMBER_DEFAULT1.format
            ->
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

    override fun validate(): Boolean {
        if (isMandatory() && editText.text.trim().isEmpty()) {
            showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }

        editText.text.toString().toFloatOrNull()?.let { value ->

            getMin()?.let { mix ->
                if (value < mix) {
                    showError(
                        itemView.resources.getString(
                            R.string.action_parameter_min_value_error,
                            mix.toString()
                        )
                    )
                    return false
                }
            }

            getMax()?.let { max ->
                if (value > max) {
                    showError(
                        itemView.resources.getString(
                            R.string.action_parameter_max_value_error,
                            max.toString()
                        )
                    )
                    return false
                }
            }
        }

        dismissErrorIfNeeded()
        return true
    }
}

@Suppress("ComplexMethod", "LongMethod", "MagicNumber","ReturnCount")
class SpellOutViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var editText: TextView = itemView.findViewById(R.id.editText)
    var numericValue: Long? = null

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        super.bind(item, onValueChanged)
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

    override fun validate(): Boolean {
        if (isMandatory() && editText.text.trim().isEmpty()) {
            showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }

        numericValue.toString().toFloatOrNull()?.let { value ->

            getMin()?.let { mix ->
                if (value < mix) {
                    showError(
                        itemView.resources.getString(
                            R.string.action_parameter_min_value_error,
                            mix.toString()
                        )
                    )
                    return false
                }
            }

            getMax()?.let { max ->
                if (value > max) {
                    showError(
                        itemView.resources.getString(
                            R.string.action_parameter_max_value_error,
                            max.toString()
                        )
                    )
                    return false
                }
            }
        }
        dismissErrorIfNeeded()
        return true
    }
}

@Suppress("ComplexMethod", "LongMethod", "MagicNumber","ReturnCount")
class ScientificViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var editText: TextView = itemView.findViewById(R.id.editText)
    var numericValue: Float? = null

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        super.bind(item, onValueChanged)
        itemJsonObject.getSafeString("default")?.let {
            editText.text = it
        }
        editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

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
                    it.toString().toFloatOrNull()?.let { it1 ->
                        numericValue = it1
                    }
                }
            }
        })
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

                    val numFormat = DecimalFormat("0.#####E0")
                    editText.text = numFormat.format(numericValue)

                }
            }
        }
    }

    override fun validate(): Boolean {
        if (isMandatory() && editText.text.trim().isEmpty()) {
            showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }
        numericValue.toString().toFloatOrNull()?.let { value ->
            getMin()?.let { mix ->
                if (value < mix) {
                    showError(
                        itemView.resources.getString(
                            R.string.action_parameter_min_value_error,
                            mix.toString()
                        )
                    )
                    return false
                }
            }
            getMax()?.let { max ->
                if (value > max) {
                    showError(
                        itemView.resources.getString(
                            R.string.action_parameter_max_value_error,
                            max.toString()
                        )
                    )
                    return false
                }
            }
        }

        dismissErrorIfNeeded()
        return true
    }
}

const val PERCENT_KEY = "%"
const val PERCENT_MULTIPLIER = 0.01F

@Suppress("ComplexMethod", "LongMethod", "MagicNumber", "ReturnCount")
class PercentageViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var editText: TextView = itemView.findViewById(R.id.editText)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        super.bind(item, onValueChanged)

        editText.hint = itemJsonObject.getSafeString("placeholder")
        itemJsonObject.getSafeString("default")?.let {
            editText.text = it
        }

        (editText as EditText).addSuffix(PERCENT_KEY)
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        //editText.text = PERCENT_KEY
        Selection.setSelection(editText.text as Spannable?, editText.text.length - 1)

        editText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                s.toString().replace(PERCENT_KEY, "").let {
                    if (it.isNotEmpty()) {
                        val percentValue =
                            "%.2f".format(it.toFloatOrNull()?.times(PERCENT_MULTIPLIER))
                        if (percentValue != null) {
                            onValueChanged(
                                parameterName,
                                percentValue,
                                null
                            )
                        }
                    }
                }
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

    override fun validate(): Boolean {
        val textWithoutPercent = editText.text.toString().replace(PERCENT_KEY, "").trim()
        if (isMandatory() && textWithoutPercent.isEmpty()) {
            showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }

        textWithoutPercent.toFloatOrNull()?.let { value ->
            getMin()?.let { mix ->
                if (value < mix) {
                    showError(
                        itemView.resources.getString(
                            R.string.action_parameter_min_value_error,
                            mix.toString()
                        )
                    )
                    return false
                }
            }

            getMax()?.let { max ->
                if (value > max) {
                    showError(
                        itemView.resources.getString(
                            R.string.action_parameter_max_value_error,
                            max.toString()
                        )
                    )
                    return false
                }
            }
        }

        dismissErrorIfNeeded()
        return true
    }
}

/**
 * BOOLEAN VIEW HOLDERS
 */

@Suppress("ComplexMethod", "LongMethod", "MagicNumber", "ReturnCount")
class BooleanSwitchViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var switch: Switch = itemView.findViewById(R.id.switchButton)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        super.bind(item, onValueChanged)
        switch.setOnCheckedChangeListener { _, checked ->
            onValueChanged(parameterName, checked, null)
        }
    }

    override fun validate(): Boolean {
        return true
    }
}

@Suppress("ComplexMethod", "LongMethod", "MagicNumber", "ReturnCount")

class BooleanCheckMarkViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    private var checkBox: CheckBox = itemView.findViewById(R.id.checkbox)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        super.bind(item, onValueChanged)
        checkBox.setOnCheckedChangeListener { _, b ->
            onValueChanged(parameterName, b, null)
        }
    }

    override fun validate(): Boolean {
        return true
    }
}


/**
 * IMAGE VIEW HOLDERS
 */
class ImageViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        super.bind(item, onValueChanged)
    }

    override fun validate(): Boolean {
        return true
    }
}

/**
 * TIME VIEW HOLDERS
 */
const val AM_KEY = "AM"
const val PM_KEY = "PM"
const val SELECTED_HOUR = 12
const val SELECTED_MINUTE = 30

@Suppress("ComplexMethod", "LongMethod", "MagicNumber", "ReturnCount")
class TimeViewHolder(itemView: View, val format: String) :
    ActionParameterViewHolder(itemView) {
    private var selectedTime: TextView = itemView.findViewById(R.id.selectedTime)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        super.bind(item, onValueChanged)

        var selectedHour = SELECTED_HOUR
        val selectedMinute = SELECTED_MINUTE
        val is24HourFormat = format == "duration"

        itemJsonObject.getSafeString("default")?.let {
            selectedTime.text = it
        }
        val timeSetListener =
            TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                selectedHour = hourOfDay

                val formattedResult: String = if (is24HourFormat) {
                    "$selectedHour hours $minute minutes"
                } else {
                    if (selectedHour >= 12) {
                        "${selectedHour - 12}:$minute $PM_KEY"
                    } else {
                        "$selectedHour:$minute $AM_KEY"
                    }
                }
                selectedTime.text = formattedResult
                val numberOfSeconds = selectedHour * 60 * 60 + minute * 60
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

    override fun validate(): Boolean {
        if (isMandatory() && selectedTime.text.trim().isEmpty()) {
            showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }

        dismissErrorIfNeeded()
        return true
    }
}

/**
 * DATE VIEW HOLDERS
 */

const val SELECTED_YEAR = 2000
const val SELECTED_MONTH = 5
const val SELECTED_DAY = 10

@Suppress("ComplexMethod", "LongMethod", "MagicNumber", "ReturnCount")
class DateViewHolder(itemView: View, val format: String) :
    ActionParameterViewHolder(itemView) {
    private val selectedDate: TextView = itemView.findViewById<TextView>(R.id.selectedDate)

    override fun bind(item: Any, onValueChanged: (String, Any, String?) -> Unit) {
        super.bind(item, onValueChanged)
        itemJsonObject.getSafeString("default")?.let {
            selectedDate.text = it
        }
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

    override fun validate(): Boolean {
        if (isMandatory() && selectedDate.text.trim().isEmpty()) {
            showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }

        dismissErrorIfNeeded()
        return true
    }

}


