@file:Suppress("TooGenericExceptionCaught", "SwallowedException", "UnusedPrivateMember")

package com.qmobile.qmobileui.action

import android.app.Activity
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.text.Spannable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.github.gcacace.signaturepad.views.SignaturePad
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qmobile.qmobileapi.model.entity.EntityHelper
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.model.entity.Photo
import com.qmobile.qmobileapi.utils.getSafeArray
import com.qmobile.qmobileapi.utils.getSafeInt
import com.qmobile.qmobileapi.utils.getSafeObject
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.binding.bindImageFromUrl
import com.qmobile.qmobileui.formatters.FormatterUtils
import com.qmobile.qmobileui.list.SpellOutHelper
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.DecimalFormat

abstract class ActionParameterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    lateinit var itemJsonObject: JSONObject
    var label: TextView = itemView.findViewById(R.id.label)
    private var errorLabel: TextView = itemView.findViewById(R.id.error_label)
    lateinit var parameterName: String

    open fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        alreadyFilledValue: Any?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit,
        goToScanner: ((Int) -> Unit)?,
        goToCamera: ((Intent, Int, String) -> Unit)?,
        onSigned: ((String, Uri?) -> Unit)?
    ) {
        itemJsonObject = item as JSONObject
        parameterName = itemJsonObject.getSafeString("name") ?: ""
        val parameterLabel = itemJsonObject.getSafeString("label") ?: ""
        if (isMandatory()) {
            "$parameterLabel *".also { label.text = it }
            onValueChanged(parameterName, "", null, validate())
        } else {
            label.text = parameterLabel
        }
    }

    abstract fun setDefaultFieldIfNeeded(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        onValueChanged: (String, Any, String?, isValid: Boolean) -> Unit
    )

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
        errorLabel.visibility = View.VISIBLE
        errorLabel.setText(text)
    }

    fun dismissErrorIfNeeded() {
        errorLabel.visibility = View.GONE
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
    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        alreadFilledValue: Any?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit,
        goToScanner: ((Int) -> Unit)?,
        goToCamera: ((Intent, Int, String) -> Unit)?,
        onSigned: ((String, Uri?) -> Unit)?
    ) {
        super.bind(
            item,
            currentEntityJsonObject,
            alreadFilledValue,
            onValueChanged,
            null,
            null,
            null
        )
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
                s?.let { onValueChanged(parameterName, s.toString(), null, validate()) }
            }
        })

        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged)

        editText.handleDarkMode()

        alreadFilledValue?.let {
            if (!(it as String).isNullOrEmpty()) {
                editText.text = it
            }
        }
    }

    override fun setDefaultFieldIfNeeded(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        currentEntity?.let {
            val defaultField = itemJsonObject.getSafeString("defaultField")
            if (defaultField != null) {
                EntityHelper.readInstanceProperty<String>(it, defaultField).also { value ->
                    editText.text = value
                    onValueChanged(parameterName, value, null, validate())
                }
            }
        }
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
            val isValidUrl = Patterns.WEB_URL.matcher(editText.text).matches()
            if (!isValidUrl) {
                showError(itemView.context.resources.getString(R.string.action_parameter_invalid_url_error))
                return false
            }
        }
        dismissErrorIfNeeded()
        return true
    }
}

@Suppress("ComplexMethod", "LongMethod", "MagicNumber", "ReturnCount")
class TextAreaViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var editText: TextView = itemView.findViewById(R.id.editText)

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        alreadFilledValue: Any?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit,
        goToScanner: ((Int) -> Unit)?,
        goToCamera: ((Intent, Int, String) -> Unit)?,
        onSigned: ((String, Uri?) -> Unit)?
    ) {
        super.bind(
            item,
            currentEntityJsonObject,
            alreadFilledValue,
            onValueChanged,
            null,
            null,
            null
        )
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
                s?.let { onValueChanged(parameterName, s.toString(), null, validate()) }
            }
        })
        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged)
        editText.handleDarkMode()

        alreadFilledValue?.let {
            if (!(it as String).isNullOrEmpty()) {
                editText.text = it
            }
        }
    }

    override fun validate(): Boolean {
        if (isMandatory() && editText.text.trim().isEmpty()) {
            showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }
        dismissErrorIfNeeded()
        return true
    }

    override fun setDefaultFieldIfNeeded(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        currentEntity?.let {
            val defaultField = itemJsonObject.getSafeString("defaultField")
            if (defaultField != null) {
                EntityHelper.readInstanceProperty<String>(it, defaultField).also { value ->
                    editText.text = value
                    onValueChanged(parameterName, value, null, validate())
                }
            }
        }
    }
}

