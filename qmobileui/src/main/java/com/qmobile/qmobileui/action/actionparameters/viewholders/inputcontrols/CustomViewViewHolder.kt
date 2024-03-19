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
    override var placeHolder: String = ""
    override var currentEditEntityValue: Any? = null
    override val circularProgressBar: CircularProgressIndicator = itemView.findViewById(R.id.circular_progress)
    override val fieldValueMap = mutableMapOf<Int, Any?>()
    override val displayTextMap = mutableMapOf<Int, String>()

    private val label: TextView = itemView.findViewById(R.id.label)
    internal val error: TextView = itemView.findViewById(R.id.error)

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

        fieldMapping = retrieveFieldMapping()

        itemJsonObject.getSafeString("label")?.let { parameterLabel ->
            label.text = if (isMandatory()) {
                "$parameterLabel *"
            } else {
                parameterLabel
            }
        }

        alreadyFilledValue?.let {
            fill(it)
        } ?: kotlin.run {
            getDefaultFieldValue(currentEntity, itemJsonObject) {
                fill(it)
            }
        }

        setupInputControlData(isMandatory())
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

    override fun fill(value: Any) {
        val string = if (value == JSONObject.NULL) "" else value.toString()
        if (string.isNotEmpty()) {
            currentEditEntityValue = value
        }
    }

    abstract fun setVisibility()
}
