package com.ai4biz.app.data.repository

import com.ai4biz.app.data.local.CustomerDao
import com.ai4biz.app.data.local.CustomerEntity
import com.ai4biz.app.model.Customer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomerRepository(private val dao: CustomerDao) {

    fun observeAllByWorkspace(workspaceId: String): Flow<List<Customer>> =
        dao.observeAllByWorkspace(workspaceId).map { entities -> entities.map { it.toDomain() } }

    fun observe(id: String): Flow<Customer?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun save(customer: Customer) {
        dao.upsert(customer.toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    private fun CustomerEntity.toDomain() = Customer(
        id = id,
        workspaceId = workspaceId,
        name = name,
        companyName = companyName,
        email = email,
        phone = phone,
        whatsapp = whatsapp,
        industry = industry,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Customer.toEntity() = CustomerEntity(
        id = id,
        workspaceId = workspaceId,
        name = name,
        companyName = companyName,
        email = email,
        phone = phone,
        whatsapp = whatsapp,
        industry = industry,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
