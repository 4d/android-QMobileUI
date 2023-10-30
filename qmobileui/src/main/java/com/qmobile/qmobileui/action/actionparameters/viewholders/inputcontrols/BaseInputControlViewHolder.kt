/*
 * Created by qmarciset on 28/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols

import android.view.View
import android.widget.EditText
import androidx.fragment.app.FragmentManager
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import com.qmobile.qmobileapi.utils.getSafeAny
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobiledatasync.utils.fieldAdjustment
import com.qmobile.qmobileui.action.actionparameters.ActionParametersFragment
import com.qmobile.qmobileui.action.inputcontrols.InputControl
import com.qmobile.qmobileui.action.inputcontrols.InputControl.getImageName
import com.qmobile.qmobileui.action.inputcontrols.InputControlDataHandler
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.binding.px
import com.qmobile.qmobileui.utils.ReflectionUtils
import com.qmobile.qmobileui.utils.getParentFragment
import org.json.JSONObject
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

interface BaseInputControlViewHolder : InputControlDataHandler {

    companion object {
        const val NO_VALUE_PLACEHOLDER = " - "
        const val NO_VALUE_PLACEHOLDER_KEY = "qmobile_no_value_placeholder"
        const val INPUT_LESS_ICON_SIZE = 20
    }

    val fragMng: FragmentManager?
    var placeHolder: String
    val circularProgressBar: CircularProgressIndicator?
    var currentEditEntityValue: Any?
    val fieldValueMap: Map<Int, Any?>
    val displayTextMap: Map<Int, String>
    var onValueChanged: (String, Any?, String?, Boolean) -> Unit
    var parameterName: String
    var itemJsonObject: JSONObject

    // Triggered everytime we get data from observable
    fun setupValues(items: LinkedList<Any>, field: String? = null, entityFormat: String? = null)

    fun validate(displayError: Boolean): Boolean

    override fun FieldMapping.prepareStaticData(isMandatory: Boolean) {
        handleMandatory(this.getChoiceList(), isMandatory) { items ->
            setupValues(items)
        }
    }

    override fun FieldMapping.prepareDataSource(isMandatory: Boolean) {
        val fragment = fragMng?.getParentFragment()
        if (fragment is ActionParametersFragment) {
            val setupValuesOnce = AtomicBoolean(false)
            circularProgressBar?.visibility = View.VISIBLE
            super.handleDataSource(
                fragment = fragment,
                fieldMapping = this,
                callback = { entitiesMap, field, entityFormat, _ ->
                    if (!setupValuesOnce.getAndSet(true)) {
                        handleMandatory(entitiesMap, isMandatory) { items ->
                            setupValues(items, field, entityFormat)
                        }
                    }
                }
            )
        }
    }

    fun retrieveFieldMapping(): FieldMapping? {
        val inputControlName = itemJsonObject.getSafeString("source")?.removePrefix("/")
        return BaseApp.runtimeDataHolder.inputControls.find { it.name == inputControlName }
    }

    @Suppress("ComplexCondition")
    fun setTextOrIcon(container: TextInputLayout, view: EditText, text: String?) {
        if (fieldMapping?.binding == "imageNamed" &&
            text != placeHolder &&
            text != NO_VALUE_PLACEHOLDER &&
            text != null
        ) {
            fieldMapping?.getImageName(text)?.let { imageName ->
                container.startIconDrawable =
                    ImageHelper.getDrawableFromString(
                        container.context,
                        imageName,
                        INPUT_LESS_ICON_SIZE.px,
                        INPUT_LESS_ICON_SIZE.px
                    )
                container.setStartIconTintList(null)
                view.text = null
            }
        } else {
            container.startIconDrawable = null
            view.setText(text)
        }
    }

    fun getText(
        item: Any,
        index: Int,
        isMandatory: Boolean,
        field: String?,
        entityFormat: String?,
        callback: (displayText: String, fieldValue: Any?) -> Unit
    ) {
        when (item) {
            is RoomEntity -> {
                field?.let {
                    val fieldValue = ReflectionUtils.getInstanceProperty(item, field.fieldAdjustment())
                    val displayText: String = InputControl.applyEntityFormat(item, entityFormat).ifEmpty {
                        fieldValue?.toString() ?: ""
                    }
                    callback(displayText, fieldValue)
                }
            }
            is String -> {
                if (item == NO_VALUE_PLACEHOLDER_KEY) {
                    callback(NO_VALUE_PLACEHOLDER, null)
                } else {
                    if (isMandatory || this is CustomViewViewHolder) {
                        callback(item, index.toString())
                    } else {
                        // the null placeholder get the position 0
                        callback(item, (index - 1).toString())
                    }
                }
            }
            is Pair<*, *> -> {
                if (item.first == NO_VALUE_PLACEHOLDER_KEY) {
                    callback(NO_VALUE_PLACEHOLDER, null)
                } else {
                    item.second?.let {
                        callback(it.toString(), item.first)
                    }
                }
            }
        }
    }

    fun isInputValid(text: String): Boolean =
        text.trim().isEmpty() || text == placeHolder || text == NO_VALUE_PLACEHOLDER

    fun handleDefaultField(bindingAdapterPosition: Int, foundPositionInMapCallback: (position: Int) -> Unit) {
        val fieldValue: Any? = getFieldValue(bindingAdapterPosition, "", "")
        val targetValue = fieldValue ?: currentEditEntityValue
        if (targetValue != null) {
            for ((position, value) in fieldValueMap) {
                if (value == targetValue) {
                    foundPositionInMapCallback(position)
                    onValueChanged(parameterName, fieldValueMap[position], null, validate(false))
                    return
                }
            }
        }

        if (this !is CustomViewViewHolder) {
            val displayText: String = getDisplayText(bindingAdapterPosition, "", "")
            if (displayText == NO_VALUE_PLACEHOLDER) {
                foundPositionInMapCallback(0)
            } else {
                foundPositionInMapCallback(-1)
            }
        }
        // Done only if no fieldValue sent or not found in map
        onValueChanged(parameterName, null, null, validate(false))
    }

    private fun getDisplayText(position: Int, currentText: String, placeHolder: String): String =
        itemJsonObject.getSafeString(ActionParametersFragment.INPUT_CONTROL_DISPLAY_TEXT_INJECT_KEY + "_$position")
            ?: currentText.ifEmpty { placeHolder }

    private fun getFieldValue(position: Int, currentText: String, placeHolder: String): Any? {
        val injectedJsonValue =
            itemJsonObject.getSafeAny(ActionParametersFragment.INPUT_CONTROL_FIELD_VALUE_INJECT_KEY + "_$position")
        return when {
            injectedJsonValue != null -> InputControl.getTypedValue(itemJsonObject, injectedJsonValue)
            currentText != placeHolder -> currentText
            else -> null
        }
    }
}
