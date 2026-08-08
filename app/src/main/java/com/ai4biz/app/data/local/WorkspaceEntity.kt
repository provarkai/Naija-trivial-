package com.ai4biz.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val name: String,
    val businessProfileId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean
)
