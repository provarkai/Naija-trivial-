package com.ai4biz.app.data.repository

import com.ai4biz.app.data.local.ProductServiceDao
import com.ai4biz.app.data.local.ProductServiceEntity
import com.ai4biz.app.model.ProductService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductServiceRepository(private val dao: ProductServiceDao) {

    fun observeAllByWorkspace(workspaceId: String): Flow<List<ProductService>> =
        dao.observeAllByWorkspace(workspaceId).map { entities -> entities.map { it.toDomain() } }

    fun observe(id: String): Flow<ProductService?> =
        dao.observeById(id).map { it?.toDomain() }

    /** Also used to edit an existing item -- save(x.copy(...)) with the same id upserts. */
    suspend fun save(productService: ProductService) {
        dao.upsert(productService.toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    private fun ProductServiceEntity.toDomain() = ProductService(
        id = id,
        workspaceId = workspaceId,
        name = name,
        type = type,
        description = description,
        category = category,
        price = price,
        currency = currency,
        unit = unit,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun ProductService.toEntity() = ProductServiceEntity(
        id = id,
        workspaceId = workspaceId,
        name = name,
        type = type,
        description = description,
        category = category,
        price = price,
        currency = currency,
        unit = unit,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