/**
 * Number VIEW HOLDERS
 */

@Suppress("ComplexMethod", "LongMethod", "MagicNumber", "ReturnCount")
class NumberViewHolder(itemView: View, val format: String) :
    ActionParameterViewHolder(itemView) {
    private var editText: TextView = itemView.findViewById(R.id.editText)

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        alreadFilledValue: Any?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit,
        goToScanner: ((Int) -> Unit)?,
        goToCamera: ((Intent, Int, String) -> Unit)?,
        onSigned: ((String, Uri?) -> Unit)?
    ) {
        super.bind(
            item,
            currentEntityJsonObject,
            alreadFilledValue,
            onValueChanged,
            null,
            null,
            null
        )
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
                            null,
                            validate()
                        )
                    }
                }
            }
        })
        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged)
        editText.handleDarkMode()
        alreadFilledValue?.let {
            if (!(it as String).isNullOrEmpty()) {
                editText.text = it
            }
        }
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

    override fun setDefaultFieldIfNeeded(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        currentEntity?.let {
            val defaultField = itemJsonObject.getSafeString("defaultField")
            if (defaultField != null) {
                EntityHelper.readInstanceProperty<Float>(it, defaultField)
                    .also { value ->
                        val isInteger = (value - value.toInt()) == 0.0F
                        val formattedValue = if (isInteger) {
                            value.toInt()
                        } else {
                            value
                        }
                        editText.text = formattedValue.toString()
                        onValueChanged(
                            parameterName,
                            formattedValue,
                            null,
                            validate()
                        )
                    }
            }
        }
    }
}

