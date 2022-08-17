/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.actionparameters.ActionParameter
import com.qmobile.qmobileui.action.model.Action

@Suppress("LongMethod")
class ActionParameterViewHolderFactory private constructor() {
    companion object {
        fun createViewHolderFromViewType(
            viewType: Int,
            parent: ViewGroup,
            context: Context,
            fragmentManager: FragmentManager?,
            actionTypesCallback: (actionTypes: Action.Type, position: Int) -> Unit
        ): BaseViewHolder {
            return when (val itemType = ActionParameter.values()[viewType]) {
                // Text
                ActionParameter.TEXT_DEFAULT,
                ActionParameter.TEXT_EMAIL,
                ActionParameter.TEXT_PASSWORD,
                ActionParameter.TEXT_ZIP,
                ActionParameter.TEXT_PHONE,
                ActionParameter.TEXT_ACCOUNT,
                ActionParameter.TEXT_URL,
                ActionParameter.TEXT_AREA ->
                    TextViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_text, parent, false),
                        itemType.format
                    )

                // Number
                ActionParameter.NUMBER_DEFAULT1,
                ActionParameter.NUMBER_DEFAULT2,
                ActionParameter.NUMBER_INTEGER,
                ActionParameter.NUMBER_PERCENTAGE,
                ActionParameter.NUMBER_SCIENTIFIC,
                ActionParameter.NUMBER_SPELL_OUT ->
                    NumberViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_text, parent, false),
                        itemType.format
                    )

                // Date
                ActionParameter.DATE_DEFAULT1,
                ActionParameter.DATE_DEFAULT2,
                ActionParameter.DATE_SHORT,
                ActionParameter.DATE_FULL,
                ActionParameter.DATE_LONG ->
                    DateViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_text, parent, false),
                        itemType.format,
                        fragmentManager
                    )

                // Time
                ActionParameter.TIME_DEFAULT,
                ActionParameter.TIME_DURATION ->
                    TimeViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_text, parent, false),
                        itemType.format,
                        fragmentManager
                    )
                // Boolean
                ActionParameter.BOOLEAN_DEFAULT ->
                    BooleanViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_boolean_switch, parent, false),
                        itemType.format
                    )
                ActionParameter.BOOLEAN_CHECK ->
                    BooleanViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_boolean_check_mark, parent, false),
                        itemType.format
                    )

                // Image
                ActionParameter.IMAGE ->
                    ImageViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_image, parent, false),
                        itemType.format,
                        actionTypesCallback
                    )
                ActionParameter.SIGNATURE ->
                    SignatureViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_image, parent, false),
                        itemType.format,
                        actionTypesCallback
                    )
                ActionParameter.BARCODE ->
                    BarcodeViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_text, parent, false),
                        itemType.format,
                        actionTypesCallback
                    )
            }
        }
    }
}