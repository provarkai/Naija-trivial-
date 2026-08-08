package com.ai4biz.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductServiceDao {

    @Query("SELECT * FROM products_services WHERE workspaceId = :workspaceId")
    fun observeAllByWorkspace(workspaceId: String): Flow<List<ProductServiceEntity>>

    @Query("SELECT * FROM products_services WHERE id = :id")
    fun observeById(id: String): Flow<ProductServiceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(productService: ProductServiceEntity)

    @Query("DELETE FROM products_services WHERE id = :id")
    suspend fun deleteById(id: String)
}
