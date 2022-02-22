/*
 * Created by qmarciset on 9/2/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.Manifest
import android.view.View
import androidx.core.content.ContextCompat
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.ActionTypes
import com.qmobile.qmobileui.utils.PermissionChecker

class BarcodeViewHolder(
    itemView: View,
    private val actionTypesCallback: (actionTypes: ActionTypes, position: Int) -> Unit
) : BaseInputLessViewHolder(itemView) {

    override fun bind(
        item: Any,
        currentEntity: EntityModel?,
        preset: String?,
        isLastParameter: Boolean,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntity, preset, isLastParameter, onValueChanged)

        container.endIconDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.barcode_scan)

        itemJsonObject.getSafeString("barcode_value")?.let {
            input.setText(it)
        }

        onValueChanged(parameterName, input.text.toString(), null, validate(false))

        onEndIconClick {
            (itemView.context as PermissionChecker?)?.askPermission(
                context = itemView.context,
                permission = Manifest.permission.CAMERA,
                rationale = itemView.context.getString(R.string.permission_rationale_barcode)
            ) { isGranted ->
                if (isGranted) {
                    actionTypesCallback(ActionTypes.SCAN, bindingAdapterPosition)
                }
            }
        }
    }

    override fun formatToDisplay(input: String): String = input
}
