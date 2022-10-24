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

    override fun bind(actionTask: ActionTask, isFromSettings: Boolean) {
        super.bind(actionTask, isFromSettings)

        if (!actionTask.message.isNullOrEmpty()) {
            binding.status.text = actionTask.message
        }

        when (actionTask.status) {
            ActionTask.Status.SUCCESS -> {
                binding.iconState.setImageResource(R.drawable.check_circle)
            }
            ActionTask.Status.ERROR_SERVER -> {
                binding.iconState.setImageResource(R.drawable.error)
            }
            else -> {}
        }
    }
}
