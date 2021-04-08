/*
 * Created by Quentin Marciset on 8/4/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.ui.parallax

import android.content.Context
import android.util.AttributeSet

class VerticalScrollParallaxImageView : ScrollTransformImageView {
    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attributeSet: AttributeSet) : super(ctx, attributeSet)

    init {
        super.viewTransformer = VerticalParallaxTransformer()
    }
}