@Suppress("ComplexMethod", "LongMethod", "MagicNumber", "ReturnCount")
class SpellOutViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var editText: TextView = itemView.findViewById(R.id.editText)
    var numericValue: Long? = null

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        alreadFilledValue: Any?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit,
        goToScanner: ((Int) -> Unit)?,
        goToCamera: ((Intent, Int, String) -> Unit)?,
        onSigned: ((String, Uri?) -> Unit)?
    ) {
        super.bind(
            item,
            currentEntityJsonObject,
            alreadFilledValue,
            onValueChanged,
            null,
            null,
            null
        )
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
                        null,
                        validate()
                    )
                    SpellOutHelper.convert(it).apply {
                        editText.text = this
                    }
                }
            }
        }
        editText.handleDarkMode()
        alreadFilledValue?.let {
            if (!(it as String).isNullOrEmpty()) {
                editText.text = it
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

    override fun setDefaultFieldIfNeeded(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        currentEntity?.let {
            val defaultField = itemJsonObject.getSafeString("defaultField")
            if (defaultField != null) {
                EntityHelper.readInstanceProperty<String>(it, defaultField).also { value ->
                    editText.text = value
                    onValueChanged(
                        parameterName,
                        value,
                        null,
                        validate()
                    )
                }
            }
        }
    }
}

@Suppress("ComplexMethod", "LongMethod", "MagicNumber", "ReturnCount")
class ScientificViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var editText: TextView = itemView.findViewById(R.id.editText)
    var numericValue: Float? = null

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        alreadFilledValue: Any?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit,
        goToScanner: ((Int) -> Unit)?,
        goToCamera: ((Intent, Int, String) -> Unit)?,
        onSigned: ((String, Uri?) -> Unit)?
    ) {
        super.bind(
            item,
            currentEntityJsonObject,
            alreadFilledValue,
            onValueChanged,
            null,
            null,
            null
        )
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
                        null,
                        validate()
                    )

                    val numFormat = DecimalFormat("0.#####E0")
                    editText.text = numFormat.format(numericValue)
                }
            }
        }
        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged)
        editText.handleDarkMode()
        alreadFilledValue?.let {
            if (!(it as String).isNullOrEmpty()) {
                editText.text = it
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

    override fun setDefaultFieldIfNeeded(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        currentEntity?.let {
            val defaultField = itemJsonObject.getSafeString("defaultField")
            if (defaultField != null) {
                EntityHelper.readInstanceProperty<String>(it, defaultField).also { value ->
                    editText.text = value
                    onValueChanged(
                        parameterName,
                        it,
                        null,
                        validate()
                    )
                }
            }
        }
    }
}

const val PERCENT_KEY = "%"
const val PERCENT_MULTIPLIER = 0.01F

@Suppress("ComplexMethod", "LongMethod", "MagicNumber", "ReturnCount")
class PercentageViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var editText: TextView = itemView.findViewById(R.id.editText)

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        alreadFilledValue: Any?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit,
        goToScanner: ((Int) -> Unit)?,
        goToCamera: ((Intent, Int, String) -> Unit)?,
        onSigned: ((String, Uri?) -> Unit)?
    ) {
        super.bind(
            item,
            currentEntityJsonObject,
            alreadFilledValue,
            onValueChanged,
            null,
            null,
            null
        )

        editText.hint = itemJsonObject.getSafeString("placeholder")
        itemJsonObject.getSafeString("default")?.let {
            editText.text = it
        }

        (editText as EditText).addSuffix(PERCENT_KEY)
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        // editText.text = PERCENT_KEY
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
                                null,
                                validate(),
                            )
                        }
                    }
                }
            }

            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
                // Nothing to do
            }

            override fun afterTextChanged(s: Editable) {
                // Nothing to do
            }
        })

        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged)
        editText.handleDarkMode()
        alreadFilledValue?.let {
            if (!(it as String).isNullOrEmpty()) {
                editText.text = it
            }
        }
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

    override fun setDefaultFieldIfNeeded(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        currentEntity?.let {
            val defaultField = itemJsonObject.getSafeString("defaultField")
            if (defaultField != null) {
                EntityHelper.readInstanceProperty<String>(it, defaultField).also { value ->
                    editText.text = EntityHelper.readInstanceProperty<String>(value, defaultField)
                    onValueChanged(
                        parameterName,
                        value,
                        null,
                        validate()
                    )
                }
            }
        }
    }
}

/**
 * BOOLEAN VIEW HOLDERS
 */

@Suppress("ComplexMethod", "LongMethod", "MagicNumber", "ReturnCount")
class BooleanSwitchViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var switch: Switch = itemView.findViewById(R.id.switchButton)

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        alreadFilledValue: Any?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit,
        goToScanner: ((Int) -> Unit)?,
        goToCamera: ((Intent, Int, String) -> Unit)?,
        onSigned: ((String, Uri?) -> Unit)?
    ) {
        super.bind(
            item,
            currentEntityJsonObject,
            alreadFilledValue,
            onValueChanged,
            null,
            null,
            null
        )
        switch.setOnCheckedChangeListener { _, checked ->
            onValueChanged(parameterName, checked, null, true)
        }
        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged)
        alreadFilledValue?.let {
            switch.isChecked = it as Boolean
        }
    }

    override fun validate(): Boolean {
        return true
    }

    override fun setDefaultFieldIfNeeded(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        currentEntity?.let {
            val defaultField = itemJsonObject.getSafeString("defaultField")
            if (defaultField != null) {
                EntityHelper.readInstanceProperty<Boolean>(it, defaultField).also { value ->
                    switch.isChecked = value
                    onValueChanged(parameterName, value, null, true)
                }
            }
        }
    }
}

@Suppress("ComplexMethod", "LongMethod", "MagicNumber", "ReturnCount")

class BooleanCheckMarkViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    private var checkBox: CheckBox = itemView.findViewById(R.id.checkbox)

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        alreadFilledValue: Any?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit,
        goToScanner: ((Int) -> Unit)?,
        goToCamera: ((Intent, Int, String) -> Unit)?,
        onSigned: ((String, Uri?) -> Unit)?
    ) {
        super.bind(
            item,
            currentEntityJsonObject,
            alreadFilledValue,
            onValueChanged,
            null,
            null,
            null
        )
        checkBox.setOnCheckedChangeListener { _, b ->
            onValueChanged(parameterName, b, null, true)
        }
        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged)
        alreadFilledValue?.let {
            checkBox.isChecked = it as Boolean
        }
    }

    override fun validate(): Boolean {
        return true
    }

    override fun setDefaultFieldIfNeeded(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        currentEntity?.let {
            val defaultField = itemJsonObject.getSafeString("defaultField")
            if (defaultField != null) {
                checkBox.isChecked = EntityHelper.readInstanceProperty(it, defaultField)
                onValueChanged(
                    parameterName,
                    EntityHelper.readInstanceProperty(it, defaultField),
                    null,
                    true
                )
            }
        }
    }
}

/**
 * IMAGE VIEW HOLDERS
 */

@Suppress("ComplexMethod", "LongMethod", "MagicNumber", "ReturnCount", "MaxLineLength")
class ImageViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var imageButton: ImageView = itemView.findViewById(R.id.image_button)
    var container: View = itemView.findViewById(R.id.container)

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        alreadFilledValue: Any?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit,
        goToScanner: ((Int) -> Unit)?,
        goToCamera: ((Intent, Int, String) -> Unit)?,
        onSigned: ((String, Uri?) -> Unit)?
    ) {
        super.bind(
            item,
            currentEntityJsonObject,
            alreadFilledValue,
            onValueChanged,
            null,
            goToCamera,
            null
        )
        container.setOnClickListener {
            // setup the alert builder
            val builder = MaterialAlertDialogBuilder(itemView.context)
            builder.setTitle("Choose a picture")
            val options = arrayOf("Camera", "Gallery")
            builder.setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        capturePhoto(goToCamera)
                    }
                    1 -> {
                        pickImageFromGallery()
                    }
                }
            }
            // create and show the alert dialog
            val dialog = builder.create()
            dialog.show()
        }
        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged)
        displaySelectedImageIfNeed()

        alreadFilledValue?.let {
            if (it is Uri) {
                imageButton.setImageURI(it)
            }
        }
    }

    private fun displaySelectedImageIfNeed() {
        try {
            if (itemJsonObject.get("uri") != null) {
                val uri = itemJsonObject.get("uri") as Uri
                imageButton.setImageURI(uri)
                itemJsonObject.remove("uri")
            }
        } catch (e: Exception) {
            Timber.e("ActionParameterViewHolder: ", e.localizedMessage)
        }
    }

    override fun validate(): Boolean {
        return true
    }

    override fun setDefaultFieldIfNeeded(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        currentEntity?.let {
            val defaultField = itemJsonObject.getSafeString("defaultField")
            if (defaultField != null) {
                EntityHelper.readInstanceProperty<Photo>(it, defaultField).also { value ->
                    if (value != null) {
                        bindImageFromUrl(
                            imageButton,
                            value.__deferred?.uri,
                            null,
                            null,
                            null,
                            null
                        )
                        onValueChanged(parameterName, "", null, validate())
                    }
                }
            }
        }
    }

    private fun capturePhoto(goToCamera: ((Intent, Int, String) -> Unit)?) {
        val context = itemView.context
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->

            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(context.packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile(itemView.context)
                } catch (e: IOException) {
                    Timber.e("ImageViewHolder: ", e.localizedMessage)
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val uri = FileProvider.getUriForFile(
                        context,
                        context.packageName + ".provider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    goToCamera?.let { it1 ->
                        it1(
                            takePictureIntent,
                            bindingAdapterPosition,
                            photoFile.absolutePath
                        )
                    }
                }
            }
        }
    }

    private fun pickImageFromGallery() {
        val context = itemView.context
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        (context as Activity).startActivityForResult(
            intent,
            bindingAdapterPosition // Send position as request code, so we can update image preview only for the selected item
        )
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
    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        alreadFilledValue: Any?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit,
        goToScanner: ((Int) -> Unit)?,
        goToCamera: ((Intent, Int, String) -> Unit)?,
        onSigned: ((String, Uri?) -> Unit)?
    ) {
        super.bind(
            item,
            currentEntityJsonObject,
            alreadFilledValue,
            onValueChanged,
            null,
            null,
            null
        )

        var selectedHour = SELECTED_HOUR
        val selectedMinute = SELECTED_MINUTE
        val is24HourFormat = format == "duration"

        itemJsonObject.getSafeString("placeholder")?.let {
            selectedTime.hint = it
        }

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
                onValueChanged(parameterName, numberOfSeconds, null, validate())
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
        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged)
    }

    override fun validate(): Boolean {
        if (isMandatory() && selectedTime.text.trim().isEmpty()) {
            showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }

        dismissErrorIfNeeded()
        return true
    }

    override fun setDefaultFieldIfNeeded(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        currentEntity?.let {
            val defaultField = itemJsonObject.getSafeString("defaultField")
            if (defaultField != null) {
                EntityHelper.readInstanceProperty<String>(it, defaultField).also { value ->
                    selectedTime.text = value
                    onValueChanged(parameterName, value, null, validate())
                }
            }
        }
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
        alreadFilledValue: Any?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit,
        goToScanner: ((Int) -> Unit)?,
        goToCamera: ((Intent, Int, String) -> Unit)?,
        onSigned: ((String, Uri?) -> Unit)?
    ) {
        super.bind(
            item,
            currentEntityJsonObject,
            alreadFilledValue,
            onValueChanged,
            null,
            null,
            null
        )
        itemJsonObject.getSafeString("placeholder")?.let {
            selectedDate.hint = it
        }
        itemJsonObject.getSafeString("default")?.let {
            selectedDate.text = it
        }
        val dateSetListener =
            OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                val dateToSubmit = dayOfMonth.toString() + "!" + (monthOfYear + 1) + "!" + year
                val formattedDate = FormatterUtils.applyFormat(
                    dateFormat,
                    dateToSubmit
                )
                selectedDate.text = formattedDate
                onValueChanged(parameterName, dateToSubmit, "simpleDate", validate())
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
        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged)
    }

    override fun validate(): Boolean {
        if (isMandatory() && selectedDate.text.trim().isEmpty()) {
            showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }

        dismissErrorIfNeeded()
        return true
    }

    override fun setDefaultFieldIfNeeded(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        currentEntity?.let {
            val defaultField = itemJsonObject.getSafeString("defaultField")
            if (defaultField != null) {
                EntityHelper.readInstanceProperty<String>(it, defaultField).also { value ->
                    val formattedDate = FormatterUtils.applyFormat(
                        dateFormat,
                        value
                    )
                    selectedDate.text = formattedDate
                    onValueChanged(parameterName, it, null, validate())
                }
            }
        }
    }
}

