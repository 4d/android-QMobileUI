/*
 * Created by qmarciset on 11/1/2022.
 * 4D SAS
 * Copyright (c) 2022 qmarciset. All rights reserved.
 */

package com.qmobile.qmobileui.action.viewholders

import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobileapi.utils.getSafeAny
import com.qmobile.qmobileapi.utils.getSafeString
import com.qmobile.qmobileui.R
import com.qmobile.qmobileui.glide.CustomRequestListener

class ImageViewHolder(
    itemView: View,
    hideKeyboardCallback: () -> Unit,
    private val intentChooserCallback: (position: Int) -> Unit
) :
    BaseViewHolder(itemView, hideKeyboardCallback) {

    private val label: TextView = itemView.findViewById(R.id.label)
    private val error: TextView = itemView.findViewById(R.id.error)
    private var imageView: ImageView = itemView.findViewById(R.id.image_view)
    private var removeItem: ImageView = itemView.findViewById(R.id.item_remove)

    override fun bind(
        item: Any,
        currentEntityJsonObject: EntityModel?,
        onValueChanged: (String, Any?, String?, Boolean) -> Unit
    ) {
        super.bind(item, currentEntityJsonObject, onValueChanged)

        itemJsonObject.getSafeString("label")?.let { parameterLabel ->
            label.text = if (isMandatory()) {
                "$parameterLabel *"
            } else {
                parameterLabel
            }
        }

        imageView.setOnClickListener {
            intentChooserCallback(bindingAdapterPosition)
        }

        removeItem.setOnClickListener {
            ContextCompat.getDrawable(imageView.context, R.drawable.image_plus)?.let {
                imageView.setImageDrawable(it)
                removeItem.visibility = View.INVISIBLE
                imageView.alpha = 0.6F
            }
        }
        displaySelectedImageIfNeed()
        onValueChanged(parameterName, null, null, validate(false))
    }

    override fun validate(displayError: Boolean): Boolean {

        if (isMandatory() && !itemJsonObject.has("image_uri")) {
            if (displayError)
                showError(itemView.context.resources.getString(R.string.action_parameter_mandatory_error))
            return false
        }
        return true
    }

    override fun getInputType(format: String): Int {
        return -1
    }

    private fun displaySelectedImageIfNeed() {
        error.visibility = View.INVISIBLE
        itemJsonObject.getSafeAny("image_uri")?.let { anyUri ->
            (anyUri as Uri?)?.let { uri ->
                val factory =
                    DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
                val glideRequest = Glide.with(imageView.context.applicationContext)
                    .load(uri)
                    .transition(DrawableTransitionOptions.withCrossFade(factory))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(CustomRequestListener())
                    .error(R.drawable.ic_placeholder)

                glideRequest.into(imageView)
                imageView.alpha = 1F
                removeItem.visibility = View.VISIBLE
            }
        }
    }

    private fun showError(text: String) {
        error.text = text
        error.visibility = View.VISIBLE
    }
}
