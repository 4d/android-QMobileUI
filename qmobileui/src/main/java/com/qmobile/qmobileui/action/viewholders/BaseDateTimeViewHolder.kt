/*
 * Created by qmarciset on 14/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.ui.ViewUtils

abstract class BaseDateTimeViewHolder(
    itemView: View,
    hideKeyboardCallback: () -> Unit
) : BaseViewHolder(itemView, hideKeyboardCallback) {

    internal val input: TextInputEditText = itemView.findViewById(R.id.input)
    internal val container: TextInputLayout = itemView.findViewById(R.id.container)

    private val shakeAnimation = ViewUtils.getShakeAnimation(itemView.context)

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        onValueChanged: (String, Any, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntityJsonObject, onValueChanged)

        itemJsonObject.getSafeString("label")?.let { parameterLabel ->
            container.hint = if (isMandatory()) {
                "$parameterLabel *"
            } else {
                parameterLabel
            }
        }

        if (itemJsonObject.getSafeString("default") != null)
            input.setText(itemJsonObject.getSafeString("default"))
        else if (itemJsonObject.getSafeString("placeholder") != null)
            input.setText(itemJsonObject.getSafeString("placeholder"))

        setDefaultFieldIfNeeded(currentEntityJsonObject, itemJsonObject, onValueChanged) {
            input.setText(it.toString())
        }
    }

    override fun validate(displayError: Boolean): Boolean {
        if (isMandatory() && input.text.toString().trim().isEmpty()) {
            if (displayError)
                showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }

        return true
    }

    private fun showError(text: String) {
        container.error = text
        input.startAnimation(shakeAnimation)
    }

    override fun getInputType(format: String): Int {
        return -1
    }

    internal fun configureInputLayout(
        fragmentManager: FragmentManager?,
        dialogFragment: DialogFragment
    ) {
        input.isLongClickable = false
        input.setTextIsSelectable(false)
        input.isFocusableInTouchMode = false
        input.isClickable = true
        input.setOnClickListener {
            fragmentManager?.let {
                val dialog = dialogFragment.dialog
                if (dialog == null || !dialog.isShowing) {
                    dialogFragment.show(it, dialogFragment.toString())
                }
            }
        }
    }
}
