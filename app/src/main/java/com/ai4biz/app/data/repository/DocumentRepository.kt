package com.ai4biz.app.data.repository

import com.ai4biz.app.data.local.GeneratedDocumentDao
import com.ai4biz.app.data.local.GeneratedDocumentEntity
import com.ai4biz.app.data.local.WorkspaceDefaults
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

    /** Phase 2 Sprint 3 (docs/PHASE2_ARCHITECTURE.md) -- backs the Workspace screen's Documents tab. */
    fun observeHistoryByWorkspace(workspaceId: String): Flow<List<GeneratedDocument>> =
        dao.observeAllByWorkspace(workspaceId).map { entities -> entities.map { it.toDomain() } }

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
        createdAt = createdAt,
        // GeneratedDocument (domain) doesn't carry a workspaceId yet -- see
        // docs/PHASE2_ARCHITECTURE.md Sprint 2. Every document is attributed
        // to the single default workspace until workspace-switching UI exists.
        workspaceId = WorkspaceDefaults.DEFAULT_WORKSPACE_ID
    )
}
