package com.ai4biz.app.data.repository

import com.ai4biz.app.data.local.GeneratedDocumentDao
import com.ai4biz.app.data.local.GeneratedDocumentEntity
import com.ai4biz.app.model.GeneratedDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for generated documents / "History". Backed by
 * Room today; swapping in a remote sync (e.g. Firebase/Supabase, per the
 * PRD's Integrations list) only requires changing this class.
 */
class DocumentRepository(private val dao: GeneratedDocumentDao) {

    fun observeHistory(): Flow<List<GeneratedDocument>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeDocument(id: String): Flow<GeneratedDocument?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun save(document: GeneratedDocument) {
        dao.upsert(document.toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    private fun GeneratedDocumentEntity.toDomain() = GeneratedDocument(
        id = id,
        toolId = toolId,
        toolTitle = toolTitle,
        title = title,
        content = content,
        createdAt = createdAt
    )

    private fun GeneratedDocument.toEntity() = GeneratedDocumentEntity(
        id = id,
        toolId = toolId,
        toolTitle = toolTitle,
        title = title,
        content = content,
        createdAt = createdAt
    )
}
