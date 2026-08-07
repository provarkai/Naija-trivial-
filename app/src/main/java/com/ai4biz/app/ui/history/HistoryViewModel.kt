package com.ai4biz.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai4biz.app.data.repository.DocumentRepository
import com.ai4biz.app.model.GeneratedDocument
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val documentRepository: DocumentRepository) : ViewModel() {

    val history: StateFlow<List<GeneratedDocument>> = documentRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch { documentRepository.delete(id) }
    }
}
