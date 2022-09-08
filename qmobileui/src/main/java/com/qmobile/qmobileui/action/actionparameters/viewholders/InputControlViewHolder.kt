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

        itemJsonObject.getSafeString("inputControlIcon")?.let {
            container.endIconDrawable = apu.getInputControlDrawable(itemView.context, it)
        }

        onValueChanged(parameterName, input.text.toString(), null, validate(false))

        val format = itemJsonObject.getSafeString("format")

        setOnSingleClickListener {
            BaseApp.genericActionHelper.getInputControl(itemView, format)?.onClick { outputText ->
                input.setText(outputText)
                onValueChanged(parameterName, outputText, null, validate(false))
            }
        }
    }

    override fun formatToDisplay(input: String): String = input
}
