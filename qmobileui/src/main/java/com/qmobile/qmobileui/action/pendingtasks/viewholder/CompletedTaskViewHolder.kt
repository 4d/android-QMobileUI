/*
 * Created by qmarciset on 22/7/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.pendingtasks.viewholder

import com.qmobile.qmobiledatastore.dao.ActionTask
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.databinding.ItemCompletedTaskBinding

class CompletedTaskViewHolder(
    private val binding: ItemCompletedTaskBinding,
    onItemClick: (actionTask: ActionTask) -> Unit
) : BaseTaskViewHolder(binding.root, onItemClick) {

    override fun bind(item: ActionTask, isFromSettings: Boolean) {
        super.bind(item, isFromSettings)

        if (!item.message.isNullOrEmpty()) {
            binding.status.text = item.message
        }

        when (item.status) {
            ActionTask.Status.SUCCESS -> {
                binding.iconState.setImageResource(R.drawable.check_circle)
            }
            ActionTask.Status.ERROR_SERVER -> {
                binding.iconState.setImageResource(R.drawable.alert_circle)
            }
            else -> {}
        }
    }
}
