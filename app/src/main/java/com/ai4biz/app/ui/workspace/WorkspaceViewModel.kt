package com.ai4biz.app.ui.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai4biz.app.data.repository.BrandSettingsRepository
import com.ai4biz.app.data.repository.BusinessProfileRepository
import com.ai4biz.app.data.repository.CustomerRepository
import com.ai4biz.app.data.repository.DocumentRepository
import com.ai4biz.app.data.repository.ProductServiceRepository
import com.ai4biz.app.data.repository.WorkspaceRepository
import com.ai4biz.app.model.BrandSettings
import com.ai4biz.app.model.BusinessProfile
import com.ai4biz.app.model.Customer
import com.ai4biz.app.model.GeneratedDocument
import com.ai4biz.app.model.ProductService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the Workspace screen's 4 tabs (Phase 2 Sprint 3, see
 * docs/PHASE2_ARCHITECTURE.md). Reactive off the active workspace rather
 * than a one-shot load -- unlike [com.ai4biz.app.ui.businesssetup.BusinessSetupViewModel]'s
 * prefill, this screen should stay correct if the active workspace ever
 * changes (multi-workspace switching is future scope, but this shape
 * doesn't need to change to support it later).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModel(
    private val workspaceRepository: WorkspaceRepository,
    private val documentRepository: DocumentRepository,
    private val productServiceRepository: ProductServiceRepository,
    private val customerRepository: CustomerRepository,
    businessProfileRepository: BusinessProfileRepository,
    brandSettingsRepository: BrandSettingsRepository
) : ViewModel() {

    private val activeWorkspaceId: StateFlow<String?> =
        workspaceRepository.observeActiveWorkspace()
            .map { it?.id }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val searchQuery = MutableStateFlow("")

    val documents: StateFlow<List<GeneratedDocument>> = combine(
        activeWorkspaceId.filterNotNull().flatMapLatest { documentRepository.observeHistoryByWorkspace(it) },
        searchQuery
    ) { docs, query ->
        if (query.isBlank()) {
            docs
        } else {
            docs.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.toolTitle.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductService>> = activeWorkspaceId.filterNotNull()
        .flatMapLatest { productServiceRepository.observeAllByWorkspace(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = activeWorkspaceId.filterNotNull()
        .flatMapLatest { customerRepository.observeAllByWorkspace(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val businessProfile: StateFlow<BusinessProfile?> = activeWorkspaceId.filterNotNull()
        .flatMapLatest { businessProfileRepository.observeAllByWorkspace(it) }
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val brandSettings: StateFlow<BrandSettings?> = activeWorkspaceId.filterNotNull()
        .flatMapLatest { brandSettingsRepository.observeAllByWorkspace(it) }
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch { documentRepository.delete(id) }
    }

    fun saveProduct(product: ProductService) {
        viewModelScope.launch {
            val workspaceId = activeWorkspaceId.value ?: return@launch
            productServiceRepository.save(product.copy(workspaceId = workspaceId))
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch { productServiceRepository.delete(id) }
    }

    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            val workspaceId = activeWorkspaceId.value ?: return@launch
            customerRepository.save(customer.copy(workspaceId = workspaceId))
        }
    }

    fun deleteCustomer(id: String) {
        viewModelScope.launch { customerRepository.delete(id) }
    }
}
