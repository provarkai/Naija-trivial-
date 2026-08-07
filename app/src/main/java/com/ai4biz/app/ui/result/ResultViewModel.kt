package com.ai4biz.app.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai4biz.app.data.repository.DocumentRepository
import com.ai4biz.app.model.GeneratedDocument
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ResultViewModel(
    documentId: String,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    val document: StateFlow<GeneratedDocument?> = documentRepository.observeDocument(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateContent(document: GeneratedDocument, newContent: String) {
        viewModelScope.launch {
            documentRepository.save(document.copy(content = newContent))
        }
    }

    fun delete(document: GeneratedDocument, onDone: () -> Unit) {
        viewModelScope.launch {
            documentRepository.delete(document.id)
            onDone()
        }
    }
}
