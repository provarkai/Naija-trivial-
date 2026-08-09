package com.ai4biz.app.ui.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ai4biz.app.model.Message
import com.ai4biz.app.model.MessageRole
import com.ai4biz.app.model.ToolType
import com.ai4biz.app.navigation.Routes
import com.ai4biz.app.ui.LocalAppContainer
import com.ai4biz.app.ui.SimpleViewModelFactory

/**
 * The AI Assistant chat screen (Phase 2 Sprints 5-6, see
 * docs/PHASE2_ARCHITECTURE.md). Chats/advises and, when relevant,
 * suggests one of the 5 existing generator tools -- tapping a suggestion
 * hands off to that tool's normal form via a plain, fully reversible
 * `navigate()` (no `popUpTo`), so backing out of the Generator screen
 * returns straight to this conversation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(navController: NavHostController) {
    val container = LocalAppContainer.current
    val viewModel: AssistantViewModel = viewModel(
        factory = SimpleViewModelFactory {
            AssistantViewModel(container.workspaceRepository, container.assistantRepository)
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Assistant") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.inputText,
                        onValueChange = { viewModel.onInputChange(it) },
                        placeholder = { Text("Ask your business AI...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.sendMessage() },
                        enabled = uiState.inputText.isNotBlank() && !uiState.isSending
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.messages.isEmpty() && !uiState.isSending) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Ask me anything about your business -- I can advise, or point you to the right tool.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                reverseLayout = true,
                contentPadding = PaddingValues(16.dp)
            ) {
                if (uiState.isSending) {
                    item {
                        Row(modifier = Modifier.padding(bottom = 12.dp)) {
                            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                        }
                    }
                }
                items(uiState.messages.reversed(), key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        onToolSuggestionClick = { toolId -> navController.navigate(Routes.generator(toolId)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, onToolSuggestionClick: (String) -> Unit) {
    val isUser = message.role == MessageRole.USER
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
        if (message.suggestedToolIds.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                message.suggestedToolIds.forEach { toolId ->
                    val tool = ToolType.fromId(toolId) ?: return@forEach
                    FilterChip(
                        selected = false,
                        onClick = { onToolSuggestionClick(toolId) },
                        label = { Text(tool.title) }
                    )
                }
            }
        }
    }
}
