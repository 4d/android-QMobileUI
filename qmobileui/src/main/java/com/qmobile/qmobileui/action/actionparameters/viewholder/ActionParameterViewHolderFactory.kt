/*
 * Created by qmarciset on 3/6/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.actionparameters.viewholder

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.action.actionparameters.ActionParameterEnum

@Suppress("LongMethod")
class ActionParameterViewHolderFactory private constructor() {
    companion object {
        fun createViewHolderFromViewType(
            viewType: Int,
            parent: ViewGroup,
            context: Context
        ): ActionParameterViewHolder {
            return when (val itemType = ActionParameterEnum.values()[viewType]) {
                // Text
                ActionParameterEnum.TEXT_DEFAULT,
                ActionParameterEnum.TEXT_EMAIL,
                ActionParameterEnum.TEXT_PASSWORD,
                ActionParameterEnum.TEXT_ZIP,
                ActionParameterEnum.TEXT_PHONE,
                ActionParameterEnum.TEXT_ACCOUNT,
                ActionParameterEnum.TEXT_URL ->
                    TextViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_text, parent, false),
                        itemType.format
                    )
                ActionParameterEnum.TEXT_AREA ->
                    TextAreaViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_text_area, parent, false)
                    )

                // Boolean
                ActionParameterEnum.BOOLEAN_DEFAULT ->
                    BooleanSwitchViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_boolean_switch, parent, false)
                    )
                ActionParameterEnum.BOOLEAN_CHECK ->
                    BooleanCheckMarkViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_boolean_check_mark, parent, false)
                    )

                // Number
                ActionParameterEnum.NUMBER_DEFAULT1,
                ActionParameterEnum.NUMBER_DEFAULT2,
                ActionParameterEnum.NUMBER_INTEGER ->
                    NumberViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_number, parent, false),
                        itemType.format
                    )
                ActionParameterEnum.NUMBER_SPELL_OUT ->
                    SpellOutViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_number, parent, false)
                    )
                ActionParameterEnum.NUMBER_PERCENTAGE ->
                    PercentageViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_number, parent, false)
                    )
                ActionParameterEnum.NUMBER_SCIENTIFIC ->
                    ScientificViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_number, parent, false)
                    )

                // Date
                ActionParameterEnum.DATE_DEFAULT1,
                ActionParameterEnum.DATE_DEFAULT2,
                ActionParameterEnum.DATE_SHORT,
                ActionParameterEnum.DATE_FULL,
                ActionParameterEnum.DATE_LONG ->
                    DateViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_date, parent, false),
                        itemType.format
                    )

                // Time
                ActionParameterEnum.TIME_DEFAULT,
                ActionParameterEnum.TIME_DURATION ->
                    TimeViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_time, parent, false),
                        itemType.format
                    )

                // Image
                ActionParameterEnum.IMAGE ->
                    ImageViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_image, parent, false)
                    )
                // Bar/QR code
                ActionParameterEnum.BARCODE ->
                    BarCodeViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_barcode, parent, false)
                    )
                ActionParameterEnum.SIGNATURE ->
                    SignatureViewHolder(
                        LayoutInflater.from(context)
                            .inflate(R.layout.item_parameter_signature, parent, false)
                    )
            }
        }
    }
}
