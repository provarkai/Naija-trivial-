package com.ai4biz.app.ui.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai4biz.app.ai.AiGeneratorService
import com.ai4biz.app.data.repository.DocumentRepository
import com.ai4biz.app.model.GeneratedDocument
import com.ai4biz.app.model.ToolType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface GeneratorUiState {
    data object Idle : GeneratorUiState
    data object Loading : GeneratorUiState
    data class Error(val message: String) : GeneratorUiState
    data class Success(val documentId: String) : GeneratorUiState
}

class GeneratorViewModel(
    private val toolType: ToolType,
    private val aiGeneratorService: AiGeneratorService,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GeneratorUiState>(GeneratorUiState.Idle)
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    fun generate(inputs: Map<String, String>) {
        _uiState.value = GeneratorUiState.Loading
        viewModelScope.launch {
            aiGeneratorService.generate(toolType, inputs)
                .onSuccess { content ->
                    val id = UUID.randomUUID().toString()
                    val titleSeed = inputs.values.firstOrNull { it.isNotBlank() } ?: toolType.title
                    val document = GeneratedDocument(
                        id = id,
                        toolId = toolType.id,
                        toolTitle = toolType.title,
                        title = titleSeed.take(60),
                        content = content,
                        createdAt = System.currentTimeMillis()
                    )
                    documentRepository.save(document)
                    _uiState.value = GeneratorUiState.Success(id)
                }
                .onFailure { error ->
                    _uiState.value = GeneratorUiState.Error(error.message ?: "Something went wrong. Please try again.")
                }
        }
    }
}
