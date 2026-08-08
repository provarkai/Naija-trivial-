package com.ai4biz.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessGoalDao {

    @Query("SELECT * FROM business_goals WHERE workspaceId = :workspaceId")
    fun observeAllByWorkspace(workspaceId: String): Flow<List<BusinessGoalEntity>>

    @Query("SELECT * FROM business_goals WHERE id = :id")
    fun observeById(id: String): Flow<BusinessGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: BusinessGoalEntity)

    @Query("DELETE FROM business_goals WHERE id = :id")
    suspend fun deleteById(id: String)
}
