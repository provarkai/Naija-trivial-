package com.ai4biz.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BrandSettingsDao {

    @Query("SELECT * FROM brand_settings WHERE workspaceId = :workspaceId")
    fun observeAllByWorkspace(workspaceId: String): Flow<List<BrandSettingsEntity>>

    @Query("SELECT * FROM brand_settings WHERE id = :id")
    fun observeById(id: String): Flow<BrandSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: BrandSettingsEntity)

    @Query("DELETE FROM brand_settings WHERE id = :id")
    suspend fun deleteById(id: String)
}
