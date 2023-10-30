/*
 * Created by qmarciset on 28/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols

import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.actionparameters.viewholders.BaseViewHolder
import com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols.BaseInputControlViewHolder.Companion.NO_VALUE_PLACEHOLDER
import com.qmobile.qmobileui.action.inputcontrols.InputControl
import com.qmobile.qmobileui.action.inputcontrols.InputControl.getImageName
import com.qmobile.qmobileui.action.inputcontrols.InputControlFormatHolder
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.binding.px
import com.qmobile.qmobileui.formatters.ImageNamed
import org.json.JSONObject
import java.util.LinkedList

class MenuViewHolder(
    itemView: View,
    fragmentManager: FragmentManager?,
    private val formatHolderCallback: (holder: InputControlFormatHolder, position: Int) -> Unit,
    format: String = ""
) : BaseViewHolder(itemView, format), BaseInputControlViewHolder {

    override val fragMng: FragmentManager? = fragmentManager
    override var fieldMapping: FieldMapping? = null
    override var placeHolder = itemView.context.resources.getString(R.string.input_control_menu_baseline)
    override var currentEditEntityValue: Any? = null
    override val circularProgressBar: CircularProgressIndicator? = null
    override val fieldValueMap = mutableMapOf<Int, Any?>()
    override val displayTextMap = mutableMapOf<Int, String>()

    private val container: TextInputLayout = itemView.findViewById(R.id.container)
    private val autoCompleteTextView: MaterialAutoCompleteTextView = itemView.findViewById(R.id.autoCompleteTv)
    private var arrayAdapter: ArrayAdapter<String>? = null

    override fun bind(
        item: JSONObject,
        currentEntity: RoomEntity?,
        isLastParameter: Boolean,
        alreadyFilledValue: Any?,
        serverError: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, isLastParameter, alreadyFilledValue, serverError, onValueChanged)

        fieldMapping = retrieveFieldMapping()

        itemJsonObject.getSafeString("label")?.let { parameterLabel ->
            container.hint = if (isMandatory()) {
                "$parameterLabel *"
            } else {
                parameterLabel
            }
        }

        container.isExpandedHintEnabled = false

        alreadyFilledValue?.let {
            fill(it)
        } ?: kotlin.run {
            getDefaultFieldValue(currentEntity, itemJsonObject) {
                fill(it)
            }
        }

        setupInputControlData(isMandatory())

        autoCompleteTextView.setOnItemClickListener { _, _, i, _ ->
            container.error = null
            onItemSelected(i)
            setTextOrIcon(container, autoCompleteTextView, displayTextMap[i])
            initAdapter()
        }

        showServerError()
    }

    private fun onItemSelected(position: Int) {
        onValueChanged(parameterName, fieldValueMap[position], null, validate(false))
        displayTextMap[position]?.let { displayText ->
            val inputControlFormatHolder = InputControlFormatHolder(displayText, fieldValueMap[position])
            formatHolderCallback(inputControlFormatHolder, bindingAdapterPosition)
        }
    }

    override fun setupValues(items: LinkedList<Any>, field: String?, entityFormat: String?) {
        items.forEachIndexed { index, entry ->
            getText(entry, index, isMandatory(), field, entityFormat) { displayText, fieldValue ->
                displayTextMap[index] = displayText
                fieldValueMap[index] = InputControl.getTypedValue(itemJsonObject, fieldValue)
            }
        }

        handleDefaultField(bindingAdapterPosition) { position ->
            if (position == -1) {
                autoCompleteTextView.setText(placeHolder)
            } else {
                setTextOrIcon(container, autoCompleteTextView, displayTextMap[position])
            }
        }

        initAdapter()
    }

    private fun initAdapter() {
        if (fieldMapping?.binding == "imageNamed") {
            arrayAdapter = getAdapter(displayTextMap.values.toTypedArray())
            autoCompleteTextView.setAdapter(arrayAdapter)
        } else {
            autoCompleteTextView.setSimpleItems(displayTextMap.values.toTypedArray())
        }
    }

    private fun getAdapter(items: Array<String>): ArrayAdapter<String> {
        return object :
            ArrayAdapter<String>(itemView.context, R.layout.material_select_dialog_item, android.R.id.text1, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val itemView = super.getView(position, convertView, parent)
                val textView = itemView.findViewById<View>(android.R.id.text1) as TextView

                val text = items[position]
                if (text != NO_VALUE_PLACEHOLDER) {
                    fieldMapping?.getImageName(text)?.let { imageName ->
                        ImageNamed.setDrawable(textView, imageName, fieldMapping?.imageWidth, fieldMapping?.imageHeight)
                    }
                    textView.text = null
                    // Add margin between image and text (support various screen densities)
                    val padding = ImageHelper.ICON_MARGIN.px
                    textView.compoundDrawablePadding = padding
                    textView.setPadding(padding, 0, 0, 0)
                } else {
                    textView.setCompoundDrawables(null, null, null, null)
                    textView.text = text
                    textView.setPadding(ImageHelper.NO_ICON_PADDING.px, 0, 0, 0)
                }
                return itemView
            }
        }
    }

    override fun validate(displayError: Boolean): Boolean {
        if (isMandatory() && isInputValid(autoCompleteTextView.text.toString())) {
            if (displayError) {
                showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            }
            return false
        }
        return true
    }

    override fun getInputType(format: String): Int {
        return -1
    }

    override fun showError(text: String) {
        container.error = text
    }

    override fun fill(value: Any) {
        if (value.toString().isNotEmpty()) {
            currentEditEntityValue = value
        }
    }
}
