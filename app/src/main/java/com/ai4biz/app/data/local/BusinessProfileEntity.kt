package com.ai4biz.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profiles")
data class BusinessProfileEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val businessName: String,
    val businessType: String,
    val industry: String,
    val description: String,
    val country: String,
    val state: String,
    val city: String,
    val address: String?,
    val phone: String?,
    val email: String?,
    val website: String?,
    val whatsapp: String?,
    val currency: String,
    val taxNumber: String?,
    val createdAt: Long,
    val updatedAt: Long
)
