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

    @Query("SELECT * FROM generated_documents WHERE id = :id")
    fun observeById(id: String): Flow<GeneratedDocumentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: GeneratedDocumentEntity)

    @Query("DELETE FROM generated_documents WHERE id = :id")
    suspend fun deleteById(id: String)
}
