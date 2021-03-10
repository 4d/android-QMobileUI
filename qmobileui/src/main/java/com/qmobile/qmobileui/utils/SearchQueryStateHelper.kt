/*
 * Created by Quentin Marciset on 10/3/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.utils
//
object SearchQueryStateHelper {
    private  var stringToSearch : String ="empty"
    fun setString(string: String) {this.stringToSearch = string}
    fun getString() = this.stringToSearch
}