/**
 *  QR/Bar code HOLDER
 */

class BarCodeViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    var scannedValueTextView: TextView = itemView.findViewById(R.id.scanned_value_text_view)
    var container: View = itemView.findViewById(R.id.container)

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        alreadFilledValue: Any?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit,
        goToScanner: ((Int) -> Unit)?,
        goToCamera: ((Intent, Int, String) -> Unit)?,
        onSigned: ((String, Uri?) -> Unit)?
    ) {
        super.bind(
            item,
            currentEntityJsonObject,
            alreadFilledValue,
            onValueChanged,
            goToScanner,
            goToCamera,
            null
        )
        container.setOnClickListener {
            goToScanner?.let { it1 -> it1(bindingAdapterPosition) }
        }
        showScannedValueIfNeeded(onValueChanged)
    }

    override fun validate(): Boolean {
        if (isMandatory() && scannedValueTextView.text.trim().isEmpty()) {
            showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }
        return true
    }

    override fun setDefaultFieldIfNeeded(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        currentEntity?.let {
            val defaultField = itemJsonObject.getSafeString("defaultField")
            if (defaultField != null) {
                EntityHelper.readInstanceProperty<String>(it, defaultField).also { value ->
                    scannedValueTextView.text = value
                    onValueChanged(parameterName, value, null, validate())
                }
            }
        }
    }

    private fun showScannedValueIfNeeded(onValueChanged: (String, Any, String?, Boolean) -> Unit) {
        itemJsonObject.getSafeString("scanned")?.let {
            scannedValueTextView.text = it
            itemJsonObject.remove("scanned")
            onValueChanged(parameterName, it, null, validate())
        }
    }
}

