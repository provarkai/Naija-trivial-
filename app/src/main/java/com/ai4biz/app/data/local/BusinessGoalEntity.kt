package com.ai4biz.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ai4biz.app.model.BusinessGoalType

@Entity(tableName = "business_goals")
data class BusinessGoalEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val goalType: BusinessGoalType,
    val description: String,
    val priority: Int,
    val isActive: Boolean
)
