package com.ai4biz.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val name: String,
    val companyName: String?,
    val email: String?,
    val phone: String?,
    val whatsapp: String?,
    val industry: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)
