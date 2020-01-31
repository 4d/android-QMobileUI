package com.qmarciset.androidmobileui

/**
 * Base interface for fragments
 */
interface BaseFragment {

    var delegate: FragmentCommunication

    fun getViewModel()

    fun setupObservers()
}
