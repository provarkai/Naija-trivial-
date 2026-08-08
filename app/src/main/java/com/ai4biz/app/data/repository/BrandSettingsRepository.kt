package com.ai4biz.app.data.repository

import com.ai4biz.app.data.local.BrandSettingsDao
import com.ai4biz.app.data.local.BrandSettingsEntity
import com.ai4biz.app.model.BrandSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BrandSettingsRepository(private val dao: BrandSettingsDao) {

    fun observeAllByWorkspace(workspaceId: String): Flow<List<BrandSettings>> =
        dao.observeAllByWorkspace(workspaceId).map { entities -> entities.map { it.toDomain() } }

    fun observe(id: String): Flow<BrandSettings?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun save(settings: BrandSettings) {
        dao.upsert(settings.toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    private fun BrandSettingsEntity.toDomain() = BrandSettings(
        id = id,
        workspaceId = workspaceId,
        brandName = brandName,
        tagline = tagline,
        tone = tone,
        writingStyle = writingStyle,
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        defaultLanguage = defaultLanguage,
        logoUri = logoUri
    )

    private fun BrandSettings.toEntity() = BrandSettingsEntity(
        id = id,
        workspaceId = workspaceId,
        brandName = brandName,
        tagline = tagline,
        tone = tone,
        writingStyle = writingStyle,
        primaryColor = primaryColor,
        secondaryColor = secondaryColor,
        defaultLanguage = defaultLanguage,
        logoUri = logoUri
    )
}