/**
 *  Signature HOLDER
 */

const val MIN_ALPHA = 0.9F
const val MAX_ALPHA = 1F
const val BITMAP_QUALITY = 80
const val ORIGIN_POSITION = 0F

class SignatureViewHolder(itemView: View) :
    ActionParameterViewHolder(itemView) {
    private var signaturePad: SignaturePad? = itemView.findViewById(R.id.signature_pad)
    private var closeButton: View? = itemView.findViewById(R.id.close_button)
    private var defaultPreview: ImageView = itemView.findViewById(R.id.preview)
    var isEmpty = true

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        alreadFilledValue: Any?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit,
        goToScanner: ((Int) -> Unit)?,
        goToCamera: ((Intent, Int, String) -> Unit)?,
        onSigned: ((String, Uri?) -> Unit)?
    ) {
        super.bind(
            item,
            currentEntityJsonObject,
            alreadFilledValue,
            onValueChanged,
            goToScanner,
            goToCamera,
            null
        )
        signaturePad?.let {
            it.setOnSignedListener(object : SignaturePad.OnSignedListener {
                override fun onStartSigning() {
                    setPreviewVisibility(false)
                }

                override fun onSigned() {
                    val signatureURi = getSignatureUri(it.signatureBitmap)
                    signatureURi?.let { uri -> onSigned?.let { it2 -> it2(parameterName, uri) } }
                    isEmpty = false
                    onValueChanged(parameterName, "", null, validate())
                }

                override fun onClear() {
                    // When user signed and then cleared signature pad
                    // we should remove last signature from imagesToUpload
                    onSigned?.let { it1 -> it1(parameterName, null) }
                    isEmpty = true
                    onValueChanged(parameterName, "", null, validate())
                }
            })
        }

        closeButton?.setOnClickListener {
            signaturePad?.clear()
        }
        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged)
    }

    override fun validate(): Boolean {
        if (isMandatory() && isEmpty && signaturePad?.alpha == MAX_ALPHA) {
            showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }
        dismissErrorIfNeeded()
        return true
    }

    override fun setDefaultFieldIfNeeded(
        currentEntity: EntityModel?,
        itemJsonObject: JSONObject,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        currentEntity?.let {
            val defaultField = itemJsonObject.getSafeString("defaultField")
            if (defaultField != null) {
                EntityHelper.readInstanceProperty<Photo>(it, defaultField).also { value ->
                    if (value != null) {
                        bindImageFromUrl(
                            defaultPreview,
                            value.__deferred?.uri,
                            null,
                            null,
                            null,
                            null
                        )
                        setPreviewVisibility(true)
                        onValueChanged(parameterName, "", null, validate())
                    }
                }
            }
        }
    }

    private fun setPreviewVisibility(isVisible: Boolean) {
        if (isVisible) {
            defaultPreview.visibility = View.VISIBLE
            signaturePad?.alpha = MIN_ALPHA
        } else {
            defaultPreview.visibility = View.GONE
            signaturePad?.alpha = MAX_ALPHA
        }
    }

    private fun getSignatureUri(signature: Bitmap): Uri? {
        try {
            val photo: File? = try {
                createImageFile(itemView.context)
            } catch (e: IOException) {
                Timber.e("SignatureViewHolder: ", e.localizedMessage)
                null
            }
            saveBitmapToJPG(signature, photo)
            return Uri.fromFile(photo)
        } catch (e: IOException) {
            Timber.e("SignatureViewHolder IOException : ", e.localizedMessage)
        }
        return null
    }

    private fun saveBitmapToJPG(bitmap: Bitmap, photo: File?) {
        val newBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(newBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, ORIGIN_POSITION, ORIGIN_POSITION, null)
        val stream: OutputStream = FileOutputStream(photo)
        newBitmap.compress(Bitmap.CompressFormat.JPEG, BITMAP_QUALITY, stream)
        stream.close()
    }
}
