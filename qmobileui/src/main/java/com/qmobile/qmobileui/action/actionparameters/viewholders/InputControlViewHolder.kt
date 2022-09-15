/*
 * Created by qmarciset on 5/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.view.View
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobiledatasync.utils.BaseInputControl

open class InputControlViewHolder(itemView: View, format: String = "") : BaseInputLessViewHolder(itemView, format) {

    override fun bind(
        item: Any,
        currentEntity: RoomEntity?,
        isLastParameter: Boolean,
        alreadyFilledValue: Any?,
        serverError: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, isLastParameter, alreadyFilledValue, serverError, onValueChanged)

        onValueChanged(parameterName, input.text.toString(), null, validate(false))

        val format = itemJsonObject.getSafeString("format")

        BaseApp.genericActionHelper.getInputControl(itemView, format)?.let { inputControl ->

            if (inputControl.getIconName().isNotEmpty()) {
                container.endIconDrawable = apu.getInputControlDrawable(itemView.context, inputControl.getIconName())
            }

            if (inputControl.autocomplete) {
                processInputControl(inputControl, onValueChanged)
            }
            setOnSingleClickListener {
                processInputControl(inputControl, onValueChanged)
            }
        }
    }

    private fun processInputControl(
        inputControl: BaseInputControl,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        inputControl.process { outputText ->
            input.setText(outputText as String)
            onValueChanged(parameterName, outputText, null, validate(false))
        }
    }

    override fun formatToDisplay(input: String): String = input
}
