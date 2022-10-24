/*
 * Created by qmarciset on 28/9/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders.inputcontrols

import android.view.View
import androidx.fragment.app.FragmentManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.qmobile.qmobiledatastore.data.RoomEntity
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.inputcontrols.InputControl
import com.qmobile.qmobileui.action.inputcontrols.InputControl.getImageName
import com.qmobile.qmobileui.action.inputcontrols.InputControlFormatHolder
import com.qmobile.qmobileui.binding.ImageHelper
import com.qmobile.qmobileui.binding.px
import com.qmobile.qmobileui.ui.setMaterialFadeTransition
import com.qmobile.qmobileui.ui.setOnSingleClickListener
import org.json.JSONObject
import java.util.LinkedList

class PickerViewHolder(
    itemView: View,
    fragmentManager: FragmentManager?,
    formatHolderCallback: (holder: InputControlFormatHolder, position: Int) -> Unit,
    private val onDataLoadedCallback: () -> Unit,
    format: String = ""
) : CustomViewViewHolder(itemView, fragmentManager, formatHolderCallback, format) {

    private val chipGroup: ChipGroup = itemView.findViewById(R.id.scroll_group)
    private var isAChipChecked = false

    companion object {
        private const val chipPadding = 18
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

        showServerError()
    }

    override fun setupValues(items: LinkedList<Any>, field: String?, entityFormat: String?) {
        items.forEachIndexed { index, entry ->
            val chip: Chip = View.inflate(itemView.context, R.layout.input_control_picker_item, null) as Chip
            getText(entry, index, isMandatory(), field, entityFormat) { displayText, fieldValue ->
                displayTextMap[index] = displayText
                fieldValueMap[index] = InputControl.getTypedValue(itemJsonObject, fieldValue)

                if (fieldMapping?.binding == "imageNamed") {
                    fieldMapping?.getImageName(displayText)?.let { imageName ->
                        chip.chipIcon = ImageHelper.getDrawableFromString(
                            itemView.context,
                            imageName,
                            imageNamedIconSize.px,
                            imageNamedIconSize.px
                        )
                    }
                    chip.iconStartPadding = chipPadding.px.toFloat()
                } else {
                    chip.text = displayText
                }

                chip.setOnSingleClickListener {
                    onChipChecked(index)
                }

                chipGroup.addView(chip)
            }
        }

        handleDefaultField { position ->
            chipGroup.getChildAt(position).performClick()
        }

        setVisibility()
    }

    private fun onChipChecked(position: Int) {
        val chip = chipGroup.getChildAt(position) as? Chip
        if (chip?.isChecked == true) {
            isAChipChecked = true
            onItemSelected(position)
        } else {
            isAChipChecked = false
            onItemDeselected()
        }
    }

    override fun validate(displayError: Boolean): Boolean {
        if (isMandatory() && !isAChipChecked) {
            if (displayError) {
                showError(itemView.context.getString(R.string.action_parameter_mandatory_error))
            }
            return false
        }
        return true
    }

    override fun setVisibility() {
        setMaterialFadeTransition(itemView.findViewById(R.id.picker_container), true)
        circularProgressBar.visibility = View.GONE
        chipGroup.visibility = View.VISIBLE
        onDataLoadedCallback()
    }
}
