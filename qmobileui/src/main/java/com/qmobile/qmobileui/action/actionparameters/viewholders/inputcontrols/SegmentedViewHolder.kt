/*
 * Created by qmarciset on 22/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols

import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import androidx.core.view.children
import androidx.fragment.app.FragmentManager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.inputcontrols.InputControl
import com.qmobile.qmobileui.action.inputcontrols.InputControl.getImageName
import com.qmobile.qmobileui.action.inputcontrols.InputControlFormatHolder
import com.qmobile.qmobileui.binding.getColorFromAttr
import com.qmobile.qmobileui.binding.px
import com.qmobile.qmobileui.formatters.ImageNamed
import com.qmobile.qmobileui.ui.setMaterialFadeTransition
import org.json.JSONObject
import java.util.LinkedList

class SegmentedViewHolder(
    itemView: View,
    fragmentManager: FragmentManager?,
    formatHolderCallback: (holder: InputControlFormatHolder, position: Int) -> Unit,
    private val onDataLoadedCallback: () -> Unit,
    format: String = ""
) :
    CustomViewViewHolder(itemView, fragmentManager, formatHolderCallback, format) {

    private val horizontalScrollView: HorizontalScrollView = itemView.findViewById(R.id.horizontalScrollView)
    private val toggleButton: MaterialButtonToggleGroup = itemView.findViewById(R.id.toggleButton)

    private var isAButtonChecked = false

    companion object {
        private const val segmentedButtonIconPadding = 35
        private const val scrollMargin = 20
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

        toggleButton.addOnButtonCheckedListener { _, checkedId, isChecked ->
            onButtonChecked(checkedId, isChecked)
        }

        showServerError()
    }

    private fun onButtonChecked(checkedId: Int, isChecked: Boolean) {
        error.visibility = View.GONE
        toggleButton.children.forEachIndexed { index, view ->
            if (view.id == checkedId) {
                if (isChecked) {
                    isAButtonChecked = true
                    onItemSelected(index)
                    return@forEachIndexed
                } else {
                    isAButtonChecked = false
                    onItemDeselected()
                }
            }
        }
    }

    override fun setupValues(items: LinkedList<Any>, field: String?, entityFormat: String?) {
        items.forEachIndexed { index, entry ->
            val childButton: Button = View.inflate(itemView.context, R.layout.segmented_button, null) as Button
            getText(entry, index, isMandatory(), field, entityFormat) { displayText, fieldValue ->
                displayTextMap[index] = displayText
                fieldValueMap[index] = InputControl.getTypedValue(itemJsonObject, fieldValue)

                if (fieldMapping?.binding == "imageNamed") {
                    fieldMapping?.getImageName(displayText)?.let { imageName ->
                        ImageNamed.setDrawable(
                            childButton,
                            imageName,
                            imageNamedIconSize.px,
                            imageNamedIconSize.px
                        )
                    }
                    childButton.setPadding(segmentedButtonIconPadding.px, 0, 0, 0)
                } else {
                    childButton.text = displayText
                    childButton.setTextColor(itemView.context.getColorFromAttr(R.attr.colorOnSurface))
                }
                toggleButton.addView(childButton, index)
            }
        }
        handleDefaultField(bindingAdapterPosition) { position ->
            val child = toggleButton.getChildAt(position)
            child.performClick()
            horizontalScrollView.post { horizontalScrollView.smoothScrollTo(child.left - scrollMargin, 0) }
        }

        setVisibility()
    }

    override fun validate(displayError: Boolean): Boolean {
        if (isMandatory() && !isAButtonChecked) {
            if (displayError) {
                showError(itemView.context.getString(R.string.action_parameter_mandatory_error))
            }
            return false
        }
        return true
    }

    override fun setVisibility() {
        setMaterialFadeTransition(itemView.findViewById(R.id.segmented_container), true)
        circularProgressBar.visibility = View.GONE
        horizontalScrollView.visibility = View.VISIBLE
        onDataLoadedCallback()
    }
}
