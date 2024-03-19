/*
 * Created by qmarciset on 22/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols

import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.actionparameters.viewholders.BaseInputLessViewHolder
import com.qmobile.qmobileui.action.inputcontrols.InputControl
import org.json.JSONObject
import java.util.LinkedList

class PushViewHolder(
    itemView: View,
    fragmentManager: FragmentManager?,
    private val goToPushFragment: (position: Int) -> Unit,
    format: String = ""
) : BaseInputLessViewHolder(itemView, format), BaseInputControlViewHolder {

    override val fragMng: FragmentManager? = fragmentManager
    override var fieldMapping: FieldMapping? = null
    override var placeHolder = itemView.context.resources.getString(R.string.input_control_push_baseline)
    override var currentEditEntityValue: Any? = null
    override val circularProgressBar: CircularProgressIndicator? = null
    override val fieldValueMap = mutableMapOf<Int, Any?>()
    override val displayTextMap = mutableMapOf<Int, String>()

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

        container.isExpandedHintEnabled = false
        container.endIconDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.chevron_right)

        setupInputControlData(isMandatory())

        setOnSingleClickListener {
            goToPushFragment(bindingAdapterPosition)
        }

        showServerError()
    }

    override fun fill(value: Any) {
        super.fill(value)
        val string = if (value == JSONObject.NULL) "" else value.toString()
        if (string.isNotEmpty()) {
            currentEditEntityValue = value
        }
    }

    override fun formatToDisplay(input: String): String {
        return input
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
                input.setText(placeHolder)
            } else {
                setTextOrIcon(container, input, displayTextMap[position])
            }
        }
    }

    override fun validate(displayError: Boolean): Boolean {
        if (isMandatory() && isInputValid(input.text.toString())) {
            if (displayError) {
                showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            }
            return false
        }
        return true
    }
}
