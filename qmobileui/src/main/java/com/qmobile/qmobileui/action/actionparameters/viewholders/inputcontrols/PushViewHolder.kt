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
    override val placeHolder = itemView.context.getString(R.string.input_control_push_baseline)
    override val circularProgressBar: CircularProgressIndicator? = null

    override fun bind(
        item: JSONObject,
        currentEntity: RoomEntity?,
        isLastParameter: Boolean,
        alreadyFilledValue: Any?,
        serverError: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, isLastParameter, alreadyFilledValue, serverError, onValueChanged)

        fieldMapping = getFieldMapping(itemJsonObject)

        container.isExpandedHintEnabled = false
        container.endIconDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.chevron_right)

        val displayText = getDisplayText(itemJsonObject, bindingAdapterPosition, input.text.toString(), placeHolder)
        setTextOrIcon(container, input, displayText)

        val fieldValue: Any? = getFieldValue(itemJsonObject, bindingAdapterPosition, input.text.toString(), placeHolder)
        val typedValue = InputControl.getTypedValue(itemJsonObject, fieldValue)
        onValueChanged(parameterName, typedValue, null, validate(false))

        setOnSingleClickListener {
            goToPushFragment(bindingAdapterPosition)
        }

        showServerError()
    }

    override fun formatToDisplay(input: String): String {
        return input
    }

    override fun setupValues(items: LinkedList<Any>, field: String?, entityFormat: String?) {
        // Nothing to do
    }

    override fun validate(displayError: Boolean): Boolean {
        if (isMandatory() && isInputValid(input.text.toString())) {
            if (displayError) {
                showError(itemView.context.getString(R.string.action_parameter_mandatory_error))
            }
            return false
        }
        return true
    }
}
