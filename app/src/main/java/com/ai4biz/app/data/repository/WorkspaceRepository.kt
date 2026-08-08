package com.ai4biz.app.data.repository

import com.ai4biz.app.data.local.WorkspaceDao
import com.ai4biz.app.data.local.WorkspaceDefaults
import com.ai4biz.app.data.local.WorkspaceEntity
import com.ai4biz.app.model.Workspace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The business(es) a user manages. See docs/PHASE2_ARCHITECTURE.md -- this
 * is Phase 2 Sprint 1's data foundation; multi-workspace switching UI
 * doesn't exist yet, so today every install has exactly one (the default,
 * see [getOrCreateDefaultWorkspace]).
 */
class WorkspaceRepository(
    private val dao: WorkspaceDao,
    private val deviceIdentityRepository: DeviceIdentityRepository
) {

    fun observeWorkspaces(): Flow<List<Workspace>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeWorkspace(id: String): Flow<Workspace?> =
        dao.observeById(id).map { it?.toDomain() }

    fun observeActiveWorkspace(): Flow<Workspace?> =
        dao.observeActive().map { it?.toDomain() }

    suspend fun save(workspace: Workspace) {
        dao.upsert(workspace.toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    /**
     * Idempotent bootstrap: returns the active workspace if one already
     * exists (true on every migrated install, since MIGRATION_1_2 seeds
     * one), otherwise creates the default workspace (fresh installs).
     * Safe to call on every app start -- see Ai4bizApplication.onCreate().
     */
    suspend fun getOrCreateDefaultWorkspace(): Workspace {
        dao.getActiveOnce()?.let { return it.toDomain() }

        val now = System.currentTimeMillis()
        val workspace = Workspace(
            id = WorkspaceDefaults.DEFAULT_WORKSPACE_ID,
            ownerUserId = deviceIdentityRepository.getOrCreateDeviceUserId(),
            name = "My Business",
            businessProfileId = null,
            createdAt = now,
            updatedAt = now,
            isActive = true
        )
        dao.upsert(workspace.toEntity())
        return workspace
    }

    private fun WorkspaceEntity.toDomain() = Workspace(
        id = id,
        ownerUserId = ownerUserId,
        name = name,
        businessProfileId = businessProfileId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isActive = isActive
    )

    private fun Workspace.toEntity() = WorkspaceEntity(
        id = id,
        ownerUserId = ownerUserId,
        name = name,
        businessProfileId = businessProfileId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isActive = isActive
    )
}
