package com.ai4biz.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ai4biz.app.model.BrandTone

@Entity(tableName = "brand_settings")
data class BrandSettingsEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val brandName: String,
    val tagline: String,
    val tone: BrandTone,
    val writingStyle: String,
    val primaryColor: String,
    val secondaryColor: String,
    val defaultLanguage: String,
    val logoUri: String?
)
