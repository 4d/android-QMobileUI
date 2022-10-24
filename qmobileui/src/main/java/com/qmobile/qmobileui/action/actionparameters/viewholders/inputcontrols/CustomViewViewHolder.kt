/*
 * Created by qmarciset on 29/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols

import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.utils.FieldMapping
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.actionparameters.viewholders.BaseViewHolder
import com.qmobile.qmobileui.action.inputcontrols.InputControlFormatHolder
import org.json.JSONObject

abstract class CustomViewViewHolder(
    itemView: View,
    fragmentManager: FragmentManager?,
    private val formatHolderCallback: (holder: InputControlFormatHolder, position: Int) -> Unit,
    format: String = ""
) : BaseViewHolder(itemView, format), BaseInputControlViewHolder {

    override val fragMng: FragmentManager? = fragmentManager
    override var fieldMapping: FieldMapping? = null
    override val placeHolder: String = ""
    override val circularProgressBar: CircularProgressIndicator = itemView.findViewById(R.id.circular_progress)

    private val label: TextView = itemView.findViewById(R.id.label)
    internal val error: TextView = itemView.findViewById(R.id.error)
    internal val fieldValueMap = mutableMapOf<Int, Any?>()
    internal val displayTextMap = mutableMapOf<Int, String>()

    companion object {
        internal const val imageNamedIconSize = 18
    }

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

        itemJsonObject.getSafeString("label")?.let { parameterLabel ->
            label.text = if (isMandatory()) {
                "$parameterLabel *"
            } else {
                parameterLabel
            }
        }

        setupInputControlData(isMandatory())
    }

    fun handleDefaultField(foundPositionInMapCallback: (position: Int) -> Unit) {
        val fieldValue: Any? = getFieldValue(itemJsonObject, bindingAdapterPosition, "", "")
        if (fieldValue != null) {
            for ((position, value) in fieldValueMap) {
                if (value == fieldValue) {
                    foundPositionInMapCallback(position)
                    onValueChanged(parameterName, fieldValueMap[position], null, validate(false))
                    return
                }
            }
        }
        // Done only if no fieldValue sent or not found in map
        onValueChanged(parameterName, null, null, validate(false))
    }

    fun onItemSelected(position: Int) {
        error.visibility = View.GONE
        onValueChanged(parameterName, fieldValueMap[position], null, validate(false))
        displayTextMap[position]?.let { displayText ->
            val inputControlFormatHolder =
                InputControlFormatHolder(displayText, fieldValueMap[position])
            formatHolderCallback(inputControlFormatHolder, bindingAdapterPosition)
        }
    }

    fun onItemDeselected() {
        onValueChanged(parameterName, null, null, validate(false))
        val inputControlFormatHolder = InputControlFormatHolder("", null)
        formatHolderCallback(inputControlFormatHolder, bindingAdapterPosition)
    }

    override fun getInputType(format: String): Int {
        return -1
    }

    override fun showError(text: String) {
        error.text = text
        error.visibility = View.VISIBLE
    }

    override fun fill(value: Any) {}

    abstract fun setVisibility()
}
