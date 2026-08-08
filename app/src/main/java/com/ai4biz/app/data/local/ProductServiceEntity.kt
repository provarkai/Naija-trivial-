package com.ai4biz.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ai4biz.app.model.ProductServiceType

@Entity(tableName = "products_services")
data class ProductServiceEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val name: String,
    val type: ProductServiceType,
    val description: String,
    val category: String,
    val price: Double,
    val currency: String,
    val unit: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
