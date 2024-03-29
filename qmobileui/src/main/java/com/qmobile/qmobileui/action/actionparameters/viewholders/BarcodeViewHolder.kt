/*
 * Created by qmarciset on 11/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.Manifest
import android.view.View
import androidx.core.content.ContextCompat
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.model.Action
import com.qmobile.qmobileui.utils.PermissionChecker
import org.json.JSONObject

class BarcodeViewHolder(
    itemView: View,
    format: String,
    private val actionTypesCallback: (actionTypes: Action.Type, position: Int) -> Unit
) : BaseInputLessViewHolder(itemView, format) {

    override fun bind(
        item: JSONObject,
        currentEntity: RoomEntity?,
        isLastParameter: Boolean,
        alreadyFilledValue: Any?,
        serverError: String?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, isLastParameter, alreadyFilledValue, serverError, onValueChanged)

        container.endIconDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.qr_code_scanner)

        onValueChanged(parameterName, input.text.toString(), null, validate(false))

        setOnSingleClickListener {
            (itemView.context as? PermissionChecker)?.askPermission(
                context = itemView.context,
                permission = Manifest.permission.CAMERA,
                rationale = itemView.context.resources.getString(R.string.permission_rationale_barcode)
            ) { isGranted ->
                if (isGranted) {
                    actionTypesCallback(Action.Type.SCAN, bindingAdapterPosition)
                }
            }
        }
    }

    override fun formatToDisplay(input: String): String = input
}
