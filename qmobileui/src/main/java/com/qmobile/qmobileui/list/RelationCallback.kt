/*
 * Created by Quentin Marciset on 14/4/2021.
 * 4D SAS
 * Copyright (c) 2021 Quentin Marciset. All rights reserved.
 */

package com.qmobile.qmobileui.list

import androidx.lifecycle.LiveData
import com.qmobile.qmobileapi.model.entity.EntityModel
import com.qmobile.qmobiledatastore.data.RoomRelation

interface RelationCallback {
    fun getRelations(entity: EntityModel): Map<String, LiveData<RoomRelation>>
}
