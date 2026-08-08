package com.ai4biz.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessProfileDao {

    @Query("SELECT * FROM business_profiles WHERE workspaceId = :workspaceId")
    fun observeAllByWorkspace(workspaceId: String): Flow<List<BusinessProfileEntity>>

    @Query("SELECT * FROM business_profiles WHERE id = :id")
    fun observeById(id: String): Flow<BusinessProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: BusinessProfileEntity)

    @Query("DELETE FROM business_profiles WHERE id = :id")
    suspend fun deleteById(id: String)
}
