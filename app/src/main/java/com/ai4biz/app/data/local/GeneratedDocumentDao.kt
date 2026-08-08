package com.ai4biz.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneratedDocumentDao {

    @Query("SELECT * FROM generated_documents ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GeneratedDocumentEntity>>

    // Added for Phase 2 (docs/PHASE2_ARCHITECTURE.md, Sprint 2) -- not
    // called anywhere yet since workspace-switching UI doesn't exist, but
    // added now so that work doesn't need another migration later.
    @Query("SELECT * FROM generated_documents WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun observeAllByWorkspace(workspaceId: String): Flow<List<GeneratedDocumentEntity>>

    @Query("SELECT * FROM generated_documents WHERE id = :id")
    fun observeById(id: String): Flow<GeneratedDocumentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: GeneratedDocumentEntity)

    @Query("DELETE FROM generated_documents WHERE id = :id")
    suspend fun deleteById(id: String)
}
