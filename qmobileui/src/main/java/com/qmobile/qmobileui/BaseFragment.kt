/*
 * Created by Quentin Marciset on 7/2/2020.
 * 4D SAS
 * Copyright (c) 2020 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui

import android.content.Context
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {

    internal lateinit var delegate: FragmentCommunication

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentCommunication) {
            delegate = context
        }
    }
}
