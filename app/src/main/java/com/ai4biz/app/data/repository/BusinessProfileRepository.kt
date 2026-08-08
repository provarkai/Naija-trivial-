package com.ai4biz.app.data.repository

import com.ai4biz.app.data.local.BusinessProfileDao
import com.ai4biz.app.data.local.BusinessProfileEntity
import com.ai4biz.app.model.BusinessProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BusinessProfileRepository(private val dao: BusinessProfileDao) {

    fun observeAllByWorkspace(workspaceId: String): Flow<List<BusinessProfile>> =
        dao.observeAllByWorkspace(workspaceId).map { entities -> entities.map { it.toDomain() } }

    fun observe(id: String): Flow<BusinessProfile?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun save(profile: BusinessProfile) {
        dao.upsert(profile.toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    private fun BusinessProfileEntity.toDomain() = BusinessProfile(
        id = id,
        workspaceId = workspaceId,
        businessName = businessName,
        businessType = businessType,
        industry = industry,
        description = description,
        country = country,
        state = state,
        city = city,
        address = address,
        phone = phone,
        email = email,
        website = website,
        whatsapp = whatsapp,
        currency = currency,
        taxNumber = taxNumber,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun BusinessProfile.toEntity() = BusinessProfileEntity(
        id = id,
        workspaceId = workspaceId,
        businessName = businessName,
        businessType = businessType,
        industry = industry,
        description = description,
        country = country,
        state = state,
        city = city,
        address = address,
        phone = phone,
        email = email,
        website = website,
        whatsapp = whatsapp,
        currency = currency,
        taxNumber = taxNumber,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
