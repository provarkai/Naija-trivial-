package com.ai4biz.app.data.repository

import com.ai4biz.app.data.local.BusinessGoalDao
import com.ai4biz.app.data.local.BusinessGoalEntity
import com.ai4biz.app.model.BusinessGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BusinessGoalRepository(private val dao: BusinessGoalDao) {

    fun observeAllByWorkspace(workspaceId: String): Flow<List<BusinessGoal>> =
        dao.observeAllByWorkspace(workspaceId).map { entities -> entities.map { it.toDomain() } }

    fun observe(id: String): Flow<BusinessGoal?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun save(goal: BusinessGoal) {
        dao.upsert(goal.toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    private fun BusinessGoalEntity.toDomain() = BusinessGoal(
        id = id,
        workspaceId = workspaceId,
        goalType = goalType,
        description = description,
        priority = priority,
        isActive = isActive
    )

    private fun BusinessGoal.toEntity() = BusinessGoalEntity(
        id = id,
        workspaceId = workspaceId,
        goalType = goalType,
        description = description,
        priority = priority,
        isActive = isActive
    )
}
