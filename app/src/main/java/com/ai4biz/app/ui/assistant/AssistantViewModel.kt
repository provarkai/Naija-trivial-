package com.ai4biz.app.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai4biz.app.ai.AssistantRepository
import com.ai4biz.app.data.repository.WorkspaceRepository
import com.ai4biz.app.model.Conversation
import com.ai4biz.app.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssistantUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val error: String? = null
)

/**
 * The AI Assistant chat screen's ViewModel (Phase 2 Sprints 5-6, see
 * docs/PHASE2_ARCHITECTURE.md). Never touches `NavHostController` -- a
 * tool suggestion tap is handled entirely in [AssistantScreen] via a
 * plain callback, same convention every other screen in this app follows.
 */
class AssistantViewModel(
    private val workspaceRepository: WorkspaceRepository,
    private val assistantRepository: AssistantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var conversation: Conversation? = null

    init {
        viewModelScope.launch {
            val workspace = workspaceRepository.observeActiveWorkspace().first() ?: return@launch
            val conv = assistantRepository.getOrCreateConversation(workspace.id)
            conversation = conv
            assistantRepository.observeMessages(conv.id)
                .onEach { messages -> _uiState.update { it.copy(messages = messages) } }
                .launchIn(viewModelScope)
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val conv = conversation ?: return
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isSending) return

        _uiState.update { it.copy(inputText = "", isSending = true, error = null) }
        viewModelScope.launch {
            assistantRepository.sendMessage(conv, text)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "Something went wrong. Please try again.") }
                }
            _uiState.update { it.copy(isSending = false) }
        }
    }
}
