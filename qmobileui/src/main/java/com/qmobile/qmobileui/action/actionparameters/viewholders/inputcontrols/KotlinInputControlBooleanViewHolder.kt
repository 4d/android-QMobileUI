/*
 * Created by qmarciset on 22/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols

import android.view.View
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobiledatasync.app.BaseApp
import com.qmobile.qmobileui.action.actionparameters.viewholders.BooleanViewHolder
import org.json.JSONObject

class KotlinInputControlBooleanViewHolder(itemView: View, format: String = "") : BooleanViewHolder(itemView, format) {

    override fun bind(
        item: JSONObject,
        currentEntity: RoomEntity?,
        isLastParameter: Boolean,
        alreadyFilledValue: Any?,
        serverError: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, isLastParameter, alreadyFilledValue, serverError, onValueChanged)

        val format = itemJsonObject.getSafeString("format")

        BaseApp.genericResourceHelper.getKotlinInputControl(itemView, format)?.let { inputControl ->
            inputControl.process(inputValue = compoundButton.isChecked) { output ->
                if (output is Boolean) {
                    error.visibility = View.GONE
                    onValueChanged(parameterName, output, null, true)
                }
            }
        }
    }
}
