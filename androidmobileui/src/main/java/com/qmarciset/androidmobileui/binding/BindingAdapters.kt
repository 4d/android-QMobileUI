/*
 * Created by Quentin Marciset on 11/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmarciset.androidmobileui.binding

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.qmarciset.androidmobileui.R
import com.qmarciset.androidmobileui.glide.CustomRequestListener

/**
 * Sample avatar list
 */
private val listOfAvatars = listOf(
    R.drawable.avatar_1_raster,
    R.drawable.avatar_2_raster,
    R.drawable.avatar_3_raster,
    R.drawable.avatar_4_raster,
    R.drawable.avatar_5_raster,
    R.drawable.avatar_6_raster
)

/**
 * Provides one random avatar from the sample avatar list
 */
private fun randomAvatar(): Int =
    listOfAvatars[(listOfAvatars.indices).random() % listOfAvatars.size]

/**
 * Use Glide to load image url in a view
 */
@BindingAdapter(value = ["imageUrl", "requestListener"], requireAll = false)
fun bindImageFromUrl(view: ImageView, imageUrl: String?, listener: RequestListener<Drawable?>?) {
    val factory =
        DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()

    Glide.with(view.context.applicationContext)
        .load(if (imageUrl.isNullOrEmpty()) randomAvatar() else imageUrl)
        .transition(DrawableTransitionOptions.withCrossFade(factory))
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .centerCrop()
//        .listener(listener)
        .listener(CustomRequestListener())
        .error(R.drawable.ic_error_black_24dp)
//        .placeholder(R.drawable.profile_placeholder)
        .transform(CircleCrop())
        .into(view)
}

/**
 * Use Glide to load image drawable in a view
 */
@BindingAdapter(value = ["imageUrl", "requestListener"], requireAll = false)
fun bindImageFromUrl(view: ImageView, drawable: Drawable?, listener: RequestListener<Drawable?>?) {
    val factory =
        DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()

    Glide.with(view.context.applicationContext)
        .load(drawable ?: randomAvatar())
        .transition(DrawableTransitionOptions.withCrossFade(factory))
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .centerCrop()
//        .listener(listener)
        .listener(CustomRequestListener())
        .error(R.drawable.ic_error_black_24dp)
//        .placeholder(R.drawable.profile_placeholder)
        .transform(CircleCrop())
        .into(view)
}

/**
 * Use Glide to load image drawable in a view
 */
@BindingAdapter("imageDrawable")
fun bindImageFromDrawable(view: ImageView, imageDrawable: Int?) {
    if (imageDrawable == null)
        return

    val factory =
        DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()

    Glide.with(view.context.applicationContext)
        .load(imageDrawable)
        .transition(DrawableTransitionOptions.withCrossFade(factory))
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .error(R.drawable.ic_error_black_24dp)
        .into(view)
}

@BindingAdapter("visibleGone")
fun showHide(view: View, show: Boolean) {
    view.visibility = if (show) View.VISIBLE else View.GONE
}

@BindingAdapter("concatStringWithSpace_1", "concatStringWithSpace_2")
fun concatStringWithSpace(view: TextView, str1: String?, str2: String?) {
    view.text = if (str1.isNullOrEmpty()) {
        str2
    } else {
        if (str2.isNullOrEmpty())
            str1
        else
            "$str1 $str2"
    }
}

@BindingAdapter("concatStringRatio_1", "concatStringRatio_2")
fun concatStringRatio(view: TextView, str1: String? = "0", str2: String? = "0") {
    view.text = "$str1/$str2"
}

/*@BindingAdapter("setPersonName")
fun bindPersonName(view: TextView, person: Employee?) {
    person?.let {
        view.text = if (person.LastName.isNullOrEmpty()) {
            person.FirstName
        } else {
            if (person.FirstName.isNullOrEmpty())
                person.LastName
            else
                "${person.FirstName} ${person.LastName}"
        }
    }
}*/

/*@BindingAdapter("setRatio")
fun bindRatio(view: TextView, office: Office?) {
    office?.let {
        view.text = if (it.deskNumber != null && it.deskTaken != null)
            "${it.deskTaken}/${it.deskNumber}"
        else
            ""
    }
}*/
